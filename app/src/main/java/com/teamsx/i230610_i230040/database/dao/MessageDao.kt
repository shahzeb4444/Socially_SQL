package com.teamsx.i230610_i230040.database.dao

import androidx.lifecycle.LiveData
import androidx.room.*
import com.teamsx.i230610_i230040.database.entity.MessageEntity

/**
 * Data Access Object for Messages
 * Provides methods to interact with messages table
 */
@Dao
interface MessageDao {

    // Insert or replace message
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(message: MessageEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(messages: List<MessageEntity>)

    // Get all messages for a chat (LiveData for auto-updates)
    @Query("SELECT * FROM messages WHERE chatId = :chatId ORDER BY timestamp ASC")
    fun getMessagesForChat(chatId: String): LiveData<List<MessageEntity>>

    // Get messages for a chat (one-time)
    @Query("SELECT * FROM messages WHERE chatId = :chatId ORDER BY timestamp ASC")
    suspend fun getMessagesForChatOnce(chatId: String): List<MessageEntity>

    // Get unsynced messages
    @Query("SELECT * FROM messages WHERE isSynced = 0 ORDER BY localTimestamp ASC")
    suspend fun getUnsyncedMessages(): List<MessageEntity>

    // Update sync status
    @Query("UPDATE messages SET isSynced = :synced, syncStatus = :status WHERE messageId = :messageId")
    suspend fun updateSyncStatus(messageId: String, synced: Boolean, status: String)

    // Update message with server ID (after sync)
    @Query("UPDATE messages SET messageId = :newMessageId, isSynced = 1, syncStatus = 'synced' WHERE messageId = :oldMessageId")
    suspend fun updateMessageId(oldMessageId: String, newMessageId: String)

    // Delete message
    @Query("DELETE FROM messages WHERE messageId = :messageId")
    suspend fun deleteMessage(messageId: String)

    // Mark message as deleted (soft delete)
    @Query("UPDATE messages SET isDeleted = 1, deletedAt = :deletedAt WHERE messageId = :messageId")
    suspend fun markAsDeleted(messageId: String, deletedAt: Long)

    // Update message text (for edits)
    @Query("UPDATE messages SET text = :newText, isEdited = 1 WHERE messageId = :messageId")
    suspend fun updateMessageText(messageId: String, newText: String)

    // Get message by ID
    @Query("SELECT * FROM messages WHERE messageId = :messageId LIMIT 1")
    suspend fun getMessageById(messageId: String): MessageEntity?

    // Delete all messages for a chat
    @Query("DELETE FROM messages WHERE chatId = :chatId")
    suspend fun deleteAllMessagesForChat(chatId: String)

    // Get messages count for a chat
    @Query("SELECT COUNT(*) FROM messages WHERE chatId = :chatId")
    suspend fun getMessagesCount(chatId: String): Int
}

