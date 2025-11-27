package com.teamsx.i230610_i230040

import android.app.Application
import com.teamsx.i230610_i230040.utils.PicassoConfig
import com.teamsx.i230610_i230040.worker.SyncManager

/**
 * Application class - initializes offline support components
 */
class SociallyApplication : Application() {

    override fun onCreate() {
        super.onCreate()

        // Initialize Picasso with caching
        PicassoConfig.setSingletonInstance(this)

        // Schedule periodic background sync
        val syncManager = SyncManager(this)
        syncManager.schedulePeriodicSync()
    }
}

