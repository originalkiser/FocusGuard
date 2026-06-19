package com.focusguard.domain.usecase

import com.focusguard.data.db.dao.EmergencyBypassDao
import com.focusguard.data.db.entity.EmergencyBypassEvent
import com.focusguard.data.repository.RuleRepository
import com.focusguard.domain.model.*
import com.focusguard.util.ContactQueryHelper
import com.focusguard.util.PhoneNumberMatcher
import com.focusguard.util.ScheduleEvaluator
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Core rule-evaluation engine for incoming calls.
 * Called from FocusGuardCallScreeningService on the IO thread.
 */
@Singleton
class EvaluateCallUseCase @Inject constructor(
    private val ruleRepository: RuleRepository,
    private val contactQueryHelper: ContactQueryHelper,
    private val emergencyBypassDao: EmergencyBypassDao
) {
    suspend fun evaluate(callerNumber: String): FilterResult {
        val contact = contactQueryHelper.resolveNumber(callerNumber)

        val rules = ruleRepository.getActiveRulesList()

        // Starred bypass: if any rule opts in and contact is starred, allow immediately
        if (contact?.isStarred == true && rules.any { it.allowStarredBypass }) {
            return FilterResult.Allow
        }

        // Evaluate rules in priority order (highest priority first, as returned by the DAO)
        for (rule in rules) {
            if (!ScheduleEvaluator.isActiveNow(rule.scheduleSlots)) continue
            if (!doesRuleMatchCall(rule, callerNumber, contact)) continue

            return when (rule.mode) {
                RuleMode.BLOCK -> {
                    // Check emergency bypass before deciding to block
                    if (rule.emergencyBypass != null && isEmergencyBypassTriggered(rule, callerNumber)) {
                        FilterResult.Allow
                    } else {
                        // Record this blocked call attempt for future bypass counting
                        emergencyBypassDao.recordCall(
                            EmergencyBypassEvent(ruleId = rule.id, callerNumber = callerNumber)
                        )
                        FilterResult.Block(
                            ruleId = rule.id,
                            ruleName = rule.name,
                            autoReply = if (rule.autoReply != null && rule.suppressSms)
                                rule.autoReply.message
                            else null
                        )
                    }
                }
                RuleMode.ALLOW -> FilterResult.Allow
            }
        }

        return FilterResult.PassThrough
    }

    private suspend fun isEmergencyBypassTriggered(rule: Rule, callerNumber: String): Boolean {
        val cfg = rule.emergencyBypass ?: return false
        val since = System.currentTimeMillis() - cfg.withinMinutes * 60_000L
        // Count previous blocked attempts; current call not yet recorded → compare with count-1
        val previousAttempts = emergencyBypassDao.getCallCount(rule.id, callerNumber, since)
        return previousAttempts >= cfg.callCount - 1
    }

    private fun doesRuleMatchCall(rule: Rule, number: String, contact: Contact?): Boolean {
        if (!rule.suppressCalls) return false
        return doesFilterMatch(rule.contactFilter, number, contact)
    }

    internal fun doesFilterMatch(filter: ContactFilter, number: String, contact: Contact?): Boolean =
        when (filter) {
            is ContactFilter.Individual  -> contact != null && contact.lookupKey in filter.lookupKeys
            // PhoneLookup doesn't expose company; query the Data table directly
            is ContactFilter.Company     -> contact != null &&
                contactQueryHelper.queryByCompany(filter.companyName).contains(contact.lookupKey)
            is ContactFilter.AreaCode    -> PhoneNumberMatcher.matchesAreaCode(number, filter.areaCode)
            is ContactFilter.PhonePattern -> PhoneNumberMatcher.matchesPattern(number, filter.pattern)
            is ContactFilter.Group       -> contact != null &&
                contactQueryHelper.queryByGroup(filter.groupId).contains(contact.lookupKey)
            is ContactFilter.Tag         -> false  // tag lookup requires DB; handled via pre-resolved keys
            ContactFilter.All            -> true
        }
}
