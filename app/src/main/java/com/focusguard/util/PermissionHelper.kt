package com.focusguard.util

import android.Manifest
import android.app.AlarmManager
import android.app.NotificationManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.PowerManager
import android.provider.Settings
import androidx.core.content.ContextCompat

object PermissionHelper {

    data class PermissionStatus(
        val name: String,
        val description: String,
        val isGranted: Boolean,
        val settingsIntent: Intent?  // null = runtime permission; non-null = settings screen
    )

    fun getPermissionStatuses(context: Context): List<PermissionStatus> = listOf(
        PermissionStatus(
            name = "Read Contacts",
            description = "Match callers against your contact list and apply name/company rules",
            isGranted = hasPermission(context, Manifest.permission.READ_CONTACTS),
            settingsIntent = null
        ),
        PermissionStatus(
            name = "Notification Access",
            description = "Suppress SMS/notification banners from blocked contacts (brief flash may still appear)",
            isGranted = hasNotificationListenerAccess(context),
            settingsIntent = Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS)
        ),
        PermissionStatus(
            name = "Do Not Disturb Access",
            description = "Allows FocusGuard to modify Do Not Disturb settings if needed",
            isGranted = hasNotificationPolicyAccess(context),
            settingsIntent = Intent(Settings.ACTION_NOTIFICATION_POLICY_ACCESS_SETTINGS)
        ),
        PermissionStatus(
            name = "Battery Optimization",
            description = "Prevents Android from killing FocusGuard — critical on Samsung/Xiaomi",
            isGranted = isBatteryOptimizationIgnored(context),
            settingsIntent = Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS).apply {
                data = Uri.parse("package:${context.packageName}")
            }
        ),
        PermissionStatus(
            name = "Post Notifications",
            description = "Show the persistent status notification that keeps the service alive",
            isGranted = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU)
                hasPermission(context, Manifest.permission.POST_NOTIFICATIONS)
            else true,
            settingsIntent = null
        ),
        PermissionStatus(
            name = "Exact Alarms",
            description = "Trigger schedule rules at precise times (optional but improves accuracy)",
            isGranted = hasExactAlarmPermission(context),
            settingsIntent = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S)
                Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM)
            else null
        )
    )

    fun hasPermission(context: Context, permission: String): Boolean =
        ContextCompat.checkSelfPermission(context, permission) == PackageManager.PERMISSION_GRANTED

    fun hasNotificationListenerAccess(context: Context): Boolean {
        val cn = ComponentName(
            context,
            com.focusguard.service.FocusGuardNotificationListenerService::class.java
        )
        val flat = Settings.Secure.getString(
            context.contentResolver,
            "enabled_notification_listeners"
        )
        return flat?.contains(cn.flattenToString()) == true
    }

    fun hasNotificationPolicyAccess(context: Context): Boolean =
        context.getSystemService(NotificationManager::class.java)
            ?.isNotificationPolicyAccessGranted == true

    fun isBatteryOptimizationIgnored(context: Context): Boolean =
        context.getSystemService(PowerManager::class.java)
            ?.isIgnoringBatteryOptimizations(context.packageName) == true

    fun hasExactAlarmPermission(context: Context): Boolean {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) return true
        return context.getSystemService(AlarmManager::class.java)
            ?.canScheduleExactAlarms() == true
    }
}
