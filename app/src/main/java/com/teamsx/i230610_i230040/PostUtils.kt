package com.teamsx.i230610_i230040

object PostUtils {
    fun generatePostId(): String {
        return System.currentTimeMillis().toString() + "_" + (0..9999).random()
    }

    fun generateCommentId(): String {
        return System.currentTimeMillis().toString() + "_" + (0..9999).random()
    }

    fun getTimeAgo(timestamp: Long): String {
        val diff = System.currentTimeMillis() - timestamp
        val seconds = diff / 1000
        val minutes = seconds / 60
        val hours = minutes / 60
        val days = hours / 24
        val weeks = days / 7

        return when {
            weeks > 0 -> "${weeks}w"
            days > 0 -> "${days}d"
            hours > 0 -> "${hours}h"
            minutes > 0 -> "${minutes}m"
            else -> "Just now"
        }
    }
}