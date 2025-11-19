package com.teamsx.i230610_i230040.network

import com.google.gson.annotations.SerializedName
import com.teamsx.i230610_i230040.models.User

// ============================================================
// REQUEST MODELS
// ============================================================

data class RegisterRequest(
    @SerializedName("email") val email: String,
    @SerializedName("password") val password: String,
    @SerializedName("username") val username: String,
    @SerializedName("full_name") val fullName: String,
    @SerializedName("profile_image_url") val profileImageUrl: String? = null
)

data class LoginRequest(
    @SerializedName("email") val email: String,
    @SerializedName("password") val password: String
)

// ============================================================
// RESPONSE MODELS
// ============================================================

data class RegisterResponse(
    @SerializedName("success") val success: Boolean,
    @SerializedName("data") val data: UserDataWrapper? = null,
    @SerializedName("error") val error: String? = null
)

data class LoginResponse(
    @SerializedName("success") val success: Boolean,
    @SerializedName("data") val data: UserDataWrapper? = null,
    @SerializedName("error") val error: String? = null
)

data class UserDataWrapper(
    @SerializedName("user") val user: ApiUser
)

// API User model matching PHP response
data class ApiUser(
    @SerializedName("id") val id: String,
    @SerializedName("uid") val uid: String,
    @SerializedName("email") val email: String,
    @SerializedName("username") val username: String,
    @SerializedName("full_name") val fullName: String?,
    @SerializedName("bio") val bio: String?,
    @SerializedName("profile_image_url") val profileImageUrl: String?,
    @SerializedName("cover_image_url") val coverImageUrl: String?,
    @SerializedName("created_at") val createdAt: String
)

// Extension to convert ApiUser to User
fun ApiUser.toUser(): User {
    return User(
        uid = this.uid,
        username = this.username,
        email = this.email,
        fullName = this.fullName ?: "",
        bio = this.bio,
        profileImageUrl = this.profileImageUrl,
        coverImageUrl = this.coverImageUrl,
        createdAt = this.createdAt
    )
}

// ============================================================
// POSTS REQUEST/RESPONSE MODELS
// ============================================================

data class CreatePostRequest(
    @SerializedName("post_id") val postId: String,
    @SerializedName("user_id") val userId: String,
    @SerializedName("description") val description: String,
    @SerializedName("location") val location: String = "",
    @SerializedName("images") val images: List<String>,
    @SerializedName("timestamp") val timestamp: Long
)

data class CreatePostResponse(
    @SerializedName("success") val success: Boolean,
    @SerializedName("data") val data: PostDataWrapper? = null,
    @SerializedName("error") val error: String? = null
)

data class PostDataWrapper(
    @SerializedName("post") val post: ApiPost
)

data class ApiPost(
    @SerializedName("post_id") val postId: String,
    @SerializedName("user_id") val userId: String,
    @SerializedName("username") val username: String,
    @SerializedName("user_photo_base64") val userPhotoBase64: String?,
    @SerializedName("location") val location: String,
    @SerializedName("description") val description: String,
    @SerializedName("images") val images: List<String>,
    @SerializedName("timestamp") val timestamp: Long,
    @SerializedName("likes") val likes: Map<String, Boolean>,
    @SerializedName("likes_count") val likesCount: Int,
    @SerializedName("comments_count") val commentsCount: Int
)

data class GetFeedRequest(
    @SerializedName("user_id") val userId: String
)

data class GetFeedResponse(
    @SerializedName("success") val success: Boolean,
    @SerializedName("data") val data: FeedDataWrapper? = null,
    @SerializedName("error") val error: String? = null
)

data class FeedDataWrapper(
    @SerializedName("posts") val posts: List<ApiPost>
)

data class ToggleLikeRequest(
    @SerializedName("post_id") val postId: String,
    @SerializedName("user_id") val userId: String
)

data class ToggleLikeResponse(
    @SerializedName("success") val success: Boolean,
    @SerializedName("data") val data: ToggleLikeDataWrapper? = null,
    @SerializedName("error") val error: String? = null
)

data class ToggleLikeDataWrapper(
    @SerializedName("post") val post: ApiPost,
    @SerializedName("action") val action: String
)

// ============================================================
// STORIES REQUEST/RESPONSE MODELS
// ============================================================

data class CreateStoryRequest(
    @SerializedName("story_id") val storyId: String,
    @SerializedName("user_id") val userId: String,
    @SerializedName("image_base64") val imageBase64: String,
    @SerializedName("timestamp") val timestamp: Long,
    @SerializedName("expires_at") val expiresAt: Long,
    @SerializedName("is_close_friends_only") val isCloseFriendsOnly: Boolean
)

data class CreateStoryResponse(
    @SerializedName("success") val success: Boolean,
    @SerializedName("data") val data: StoryDataWrapper? = null,
    @SerializedName("error") val error: String? = null
)

data class StoryDataWrapper(
    @SerializedName("story") val story: ApiStory
)

data class ApiStory(
    @SerializedName("story_id") val storyId: String,
    @SerializedName("user_id") val userId: String,
    @SerializedName("username") val username: String,
    @SerializedName("user_photo_base64") val userPhotoBase64: String?,
    @SerializedName("image_base64") val imageBase64: String,
    @SerializedName("timestamp") val timestamp: Long,
    @SerializedName("expires_at") val expiresAt: Long,
    @SerializedName("viewedBy") val viewedBy: Map<String, Boolean>,
    @SerializedName("is_close_friends_only") val isCloseFriendsOnly: Boolean
)

data class GetFeedStoriesResponse(
    @SerializedName("success") val success: Boolean,
    @SerializedName("data") val data: StoriesFeedDataWrapper? = null,
    @SerializedName("error") val error: String? = null
)

data class StoriesFeedDataWrapper(
    @SerializedName("story_groups") val storyGroups: List<ApiStoryGroup>
)

data class ApiStoryGroup(
    @SerializedName("userId") val userId: String,
    @SerializedName("username") val username: String,
    @SerializedName("userPhotoBase64") val userPhotoBase64: String?,
    @SerializedName("stories") val stories: List<ApiStory>
)

data class MarkStoryViewedRequest(
    @SerializedName("story_id") val storyId: String,
    @SerializedName("viewer_id") val viewerId: String
)

data class BaseResponse(
    @SerializedName("success") val success: Boolean,
    @SerializedName("data") val data: Map<String, Any>? = null,
    @SerializedName("error") val error: String? = null
)

