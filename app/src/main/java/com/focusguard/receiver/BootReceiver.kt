package com.focusguard.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.focusguard.service.FocusGuardForegroundService

/** Restarts the foreground service after a device reboot so rules stay active. */
class BootReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action ?: return
        if (action == Intent.ACTION_BOOT_COMPLETED ||
            action == "android.intent.action.QUICKBOOT_POWERON"
        ) {
            context.startForegroundService(
                Intent(context, FocusGuardForegroundService::class.java).apply {
                    this.action = FocusGuardForegroundService.ACTION_START
                }
            )
        }
    }
}
