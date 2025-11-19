package com.teamsx.i230610_i230040.network

import com.teamsx.i230610_i230040.Post
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class PostRepository(private val apiService: ApiService) {

    suspend fun createPost(
        postId: String,
        userId: String,
        description: String,
        location: String,
        images: List<String>,
        timestamp: Long
    ): Resource<Post> = withContext(Dispatchers.IO) {
        try {
            val request = CreatePostRequest(
                postId = postId,
                userId = userId,
                description = description,
                location = location,
                images = images,
                timestamp = timestamp
            )

            val response = apiService.createPost(request)

            if (response.isSuccessful) {
                val body = response.body()
                if (body != null && body.success && body.data != null) {
                    val apiPost = body.data.post
                    val post = Post(
                        postId = apiPost.postId,
                        userId = apiPost.userId,
                        username = apiPost.username,
                        userPhotoBase64 = apiPost.userPhotoBase64,
                        location = apiPost.location,
                        description = apiPost.description,
                        images = apiPost.images,
                        timestamp = apiPost.timestamp,
                        likes = apiPost.likes,
                        commentsCount = apiPost.commentsCount
                    )
                    Resource.Success(post)
                } else {
                    Resource.Error(body?.error ?: "Failed to create post")
                }
            } else {
                Resource.Error("Server error: ${response.code()}")
            }
        } catch (e: Exception) {
            Resource.Error(e.message ?: "Network error")
        }
    }

    suspend fun getFeedPosts(userId: String): Resource<List<Post>> = withContext(Dispatchers.IO) {
        try {
            val request = GetFeedRequest(userId = userId)
            val response = apiService.getFeedPosts(request)

            if (response.isSuccessful) {
                val body = response.body()
                if (body != null && body.success && body.data != null) {
                    val posts = body.data.posts.map { apiPost ->
                        Post(
                            postId = apiPost.postId,
                            userId = apiPost.userId,
                            username = apiPost.username,
                            userPhotoBase64 = apiPost.userPhotoBase64,
                            location = apiPost.location,
                            description = apiPost.description,
                            images = apiPost.images,
                            timestamp = apiPost.timestamp,
                            likes = apiPost.likes,
                            commentsCount = apiPost.commentsCount
                        )
                    }
                    Resource.Success(posts)
                } else {
                    Resource.Error(body?.error ?: "Failed to fetch feed")
                }
            } else {
                Resource.Error("Server error: ${response.code()}")
            }
        } catch (e: Exception) {
            Resource.Error(e.message ?: "Network error")
        }
    }

    suspend fun toggleLike(postId: String, userId: String): Resource<Post> = withContext(Dispatchers.IO) {
        try {
            val request = ToggleLikeRequest(
                postId = postId,
                userId = userId
            )

            val response = apiService.toggleLike(request)

            if (response.isSuccessful) {
                val body = response.body()
                if (body != null && body.success && body.data != null) {
                    val apiPost = body.data.post
                    val post = Post(
                        postId = apiPost.postId,
                        userId = apiPost.userId,
                        username = apiPost.username,
                        userPhotoBase64 = apiPost.userPhotoBase64,
                        location = apiPost.location,
                        description = apiPost.description,
                        images = apiPost.images,
                        timestamp = apiPost.timestamp,
                        likes = apiPost.likes,
                        commentsCount = apiPost.commentsCount
                    )
                    Resource.Success(post)
                } else {
                    Resource.Error(body?.error ?: "Failed to toggle like")
                }
            } else {
                Resource.Error("Server error: ${response.code()}")
            }
        } catch (e: Exception) {
            Resource.Error(e.message ?: "Network error")
        }
    }
}

