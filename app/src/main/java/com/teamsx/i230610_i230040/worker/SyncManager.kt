package com.teamsx.i230610_i230040.worker

import android.content.Context
import androidx.work.*
import java.util.concurrent.TimeUnit

/**
 * Sync Manager - Schedules and manages background sync
 */
class SyncManager(private val context: Context) {

    private val workManager = WorkManager.getInstance(context)

    /**
     * Schedule periodic sync (runs every 15 minutes when online)
     */
    fun schedulePeriodicSync() {
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()

        val periodicSyncRequest = PeriodicWorkRequestBuilder<SyncWorker>(
            15, TimeUnit.MINUTES
        )
            .setConstraints(constraints)
            .setBackoffCriteria(
                BackoffPolicy.EXPONENTIAL,
                WorkRequest.MIN_BACKOFF_MILLIS,
                TimeUnit.MILLISECONDS
            )
            .build()

        workManager.enqueueUniquePeriodicWork(
            SyncWorker.WORK_NAME,
            ExistingPeriodicWorkPolicy.KEEP,
            periodicSyncRequest
        )
    }

    /**
     * Trigger immediate sync (one-time)
     */
    fun triggerImmediateSync() {
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()

        val immediateSyncRequest = OneTimeWorkRequestBuilder<SyncWorker>()
            .setConstraints(constraints)
            .build()

        workManager.enqueueUniqueWork(
            "immediate_sync",
            ExistingWorkPolicy.REPLACE,
            immediateSyncRequest
        )
    }

    /**
     * Cancel all sync work
     */
    fun cancelSync() {
        workManager.cancelUniqueWork(SyncWorker.WORK_NAME)
    }
}

