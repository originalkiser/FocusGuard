package com.focusguard.service

import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import android.util.Log
import com.focusguard.domain.model.FilterResult
import com.focusguard.domain.usecase.EvaluateNotificationUseCase
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.*
import javax.inject.Inject

/**
 * Intercepts all posted notifications system-wide and cancels those from blocked contacts.
 *
 * Limitation: there is no pre-screening API for notifications. We cancel the notification
 * after it has been posted, so users may see a brief flash (~100–500ms) before it disappears.
 * This is a platform constraint; avoiding the flash requires being the default SMS app.
 *
 * We only process packages from known phone/messaging apps to minimise overhead.
 */
@AndroidEntryPoint
class FocusGuardNotificationListenerService : NotificationListenerService() {

    @Inject lateinit var evaluateNotificationUseCase: EvaluateNotificationUseCase

    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    // We intercept from these packages. Users with less common apps can extend this via settings.
    private val targetPackages = setOf(
        "com.android.messaging",
        "com.google.android.apps.messaging",
        "com.samsung.android.messaging",
        "com.android.mms",
        "com.google.android.dialer",
        "com.android.phone",
        "com.samsung.android.incallui",
        "com.oneplus.deskclock"
    )

    // Regex for common phone number formats in notification text
    private val phoneRegex = Regex("""[\+]?[1]?[\s.-]?\(?[0-9]{3}\)?[\s.-]?[0-9]{3}[\s.-]?[0-9]{4}""")

    override fun onNotificationPosted(sbn: StatusBarNotification) {
        if (sbn.packageName !in targetPackages) return

        val extras = sbn.notification?.extras ?: return
        val title = extras.getCharSequence("android.title")?.toString() ?: ""
        val text  = extras.getCharSequence("android.text")?.toString() ?: ""

        // Try number first; messaging apps (Google/Samsung Messages) put the contact NAME
        // in the title instead of a number, so fall back to name-based matching.
        val phoneNumber = extractPhoneNumber(title) ?: extractPhoneNumber(text)
        val senderName  = if (phoneNumber == null) title.trim().takeIf { it.isNotBlank() } else null

        if (phoneNumber == null && senderName == null) return

        scope.launch {
            try {
                val result = evaluateNotificationUseCase.evaluate(phoneNumber, senderName, sbn.packageName)
                if (result is FilterResult.Block) {
                    cancelNotification(sbn.key)
                    Log.d(TAG, "Cancelled notification from ${phoneNumber ?: senderName} (rule: ${result.ruleName})")
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error evaluating notification", e)
            }
        }
    }

    private fun extractPhoneNumber(text: String): String? =
        phoneRegex.find(text)?.value?.filter { it.isDigit() || it == '+' }

    override fun onDestroy() {
        super.onDestroy()
        scope.cancel()
    }

    companion object { private const val TAG = "FocusGuard/NotifListener" }
}
