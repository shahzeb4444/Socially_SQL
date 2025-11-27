package com.teamsx.i230610_i230040.repository

import android.content.Context
import androidx.lifecycle.LiveData
import com.teamsx.i230610_i230040.database.AppDatabase
import com.teamsx.i230610_i230040.database.entity.StoryEntity
import com.teamsx.i230610_i230040.database.entity.SyncQueueEntity
import com.teamsx.i230610_i230040.network.*
import com.teamsx.i230610_i230040.utils.NetworkMonitor
import com.google.gson.Gson
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Story Repository - Handles story operations with offline-first approach
 */
class StoryRepository(private val context: Context) {

    private val database = AppDatabase.getDatabase(context)
    private val storyDao = database.storyDao()
    private val syncQueueDao = database.syncQueueDao()
    private val apiService = RetrofitInstance.apiService
    private val gson = Gson()

    /**
     * Get active stories (not expired) - LiveData
     */
    fun getActiveStories(): LiveData<List<StoryEntity>> {
        return storyDao.getActiveStories()
    }

    /**
     * Get stories by user
     */
    fun getStoriesByUser(userId: String): LiveData<List<StoryEntity>> {
        return storyDao.getStoriesByUser(userId)
    }

    /**
     * Create a story (offline-first)
     */
    suspend fun createStory(
        userId: String,
        username: String,
        userPhotoBase64: String,
        imageBase64: String,
        isCloseFriendsOnly: Boolean = false
    ): StoryEntity = withContext(Dispatchers.IO) {

        val localStoryId = "local_story_${System.currentTimeMillis()}_${(0..9999).random()}"
        val timestamp = System.currentTimeMillis()
        val expiresAt = timestamp + (24 * 60 * 60 * 1000) // 24 hours

        val storyEntity = StoryEntity(
            storyId = localStoryId,
            userId = userId,
            username = username,
            userPhotoBase64 = userPhotoBase64,
            imageBase64 = imageBase64,
            timestamp = timestamp,
            expiresAt = expiresAt,
            isCloseFriendsOnly = isCloseFriendsOnly,
            viewedBy = "",
            isSynced = false,
            syncStatus = "pending"
        )

        storyDao.insert(storyEntity)

        // Queue for sync
        val request = CreateStoryRequest(
            storyId = localStoryId,
            userId = userId,
            imageBase64 = imageBase64,
            timestamp = timestamp,
            expiresAt = expiresAt,
            isCloseFriendsOnly = isCloseFriendsOnly
        )
        val syncQueueEntry = SyncQueueEntity(
            action = "create_story",
            endpoint = "stories/create.php",
            jsonPayload = gson.toJson(request),
            localReferenceId = localStoryId,
            status = "pending"
        )
        syncQueueDao.insert(syncQueueEntry)

        // Try sync if online
        if (NetworkMonitor.isOnline(context)) {
            trySyncStory(localStoryId)
        }

        storyEntity
    }

    /**
     * Fetch stories from server and cache locally
     */
    suspend fun fetchStoriesFromServer(userId: String) = withContext(Dispatchers.IO) {
        try {
            if (!NetworkMonitor.isOnline(context)) return@withContext

            val request = GetFeedRequest(userId)
            val response = apiService.getFeedStories(request)

            if (response.isSuccessful && response.body()?.success == true) {
                val storyGroups = response.body()?.data?.storyGroups ?: emptyList()

                val allStories = mutableListOf<StoryEntity>()

                storyGroups.forEach { group ->
                    group.stories.forEach { story ->
                        val viewedByString = story.viewedBy.keys.joinToString(",")

                        allStories.add(StoryEntity(
                            storyId = story.storyId,
                            userId = story.userId,
                            username = story.username,
                            userPhotoBase64 = story.userPhotoBase64 ?: "",
                            imageBase64 = story.imageBase64,
                            timestamp = story.timestamp,
                            expiresAt = story.expiresAt,
                            isCloseFriendsOnly = story.isCloseFriendsOnly,
                            viewedBy = viewedByString,
                            isSynced = true,
                            syncStatus = "synced"
                        ))
                    }
                }

                storyDao.insertAll(allStories)

                // Delete expired stories
                storyDao.deleteExpiredStories()
            }
        } catch (e: Exception) {
            // Silently fail - offline mode
        }
    }

    /**
     * Mark story as viewed
     */
    suspend fun markStoryViewed(storyId: String, userId: String) = withContext(Dispatchers.IO) {
        val story = storyDao.getStoryById(storyId)
        if (story != null) {
            val viewedByList = story.viewedBy.split(",").filter { it.isNotEmpty() }.toMutableList()
            if (!viewedByList.contains(userId)) {
                viewedByList.add(userId)
                val newViewedBy = viewedByList.joinToString(",")
                storyDao.updateViewedBy(storyId, newViewedBy)

                // Try sync if online
                if (NetworkMonitor.isOnline(context)) {
                    try {
                        val request = MarkStoryViewedRequest(storyId, userId)
                        apiService.markStoryViewed(request)
                    } catch (e: Exception) {
                        // Silently fail
                    }
                }
            }
        }
    }

    private suspend fun trySyncStory(localStoryId: String) = withContext(Dispatchers.IO) {
        try {
            val queueItem = syncQueueDao.getItemByReferenceId(localStoryId) ?: return@withContext
            val request = gson.fromJson(queueItem.jsonPayload, CreateStoryRequest::class.java)

            val response = apiService.createStory(request)

            if (response.isSuccessful && response.body()?.success == true) {
                val serverStoryId = response.body()?.data?.story?.storyId ?: localStoryId
                storyDao.updateStoryId(localStoryId, serverStoryId)
                storyDao.updateSyncStatus(serverStoryId, true, "synced")
                syncQueueDao.deleteItem(queueItem.id)
            }
        } catch (e: Exception) {
            // Will retry later
        }
    }
}

