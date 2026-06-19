package com.focusguard.service

import android.app.*
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import android.util.Log
import androidx.core.app.NotificationCompat
import com.focusguard.R
import com.focusguard.ui.MainActivity

/**
 * Persistent foreground service that keeps the process alive on OEMs with aggressive
 * battery management. No Hilt injection needed here — rule evaluation happens on-demand
 * in CallScreeningService and NotificationListenerService.
 */
class FocusGuardForegroundService : Service() {

    companion object {
        const val CHANNEL_ID      = "focusguard_service"
        const val NOTIFICATION_ID = 1001
        const val ACTION_START    = "com.focusguard.START"
        const val ACTION_STOP     = "com.focusguard.STOP"
        private const val TAG     = "FocusGuard/FGService"
    }

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == ACTION_STOP) {
            stopForeground(STOP_FOREGROUND_REMOVE)
            stopSelf()
            return START_NOT_STICKY
        }

        try {
            val notification = buildNotification()
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                startForeground(
                    NOTIFICATION_ID,
                    notification,
                    ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC
                )
            } else {
                startForeground(NOTIFICATION_ID, notification)
            }
        } catch (e: Exception) {
            Log.e(TAG, "startForeground failed — service will not persist in background", e)
        }

        return START_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun buildNotification(): Notification {
        val tapIntent = PendingIntent.getActivity(
            this, 0,
            Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE
        )
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("FocusGuard is active")
            .setContentText("Filtering calls and notifications")
            .setSmallIcon(R.drawable.ic_shield)
            .setContentIntent(tapIntent)
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()
    }

    private fun createNotificationChannel() {
        val channel = NotificationChannel(
            CHANNEL_ID,
            "FocusGuard Service",
            NotificationManager.IMPORTANCE_LOW
        ).apply {
            description = "Keeps FocusGuard active to filter calls and notifications"
            setShowBadge(false)
        }
        getSystemService(NotificationManager::class.java)?.createNotificationChannel(channel)
    }
}
