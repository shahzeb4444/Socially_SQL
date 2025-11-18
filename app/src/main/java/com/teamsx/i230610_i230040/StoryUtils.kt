package com.teamsx.i230610_i230040

object StoryUtils {
    // Calculate expiry time (24 hours from now)
    fun getExpiryTime(): Long {
        return System.currentTimeMillis() + (24 * 60 * 60 * 1000) // 24 hours
    }

    // Check if timestamp is within 24 hours
    fun isWithin24Hours(timestamp: Long): Boolean {
        val currentTime = System.currentTimeMillis()
        val twentyFourHoursInMillis = 24 * 60 * 60 * 1000
        return (currentTime - timestamp) < twentyFourHoursInMillis
    }

    // Generate story ID
    fun generateStoryId(): String {
        return System.currentTimeMillis().toString() + "_" + (0..9999).random()
    }
}