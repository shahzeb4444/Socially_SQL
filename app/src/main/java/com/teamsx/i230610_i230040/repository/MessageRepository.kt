package com.teamsx.i230610_i230040.repository

import android.content.Context
import androidx.lifecycle.LiveData
import com.teamsx.i230610_i230040.database.AppDatabase
import com.teamsx.i230610_i230040.database.entity.MessageEntity
import com.teamsx.i230610_i230040.database.entity.SyncQueueEntity
import com.teamsx.i230610_i230040.network.*
import com.teamsx.i230610_i230040.utils.NetworkMonitor
import com.google.gson.Gson
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Message Repository
 * Handles message operations with offline-first approach
 * - Saves to local database immediately
 * - Queues for sync if offline
 * - Syncs to server when online
 */
class MessageRepository(private val context: Context) {

    private val database = AppDatabase.getDatabase(context)
    private val messageDao = database.messageDao()
    private val syncQueueDao = database.syncQueueDao()
    private val apiService = RetrofitInstance.apiService
    private val gson = Gson()

    /**
     * Get messages for a chat (LiveData - auto-updates UI)
     */
    fun getMessagesForChat(chatId: String): LiveData<List<MessageEntity>> {
        return messageDao.getMessagesForChat(chatId)
    }

    /**
     * Send a message (offline-first)
     * 1. Save to local database immediately (instant UI update)
     * 2. Add to sync queue
     * 3. If online, sync immediately
     */
    suspend fun sendMessage(
        chatId: String,
        senderId: String,
        senderUsername: String,
        text: String,
        mediaType: String = "",
        mediaUrl: String = "",
        mediaCaption: String = "",
        isVanishMode: Boolean = false
    ): MessageEntity = withContext(Dispatchers.IO) {

        // Generate temporary local message ID
        val localMessageId = "local_${System.currentTimeMillis()}_${(0..9999).random()}"
        val timestamp = System.currentTimeMillis()

        // Create local message entity
        val messageEntity = MessageEntity(
            messageId = localMessageId,
            chatId = chatId,
            senderId = senderId,
            senderUsername = senderUsername,
            text = text,
            timestamp = timestamp,
            mediaType = mediaType,
            mediaUrl = mediaUrl,
            mediaCaption = mediaCaption,
            isVanishMode = isVanishMode,
            isSynced = false,
            syncStatus = "pending"
        )

        // Save to local database (instant UI update)
        messageDao.insert(messageEntity)

        // Create sync queue entry
        val request = SendMessageRequest(
            messageId = localMessageId,
            chatId = chatId,
            senderId = senderId,
            senderUsername = senderUsername,
            text = text,
            mediaType = mediaType,
            mediaUrl = mediaUrl,
            mediaCaption = mediaCaption,
            isVanishMode = isVanishMode
        )

        val syncQueueEntry = SyncQueueEntity(
            action = "send_message",
            endpoint = "messages/send_message.php",
            jsonPayload = gson.toJson(request),
            localReferenceId = localMessageId,
            status = "pending"
        )

        syncQueueDao.insert(syncQueueEntry)

        // If online, try to sync immediately
        if (NetworkMonitor.isOnline(context)) {
            trySyncMessage(localMessageId)
        }

        messageEntity
    }

    /**
     * Try to sync a message to server
     */
    private suspend fun trySyncMessage(localMessageId: String) = withContext(Dispatchers.IO) {
        try {
            val message = messageDao.getMessageById(localMessageId) ?: return@withContext

            // Update status to syncing
            messageDao.updateSyncStatus(localMessageId, false, "syncing")

            val request = SendMessageRequest(
                messageId = message.messageId,
                chatId = message.chatId,
                senderId = message.senderId,
                senderUsername = message.senderUsername,
                text = message.text,
                mediaType = message.mediaType,
                mediaUrl = message.mediaUrl,
                mediaCaption = message.mediaCaption,
                isVanishMode = message.isVanishMode
            )

            val response = apiService.sendMessage(request)

            if (response.isSuccessful && response.body()?.success == true) {
                val serverMessageId = response.body()?.data?.message?.messageId ?: localMessageId

                // Update with server message ID
                messageDao.updateMessageId(localMessageId, serverMessageId)
                messageDao.updateSyncStatus(serverMessageId, true, "synced")

                // Remove from sync queue
                val queueItem = syncQueueDao.getItemByReferenceId(localMessageId)
                queueItem?.let { syncQueueDao.deleteItem(it.id) }
            } else {
                // Mark as failed
                messageDao.updateSyncStatus(localMessageId, false, "failed")
            }
        } catch (e: Exception) {
            // Mark as failed
            messageDao.updateSyncStatus(localMessageId, false, "failed")
        }
    }

