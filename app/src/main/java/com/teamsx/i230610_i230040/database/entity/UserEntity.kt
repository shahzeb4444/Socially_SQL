package com.teamsx.i230610_i230040.database.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Local SQLite entity for user data
 * Caches user information for offline access
 */
@Entity(tableName = "users")
data class UserEntity(
    @PrimaryKey
    val uid: String,
    val username: String,
    val email: String = "",
    val fullName: String = "",
    val bio: String = "",
    val profileImageUrl: String = "",
    val coverImageUrl: String = "",
    val isOnline: Boolean = false,
    val lastSeen: Long = 0,
    val fcmToken: String = "",
    val createdAt: String = "",

    // Cache metadata
    val lastUpdated: Long = System.currentTimeMillis()
)

