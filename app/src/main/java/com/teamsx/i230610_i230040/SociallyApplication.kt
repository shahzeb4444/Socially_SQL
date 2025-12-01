package com.teamsx.i230610_i230040

import android.app.Application
import android.util.Log
import com.teamsx.i230610_i230040.utils.PicassoConfig
import com.teamsx.i230610_i230040.utils.NetworkMonitor
import com.teamsx.i230610_i230040.worker.SyncManager

/**
 * Application class - initializes offline support components
 */
class SociallyApplication : Application() {

    private lateinit var syncManager: SyncManager
    private lateinit var networkMonitor: NetworkMonitor

    override fun onCreate() {
        super.onCreate()

        // Initialize Picasso with caching
        PicassoConfig.setSingletonInstance(this)

        // Initialize sync manager
        syncManager = SyncManager(this)

        // Schedule periodic background sync
        syncManager.schedulePeriodicSync()

        // Setup global network monitoring for immediate sync on connectivity restore
        setupNetworkMonitoring()
    }

    /**
     * Setup global network monitoring to trigger sync when device comes back online
     */
    private fun setupNetworkMonitoring() {
        networkMonitor = NetworkMonitor(this)
        networkMonitor.observeForever { isOnline ->
            Log.d("SociallyApplication", "Network state changed: ${if (isOnline) "ONLINE" else "OFFLINE"}")
            if (isOnline) {
                // Trigger immediate sync of all pending actions when network is restored
                Log.d("SociallyApplication", "Triggering immediate sync for pending offline data")
                syncManager.triggerImmediateSync()
            }
        }
    }
}

