package com.focusguard.service

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.*
import com.focusguard.data.db.dao.EmergencyBypassDao
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import java.util.concurrent.TimeUnit

/**
 * Periodic WorkManager job that prunes stale emergency-bypass records.
 * Runs every 15 minutes (minimum WorkManager period) and is durable across reboots.
 */
@HiltWorker
class ScheduleCheckWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted params: WorkerParameters,
    private val emergencyBypassDao: EmergencyBypassDao
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        // Prune events older than 1 hour — they're irrelevant for bypass counting
        emergencyBypassDao.pruneOldEvents(System.currentTimeMillis() - 3_600_000L)
        return Result.success()
    }

    companion object {
        private const val WORK_NAME = "schedule_check"

        fun enqueue(workManager: WorkManager) {
            val request = PeriodicWorkRequestBuilder<ScheduleCheckWorker>(
                15, TimeUnit.MINUTES
            ).build()
            workManager.enqueueUniquePeriodicWork(
                WORK_NAME,
                ExistingPeriodicWorkPolicy.KEEP,
                request
            )
        }
    }
}