    /**
     * Load messages from server and update local database
     */
    suspend fun fetchMessagesFromServer(chatId: String, viewerId: String) = withContext(Dispatchers.IO) {
        try {
            if (!NetworkMonitor.isOnline(context)) return@withContext

            val request = GetMessagesRequest(chatId, viewerId = viewerId)
            val response = apiService.getMessages(request)

            if (response.isSuccessful && response.body()?.success == true) {
                val serverMessages = response.body()?.data?.messages ?: emptyList()

                // Convert to entities and save to database
                val entities = serverMessages.map { msg ->
                    MessageEntity(
                        messageId = msg.messageId,
                        chatId = msg.chatId,
                        senderId = msg.senderId,
                        senderUsername = msg.senderUsername,
                        text = msg.text,
                        timestamp = msg.timestamp,
                        isEdited = msg.isEdited,
                        isDeleted = msg.isDeleted,
                        deletedAt = msg.deletedAt,
                        mediaType = msg.mediaType,
                        mediaUrl = msg.mediaUrl,
                        mediaCaption = msg.mediaCaption,
                        isVanishMode = msg.isVanishMode,
                        viewedBy = msg.viewedBy,
                        vanishedFor = msg.vanishedFor,
                        isSynced = true,
                        syncStatus = "synced"
                    )
                }

                messageDao.insertAll(entities)
            }
        } catch (e: Exception) {
            // Silently fail - offline mode
        }
    }

    /**
     * Edit a message
     */
    suspend fun editMessage(messageId: String, newText: String) = withContext(Dispatchers.IO) {
        // Update local database
        messageDao.updateMessageText(messageId, newText)

        // Queue for sync
        val request = EditMessageRequest(messageId, newText)
        val syncQueueEntry = SyncQueueEntity(
            action = "edit_message",
            endpoint = "messages/edit_message.php",
            jsonPayload = gson.toJson(request),
            localReferenceId = messageId
        )
        syncQueueDao.insert(syncQueueEntry)

        // Sync if online
        if (NetworkMonitor.isOnline(context)) {
            trySyncEdit(messageId, newText)
        }
    }

    /**
     * Delete a message
     */
    suspend fun deleteMessage(messageId: String) = withContext(Dispatchers.IO) {
        val deletedAt = System.currentTimeMillis()

        // Update local database
        messageDao.markAsDeleted(messageId, deletedAt)

        // Queue for sync
        val request = DeleteMessageRequest(messageId)
        val syncQueueEntry = SyncQueueEntity(
            action = "delete_message",
            endpoint = "messages/delete_message.php",
            jsonPayload = gson.toJson(request),
            localReferenceId = messageId
        )
        syncQueueDao.insert(syncQueueEntry)

        // Sync if online
        if (NetworkMonitor.isOnline(context)) {
            trySyncDelete(messageId)
        }
    }

    /**
     * Sync edit to server
     */
    private suspend fun trySyncEdit(messageId: String, newText: String) = withContext(Dispatchers.IO) {
        try {
            val request = EditMessageRequest(messageId, newText)
            val response = apiService.editMessage(request)

            if (response.isSuccessful && response.body()?.success == true) {
                val queueItem = syncQueueDao.getItemByReferenceId(messageId)
                queueItem?.let { syncQueueDao.deleteItem(it.id) }
            }
        } catch (e: Exception) {
            // Will retry later
        }
    }

    /**
     * Sync delete to server
     */
    private suspend fun trySyncDelete(messageId: String) = withContext(Dispatchers.IO) {
        try {
            val request = DeleteMessageRequest(messageId)
            val response = apiService.deleteMessage(request)

            if (response.isSuccessful && response.body()?.success == true) {
                val queueItem = syncQueueDao.getItemByReferenceId(messageId)
                queueItem?.let { syncQueueDao.deleteItem(it.id) }
            }
        } catch (e: Exception) {
            // Will retry later
        }
    }

    /**
     * Get unsynced messages count
     */
    suspend fun getUnsyncedCount(): Int = withContext(Dispatchers.IO) {
        messageDao.getUnsyncedMessages().size
    }
}

