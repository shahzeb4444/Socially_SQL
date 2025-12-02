package com.teamsx.i230610_i230040.worker

import android.content.Context
import android.util.Log
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.teamsx.i230610_i230040.database.AppDatabase
import com.teamsx.i230610_i230040.network.*
import com.teamsx.i230610_i230040.utils.NetworkMonitor
import com.google.gson.Gson
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Sync Worker - Background worker that syncs pending actions to server
 * Runs when device is online
 * Processes sync_queue table
 */
class SyncWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    private val database = AppDatabase.getDatabase(context)
    private val syncQueueDao = database.syncQueueDao()
    private val messageDao = database.messageDao()
    private val apiService = RetrofitInstance.apiService
    private val gson = Gson()

    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        Log.d(TAG, "SyncWorker started")

        // Check if online
        if (!NetworkMonitor.isOnline(applicationContext)) {
            Log.d(TAG, "Device offline, skipping sync")
            return@withContext Result.retry()
        }

        try {
            // Get pending items
            val pendingItems = syncQueueDao.getPendingItems()
            Log.d(TAG, "Found ${pendingItems.size} pending items to sync")

            var successCount = 0
            var failCount = 0

            for (item in pendingItems) {
                try {
                    // Update status to processing
                    syncQueueDao.updateStatus(item.id, "processing")

                    val success = when (item.action) {
                        "send_message" -> syncSendMessage(item)
                        "edit_message" -> syncEditMessage(item)
                        "delete_message" -> syncDeleteMessage(item)
                        "create_post" -> syncCreatePost(item)
                        "create_story" -> syncCreateStory(item)
                        "toggle_like" -> syncToggleLike(item)
                        else -> {
                            Log.w(TAG, "Unknown action: ${item.action}")
                            false
                        }
                    }

                    if (success) {
                        // Delete from queue
                        syncQueueDao.deleteItem(item.id)
                        successCount++
                        Log.d(TAG, "Synced: ${item.action} - ${item.localReferenceId}")
                    } else {
                        // Increment retry count
                        syncQueueDao.incrementRetryCount(item.id, error = "Sync failed")
                        syncQueueDao.updateStatus(item.id, "failed")
                        failCount++
                        Log.w(TAG, "Failed: ${item.action} - ${item.localReferenceId}")
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Error syncing item ${item.id}", e)
                    syncQueueDao.incrementRetryCount(item.id, error = e.message ?: "Unknown error")
                    syncQueueDao.updateStatus(item.id, "failed")
                    failCount++
                }
            }

            Log.d(TAG, "Sync complete: $successCount succeeded, $failCount failed")

            // Return success if at least some items synced
            return@withContext if (failCount == 0 || successCount > 0) {
                Result.success()
            } else {
                Result.retry()
            }

        } catch (e: Exception) {
            Log.e(TAG, "SyncWorker error", e)
            return@withContext Result.retry()
        }
    }

    /**
     * Sync send message action
     */
    private suspend fun syncSendMessage(item: com.teamsx.i230610_i230040.database.entity.SyncQueueEntity): Boolean {
        return try {
            val request = gson.fromJson(item.jsonPayload, SendMessageRequest::class.java)
            Log.d(TAG, "Syncing message: chatId=${request.chatId}, localId=${item.localReferenceId}, text=${request.text.take(20)}")

            val response = apiService.sendMessage(request)

            if (response.isSuccessful) {
                val body = response.body()
                Log.d(TAG, "Send message response: success=${body?.success}, error=${body?.error}")

                if (body?.success == true) {
                    val serverMessageId = body.data?.message?.messageId
                    Log.d(TAG, "Message synced successfully. Server ID: $serverMessageId")

                    if (serverMessageId != null && serverMessageId != item.localReferenceId) {
                        // Update local message with server ID
                        messageDao.updateMessageId(item.localReferenceId, serverMessageId)
                        messageDao.updateSyncStatus(serverMessageId, true, "synced")
                    } else {
                        // Use local ID if server didn't provide new one
                        messageDao.updateSyncStatus(item.localReferenceId, true, "synced")
                    }
                    true
                } else {
                    Log.e(TAG, "Send message failed: ${body?.error}")
                    false
                }
            } else {
                Log.e(TAG, "Send message HTTP error: ${response.code()} - ${response.message()}")
                false
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error syncing send_message", e)
            false
        }
    }

    /**
     * Sync edit message action
     */
    private suspend fun syncEditMessage(item: com.teamsx.i230610_i230040.database.entity.SyncQueueEntity): Boolean {
        return try {
            val request = gson.fromJson(item.jsonPayload, EditMessageRequest::class.java)
            val response = apiService.editMessage(request)
            response.isSuccessful && response.body()?.success == true
        } catch (e: Exception) {
            Log.e(TAG, "Error syncing edit_message", e)
            false
        }
    }

    /**
     * Sync delete message action
     */
    private suspend fun syncDeleteMessage(item: com.teamsx.i230610_i230040.database.entity.SyncQueueEntity): Boolean {
        return try {
            val request = gson.fromJson(item.jsonPayload, DeleteMessageRequest::class.java)
            val response = apiService.deleteMessage(request)
            response.isSuccessful && response.body()?.success == true
        } catch (e: Exception) {
            Log.e(TAG, "Error syncing delete_message", e)
            false
        }
    }

    /**
     * Sync create post action
     */
    private suspend fun syncCreatePost(item: com.teamsx.i230610_i230040.database.entity.SyncQueueEntity): Boolean {
        return try {
            val request = gson.fromJson(item.jsonPayload, CreatePostRequest::class.java)
            val response = apiService.createPost(request)

            if (response.isSuccessful && response.body()?.success == true) {
                val serverPostId = response.body()?.data?.post?.postId
                if (serverPostId != null) {
                    database.postDao().updatePostId(item.localReferenceId, serverPostId)
                    database.postDao().updateSyncStatus(serverPostId, true, "synced")
                }
                true
            } else {
                false
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error syncing create_post", e)
            false
        }
    }

    /**
     * Sync create story action
     */
    private suspend fun syncCreateStory(item: com.teamsx.i230610_i230040.database.entity.SyncQueueEntity): Boolean {
        return try {
            val request = gson.fromJson(item.jsonPayload, CreateStoryRequest::class.java)
            val response = apiService.createStory(request)

            if (response.isSuccessful && response.body()?.success == true) {
                val serverStoryId = response.body()?.data?.story?.storyId
                if (serverStoryId != null) {
                    database.storyDao().updateStoryId(item.localReferenceId, serverStoryId)
                    database.storyDao().updateSyncStatus(serverStoryId, true, "synced")
                }
                true
            } else {
                false
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error syncing create_story", e)
            false
        }
    }

    /**
     * Sync toggle like action
     */
    private suspend fun syncToggleLike(item: com.teamsx.i230610_i230040.database.entity.SyncQueueEntity): Boolean {
        return try {
            val request = gson.fromJson(item.jsonPayload, ToggleLikeRequest::class.java)
            val response = apiService.toggleLike(request)
            response.isSuccessful && response.body()?.success == true
        } catch (e: Exception) {
            Log.e(TAG, "Error syncing toggle_like", e)
            false
        }
    }

    companion object {
        private const val TAG = "SyncWorker"
        const val WORK_NAME = "sync_worker"
    }
}

