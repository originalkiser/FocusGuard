package com.focusguard

import android.app.Application
import android.util.Log
import androidx.hilt.work.HiltWorkerFactory
import androidx.work.Configuration
import androidx.work.WorkManager
import com.focusguard.service.ScheduleCheckWorker
import dagger.hilt.android.HiltAndroidApp
import javax.inject.Inject

@HiltAndroidApp
class FocusGuardApp : Application(), Configuration.Provider {

    @Inject lateinit var workerFactory: HiltWorkerFactory

    /**
     * WorkManager calls this getter lazily (only when WorkManager.getInstance() is first called).
     * With the auto-initializer disabled in the manifest, that happens in our own onCreate(),
     * by which point Hilt has already injected workerFactory. The isInitialized guard is a
     * safety net for edge cases like process death + restore on older OS versions.
     */
    override val workManagerConfiguration: Configuration
        get() = if (::workerFactory.isInitialized) {
            Configuration.Builder()
                .setWorkerFactory(workerFactory)
                .build()
        } else {
            Log.w("FocusGuard", "workManagerConfiguration called before Hilt injection — using default factory")
            Configuration.Builder().build()
        }

    override fun onCreate() {
        super.onCreate()  // Hilt injects workerFactory before this returns
        try {
            ScheduleCheckWorker.enqueue(WorkManager.getInstance(this))
        } catch (e: Exception) {
            Log.e("FocusGuard", "WorkManager enqueue failed", e)
        }
    }
}
