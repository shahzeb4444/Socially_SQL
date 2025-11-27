package com.teamsx.i230610_i230040.utils

import android.content.Context
import com.squareup.picasso.LruCache
import com.squareup.picasso.OkHttp3Downloader
import com.squareup.picasso.Picasso

/**
 * Picasso Image Loader Configuration
 * Configures Picasso with disk caching for offline image support
 */
object PicassoConfig {

    private var picassoInstance: Picasso? = null

    /**
     * Get configured Picasso instance with caching
     */
    fun getPicasso(context: Context): Picasso {
        if (picassoInstance == null) {
            synchronized(this) {
                if (picassoInstance == null) {
                    picassoInstance = Picasso.Builder(context.applicationContext)
                        .downloader(OkHttp3Downloader(context.applicationContext, 100L * 1024 * 1024)) // 100MB disk cache
                        .memoryCache(LruCache(context.applicationContext)) // Memory cache
                        .indicatorsEnabled(false) // Set true for debugging
                        .loggingEnabled(false) // Set true for debugging
                        .build()
                }
            }
        }
        return picassoInstance!!
    }

    /**
     * Set this as the singleton Picasso instance
     */
    fun setSingletonInstance(context: Context) {
        Picasso.setSingletonInstance(getPicasso(context))
    }
}

