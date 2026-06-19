package com.focusguard.domain.usecase

import com.focusguard.data.repository.RuleRepository
import com.focusguard.domain.model.*
import com.focusguard.util.ContactQueryHelper
import com.focusguard.util.ScheduleEvaluator
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class EvaluateNotificationUseCase @Inject constructor(
    private val ruleRepository: RuleRepository,
    private val contactQueryHelper: ContactQueryHelper,
    private val evaluateCallUseCase: EvaluateCallUseCase
) {
    // These packages send SMS/RCS messages — suppressSms should silence their notifications.
    private val messagingPackages = setOf(
        "com.android.messaging",
        "com.google.android.apps.messaging",
        "com.samsung.android.messaging",
        "com.android.mms"
    )

    /**
     * @param phoneNumber  Extracted from the notification text, or null if the title was a name.
     * @param senderName   The notification title used as a contact display-name fallback.
     * @param sourcePackage Package name of the app that posted the notification.
     */
    suspend fun evaluate(
        phoneNumber: String?,
        senderName: String?,
        sourcePackage: String
    ): FilterResult {
        val contact = when {
            phoneNumber != null -> contactQueryHelper.resolveNumber(phoneNumber)
            senderName  != null -> contactQueryHelper.resolveByName(senderName)
            else                -> null
        }

        val isMessaging = sourcePackage in messagingPackages
        val rules = ruleRepository.getActiveRulesList()

        for (rule in rules) {
            // For SMS packages, either suppress flag should silence the notification.
            val shouldCheck = if (isMessaging) rule.suppressSms || rule.suppressNotifications
                              else rule.suppressNotifications
            if (!shouldCheck) continue
            if (!ScheduleEvaluator.isActiveNow(rule.scheduleSlots)) continue

            val matches = if (phoneNumber != null) {
                evaluateCallUseCase.doesFilterMatch(rule.contactFilter, phoneNumber, contact)
            } else {
                // No phone number — only contact-key-aware filters can match.
                contact != null && doesFilterMatchByContact(rule.contactFilter, contact)
            }
            if (!matches) continue

            return when (rule.mode) {
                RuleMode.BLOCK -> FilterResult.Block(rule.id, rule.name)
                RuleMode.ALLOW -> FilterResult.Allow
            }
        }

        return FilterResult.PassThrough
    }

    // Area code and pattern filters need an actual number and cannot match by name alone.
    private fun doesFilterMatchByContact(filter: ContactFilter, contact: Contact): Boolean =
        when (filter) {
            is ContactFilter.Individual -> contact.lookupKey in filter.lookupKeys
            is ContactFilter.Company    -> contactQueryHelper.queryByCompany(filter.companyName).contains(contact.lookupKey)
            is ContactFilter.Group      -> contactQueryHelper.queryByGroup(filter.groupId).contains(contact.lookupKey)
            ContactFilter.All           -> true
            else                        -> false
        }
}
