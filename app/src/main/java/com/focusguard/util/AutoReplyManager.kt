package com.focusguard.util

import android.content.Context
import android.telephony.SmsManager
import android.util.Log
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AutoReplyManager @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val tag = "FocusGuard/AutoReply"

    fun sendSms(to: String, message: String) {
        try {
            val smsManager = context.getSystemService(SmsManager::class.java)
            val parts = smsManager.divideMessage(message)
            if (parts.size == 1) {
                smsManager.sendTextMessage(to, null, message, null, null)
            } else {
                smsManager.sendMultipartTextMessage(to, null, parts, null, null)
            }
            Log.d(tag, "Auto-reply sent to $to")
        } catch (e: Exception) {
            Log.e(tag, "Failed to send auto-reply to $to", e)
        }
    }
}
