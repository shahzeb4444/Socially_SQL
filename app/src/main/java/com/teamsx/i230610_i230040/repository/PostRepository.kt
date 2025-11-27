package com.teamsx.i230610_i230040.repository

import android.content.Context
import androidx.lifecycle.LiveData
import com.teamsx.i230610_i230040.database.AppDatabase
import com.teamsx.i230610_i230040.database.entity.PostEntity
import com.teamsx.i230610_i230040.database.entity.SyncQueueEntity
import com.teamsx.i230610_i230040.network.*
import com.teamsx.i230610_i230040.utils.NetworkMonitor
import com.google.gson.Gson
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Post Repository - Handles post operations with offline-first approach
 */
class PostRepository(private val context: Context) {

    private val database = AppDatabase.getDatabase(context)
    private val postDao = database.postDao()
    private val syncQueueDao = database.syncQueueDao()
    private val apiService = RetrofitInstance.apiService
    private val gson = Gson()

    /**
     * Get all posts (LiveData - auto-updates UI)
     */
    fun getAllPosts(): LiveData<List<PostEntity>> {
        return postDao.getAllPosts()
    }

    /**
     * Get posts by user (LiveData)
     */
    fun getPostsByUser(userId: String): LiveData<List<PostEntity>> {
        return postDao.getPostsByUser(userId)
    }

    /**
     * Create a post (offline-first)
     */
    suspend fun createPost(
        userId: String,
        username: String,
        description: String,
        location: String,
        images: List<String>
    ): PostEntity = withContext(Dispatchers.IO) {

        val localPostId = "local_post_${System.currentTimeMillis()}_${(0..9999).random()}"
        val timestamp = System.currentTimeMillis()

        val postEntity = PostEntity(
            postId = localPostId,
            userId = userId,
            username = username,
            userProfileImage = "", // Will be filled from user cache
            description = description,
            location = location,
            images = images.joinToString(","),
            timestamp = timestamp,
            likesCount = 0,
            likedBy = "",
            commentsCount = 0,
            isSynced = false,
            syncStatus = "pending"
        )

        postDao.insert(postEntity)

        // Queue for sync
        val request = CreatePostRequest(
            postId = localPostId,
            userId = userId,
            description = description,
            location = location,
            images = images,
            timestamp = timestamp
        )
        val syncQueueEntry = SyncQueueEntity(
            action = "create_post",
            endpoint = "posts/create.php",
            jsonPayload = gson.toJson(request),
            localReferenceId = localPostId,
            status = "pending"
        )
        syncQueueDao.insert(syncQueueEntry)

        // Try sync if online
        if (NetworkMonitor.isOnline(context)) {
            trySyncPost(localPostId)
        }

        postEntity
    }

    /**
     * Fetch posts from server and cache locally
     */
    suspend fun fetchFeedFromServer(userId: String) = withContext(Dispatchers.IO) {
        try {
            if (!NetworkMonitor.isOnline(context)) return@withContext

            val request = GetFeedRequest(userId)
            val response = apiService.getFeedPosts(request)

            if (response.isSuccessful && response.body()?.success == true) {
                val serverPosts = response.body()?.data?.posts ?: emptyList()

                val entities = serverPosts.map { post ->
                    PostEntity(
                        postId = post.postId,
                        userId = post.userId,
                        username = post.username,
                        userProfileImage = post.userPhotoBase64 ?: "",
                        description = post.description,
                        location = post.location ?: "",
                        images = post.images.joinToString(","),
                        timestamp = post.timestamp,
                        likesCount = post.likesCount,
                        likedBy = "", // Will be updated when user likes
                        commentsCount = post.commentsCount,
                        isSynced = true,
                        syncStatus = "synced"
                    )
                }

                postDao.insertAll(entities)
            }
        } catch (e: Exception) {
            // Silently fail - offline mode
        }
    }

    /**
     * Toggle like on post
     */
    suspend fun toggleLike(postId: String, userId: String) = withContext(Dispatchers.IO) {
        val post = postDao.getPostById(postId)
        if (post != null) {
            val likedByList = post.likedBy.split(",").filter { it.isNotEmpty() }.toMutableList()
            val isLiked = likedByList.contains(userId)

            if (isLiked) {
                likedByList.remove(userId)
            } else {
                likedByList.add(userId)
            }

            val newLikedBy = likedByList.joinToString(",")
            val newCount = likedByList.size

            postDao.updateLikes(postId, newCount, newLikedBy)

            // Queue for sync
            val request = ToggleLikeRequest(postId, userId)
            val syncQueueEntry = SyncQueueEntity(
                action = "toggle_like",
                endpoint = "posts/toggle_like.php",
                jsonPayload = gson.toJson(request),
                localReferenceId = postId,
                status = "pending"
            )
            syncQueueDao.insert(syncQueueEntry)

            // Try sync if online
            if (NetworkMonitor.isOnline(context)) {
                trySyncLike(postId, userId)
            }
        }
    }

    private suspend fun trySyncPost(localPostId: String) = withContext(Dispatchers.IO) {
        try {
            val queueItem = syncQueueDao.getItemByReferenceId(localPostId) ?: return@withContext
            val request = gson.fromJson(queueItem.jsonPayload, CreatePostRequest::class.java)

            val response = apiService.createPost(request)

            if (response.isSuccessful && response.body()?.success == true) {
                val serverPostId = response.body()?.data?.post?.postId ?: localPostId
                postDao.updatePostId(localPostId, serverPostId)
                postDao.updateSyncStatus(serverPostId, true, "synced")
                syncQueueDao.deleteItem(queueItem.id)
            }
        } catch (e: Exception) {
            // Will retry later
        }
    }

    private suspend fun trySyncLike(postId: String, userId: String) = withContext(Dispatchers.IO) {
        try {
            val request = ToggleLikeRequest(postId, userId)
            apiService.toggleLike(request)

            val queueItem = syncQueueDao.getItemByReferenceId(postId)
            queueItem?.let { syncQueueDao.deleteItem(it.id) }
        } catch (e: Exception) {
            // Will retry later
        }
    }
}

