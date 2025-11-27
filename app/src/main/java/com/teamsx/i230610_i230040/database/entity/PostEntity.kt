package com.teamsx.i230610_i230040.database.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Local SQLite entity for posts
 * Stores posts for offline viewing
 */
@Entity(tableName = "posts")
data class PostEntity(
    @PrimaryKey
    val postId: String,
    val userId: String,
    val username: String,
    val userProfileImage: String = "",
    val description: String,
    val location: String = "",
    val images: String, // JSON array of image URLs
    val timestamp: Long,
    val likesCount: Int = 0,
    val likedBy: String = "", // Comma-separated user IDs
    val commentsCount: Int = 0,

    // Offline sync fields
    val isSynced: Boolean = false,
    val syncStatus: String = "pending",
    val localTimestamp: Long = System.currentTimeMillis(),
    val retryCount: Int = 0
)

