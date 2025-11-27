package com.teamsx.i230610_i230040.database.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Local SQLite entity for stories
 * Stores stories for offline viewing
 */
@Entity(tableName = "stories")
data class StoryEntity(
    @PrimaryKey
    val storyId: String,
    val userId: String,
    val username: String,
    val userPhotoBase64: String = "",
    val imageBase64: String = "",
    val timestamp: Long,
    val expiresAt: Long,
    val isCloseFriendsOnly: Boolean = false,
    val viewedBy: String = "", // Comma-separated user IDs

    // Offline sync fields
    val isSynced: Boolean = false,
    val syncStatus: String = "pending",
    val localTimestamp: Long = System.currentTimeMillis(),
    val retryCount: Int = 0
)

