package com.teamsx.i230610_i230040.database.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Sync Queue Entity
 * Stores pending actions that need to be synced to server when online
 */
@Entity(tableName = "sync_queue")
data class SyncQueueEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,

    val action: String,              // "send_message", "create_post", "edit_message", etc.
    val endpoint: String,            // API endpoint to call
    val jsonPayload: String,         // JSON data to send
    val localReferenceId: String,    // Reference to local entity (e.g., messageId)
    val timestamp: Long = System.currentTimeMillis(),
    val status: String = "pending",  // pending, processing, failed, completed
    val retryCount: Int = 0,
    val lastAttempt: Long = 0,
    val errorMessage: String = ""
)

