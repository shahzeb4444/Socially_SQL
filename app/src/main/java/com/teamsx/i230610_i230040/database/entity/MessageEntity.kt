package com.teamsx.i230610_i230040.database.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Local SQLite entity for messages
 * Stores messages for offline viewing and queuing for sync
 */
@Entity(tableName = "messages")
data class MessageEntity(
    @PrimaryKey
    val messageId: String,
    val chatId: String,
    val senderId: String,
    val senderUsername: String,
    val text: String,
    val timestamp: Long,
    val isEdited: Boolean = false,
    val isDeleted: Boolean = false,
    val deletedAt: Long = 0,
    val mediaType: String = "",
    val mediaUrl: String = "",
    val mediaCaption: String = "",
    val isVanishMode: Boolean = false,
    val viewedBy: String = "",
    val vanishedFor: String = "",

    // Offline sync fields
    val isSynced: Boolean = false,          // Has been uploaded to server
    val syncStatus: String = "pending",     // pending, syncing, synced, failed
    val localTimestamp: Long = System.currentTimeMillis(), // When created locally
    val retryCount: Int = 0                 // Number of sync retry attempts
)

