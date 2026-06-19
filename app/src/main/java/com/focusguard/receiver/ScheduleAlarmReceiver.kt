package com.focusguard.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.focusguard.service.FocusGuardForegroundService

/**
 * Receives AlarmManager intents when a schedule slot boundary is crossed.
 * Rule evaluation itself is real-time (ScheduleEvaluator.isActiveNow checks wall clock),
 * so this receiver's only job is to ensure the foreground service is alive.
 */
class ScheduleAlarmReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        Log.d("FocusGuard/Alarm", "Schedule alarm fired: ${intent.action}")
        context.startForegroundService(
            Intent(context, FocusGuardForegroundService::class.java)
        )
    }
}
