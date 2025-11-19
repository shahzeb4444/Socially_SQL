package com.teamsx.i230610_i230040.network

import com.teamsx.i230610_i230040.Story
import com.teamsx.i230610_i230040.StoryGroup
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class StoryRepository(private val apiService: ApiService) {

    suspend fun createStory(
        storyId: String,
        userId: String,
        imageBase64: String,
        timestamp: Long,
        expiresAt: Long,
        isCloseFriendsOnly: Boolean
    ): Resource<Story> = withContext(Dispatchers.IO) {
        try {
            val request = CreateStoryRequest(
                storyId = storyId,
                userId = userId,
                imageBase64 = imageBase64,
                timestamp = timestamp,
                expiresAt = expiresAt,
                isCloseFriendsOnly = isCloseFriendsOnly
            )

            val response = apiService.createStory(request)

            if (response.isSuccessful) {
                val body = response.body()
                if (body != null && body.success && body.data != null) {
                    val apiStory = body.data.story
                    val story = Story(
                        storyId = apiStory.storyId,
                        userId = apiStory.userId,
                        username = apiStory.username,
                        userPhotoBase64 = apiStory.userPhotoBase64,
                        imageBase64 = apiStory.imageBase64,
                        timestamp = apiStory.timestamp,
                        expiresAt = apiStory.expiresAt,
                        viewedBy = apiStory.viewedBy,
                        isCloseFreindsOnly = apiStory.isCloseFriendsOnly
                    )
                    Resource.Success(story)
                } else {
                    Resource.Error(body?.error ?: "Failed to create story")
                }
            } else {
                Resource.Error("Server error: ${response.code()}")
            }
        } catch (e: Exception) {
            Resource.Error(e.message ?: "Network error")
        }
    }

    suspend fun getFeedStories(userId: String): Resource<List<StoryGroup>> = withContext(Dispatchers.IO) {
        try {
            val request = GetFeedRequest(userId = userId)
            val response = apiService.getFeedStories(request)

            if (response.isSuccessful) {
                val body = response.body()
                if (body != null && body.success && body.data != null) {
                    val storyGroups = body.data.storyGroups.map { apiGroup ->
                        val stories = apiGroup.stories.map { apiStory ->
                            Story(
                                storyId = apiStory.storyId,
                                userId = apiStory.userId,
                                username = apiStory.username,
                                userPhotoBase64 = apiStory.userPhotoBase64,
                                imageBase64 = apiStory.imageBase64,
                                timestamp = apiStory.timestamp,
                                expiresAt = apiStory.expiresAt,
                                viewedBy = apiStory.viewedBy,
                                isCloseFreindsOnly = apiStory.isCloseFriendsOnly
                            )
                        }

                        // Calculate hasUnviewedStories
                        val hasUnviewed = stories.any { !it.viewedBy.containsKey(userId) }

                        StoryGroup(
                            userId = apiGroup.userId,
                            username = apiGroup.username,
                            userPhotoBase64 = apiGroup.userPhotoBase64,
                            stories = stories,
                            hasUnviewedStories = hasUnviewed
                        )
                    }
                    Resource.Success(storyGroups)
                } else {
                    Resource.Error(body?.error ?: "Failed to fetch stories")
                }
            } else {
                Resource.Error("Server error: ${response.code()}")
            }
        } catch (e: Exception) {
            Resource.Error(e.message ?: "Network error")
        }
    }

    suspend fun markStoryViewed(storyId: String, viewerId: String): Resource<Boolean> = withContext(Dispatchers.IO) {
        try {
            val request = MarkStoryViewedRequest(
                storyId = storyId,
                viewerId = viewerId
            )

            val response = apiService.markStoryViewed(request)

            if (response.isSuccessful) {
                val body = response.body()
                if (body != null && body.success) {
                    Resource.Success(true)
                } else {
                    Resource.Error(body?.error ?: "Failed to mark as viewed")
                }
            } else {
                Resource.Error("Server error: ${response.code()}")
            }
        } catch (e: Exception) {
            Resource.Error(e.message ?: "Network error")
        }
    }
}

