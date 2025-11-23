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

// ============================================================
// FOLLOW SYSTEM REQUEST/RESPONSE MODELS
// ============================================================

data class SendFollowRequest(
    @SerializedName("from_uid") val fromUid: String,
    @SerializedName("to_uid") val toUid: String
)

data class FollowActionResponse(
    @SerializedName("success") val success: Boolean,
    @SerializedName("data") val data: FollowActionData? = null,
    @SerializedName("error") val error: String? = null
)

data class FollowActionData(
    @SerializedName("message") val message: String,
    @SerializedName("from_uid") val fromUid: String? = null,
    @SerializedName("to_uid") val toUid: String? = null
)

data class GetFollowersRequest(
    @SerializedName("uid") val uid: String
)

data class GetFollowersResponse(
    @SerializedName("success") val success: Boolean,
    @SerializedName("data") val data: FollowersDataWrapper? = null,
    @SerializedName("error") val error: String? = null
)

data class FollowersDataWrapper(
    @SerializedName("followers") val followers: List<ApiFollowUser>,
    @SerializedName("count") val count: Int
)

data class GetFollowingResponse(
    @SerializedName("success") val success: Boolean,
    @SerializedName("data") val data: FollowingDataWrapper? = null,
    @SerializedName("error") val error: String? = null
)

data class FollowingDataWrapper(
    @SerializedName("following") val following: List<ApiFollowUser>,
    @SerializedName("count") val count: Int
)

data class ApiFollowUser(
    @SerializedName("uid") val uid: String,
    @SerializedName("username") val username: String,
    @SerializedName("full_name") val fullName: String?,
    @SerializedName("profile_image_url") val profileImageUrl: String?
)

data class GetFollowRequestsRequest(
    @SerializedName("uid") val uid: String
)

data class GetFollowRequestsResponse(
    @SerializedName("success") val success: Boolean,
    @SerializedName("data") val data: FollowRequestsDataWrapper? = null,
    @SerializedName("error") val error: String? = null
)

data class FollowRequestsDataWrapper(
    @SerializedName("requests") val requests: List<ApiFollowRequest>,
    @SerializedName("count") val count: Int
)

data class ApiFollowRequest(
    @SerializedName("from_uid") val fromUid: String?,
    @SerializedName("username") val username: String?,
    @SerializedName("full_name") val fullName: String?,
    @SerializedName("profile_image_url") val profileImageUrl: String?,
    @SerializedName("created_at") val createdAt: String?
)

// ============================================================
// USER SEARCH/PROFILE REQUEST/RESPONSE MODELS
// ============================================================

data class SearchUsersRequest(
    @SerializedName("query") val query: String,
    @SerializedName("current_user_uid") val currentUserUid: String = ""
)

data class SearchUsersResponse(
    @SerializedName("success") val success: Boolean,
    @SerializedName("data") val data: SearchUsersDataWrapper? = null,
    @SerializedName("error") val error: String? = null
)

data class SearchUsersDataWrapper(
    @SerializedName("users") val users: List<ApiFollowUser>,
    @SerializedName("count") val count: Int
)

data class GetUserProfileRequest(
    @SerializedName("uid") val uid: String,
    @SerializedName("current_uid") val currentUid: String? = null
)

data class GetUserProfileResponse(
    @SerializedName("success") val success: Boolean,
    @SerializedName("data") val data: UserProfileDataWrapper? = null,
    @SerializedName("error") val error: String? = null
)

data class UserProfileDataWrapper(
    @SerializedName("user") val user: ApiUserProfile,
    @SerializedName("relationship") val relationship: String? = null
)

data class ApiUserProfile(
    @SerializedName("uid") val uid: String,
    @SerializedName("username") val username: String,
    @SerializedName("full_name") val fullName: String? = null,
    @SerializedName("bio") val bio: String? = null,
    @SerializedName("profile_image_url") val profileImageUrl: String? = null,
    @SerializedName("cover_image_url") val coverImageUrl: String? = null,
    @SerializedName("followers_count") val followersCount: Int = 0,
    @SerializedName("following_count") val followingCount: Int = 0,
    @SerializedName("posts_count") val postsCount: Int = 0,
    @SerializedName("created_at") val createdAt: String? = null
)

data class UpdateProfileRequest(
    @SerializedName("uid") val uid: String,
    @SerializedName("full_name") val fullName: String? = null,
    @SerializedName("bio") val bio: String? = null,
    @SerializedName("profile_image_url") val profileImageUrl: String? = null,
    @SerializedName("cover_image_url") val coverImageUrl: String? = null
)

data class UpdateProfileResponse(
    @SerializedName("success") val success: Boolean,
    @SerializedName("data") val data: UpdateProfileDataWrapper? = null,
    @SerializedName("error") val error: String? = null
)

data class UpdateProfileDataWrapper(
    @SerializedName("user") val user: ApiUserProfile,
    @SerializedName("message") val message: String
)

// ============================================================
// GET USER POSTS & SINGLE POST MODELS
// ============================================================

data class GetUserPostsRequest(
    @SerializedName("uid") val uid: String,
    @SerializedName("limit") val limit: Int = 50,
    @SerializedName("offset") val offset: Int = 0
)

data class GetUserPostsResponse(
    @SerializedName("success") val success: Boolean,
    @SerializedName("data") val data: GetUserPostsDataWrapper? = null,
    @SerializedName("error") val error: String? = null
)

data class GetUserPostsDataWrapper(
    @SerializedName("posts") val posts: List<ApiPost>,
    @SerializedName("count") val count: Int
)

data class GetPostRequest(
    @SerializedName("post_id") val postId: String,
    @SerializedName("current_uid") val currentUid: String? = null
)

data class GetPostResponse(
    @SerializedName("success") val success: Boolean,
    @SerializedName("data") val data: GetPostDataWrapper? = null,
    @SerializedName("error") val error: String? = null
)

data class GetPostDataWrapper(
    @SerializedName("post") val post: ApiPost
)

data class ApiPost(
    @SerializedName("post_id") val postId: String,
    @SerializedName("user_id") val userId: String,
    @SerializedName("username") val username: String,
    @SerializedName("user_photo_base64") val userPhotoBase64: String? = null,
    @SerializedName("location") val location: String? = null,
    @SerializedName("description") val description: String,
    @SerializedName("images") val images: List<String>,
    @SerializedName("timestamp") val timestamp: Long,
    @SerializedName("likes_count") val likesCount: Int = 0,
    @SerializedName("comments_count") val commentsCount: Int = 0,
    @SerializedName("is_liked") val isLiked: Boolean = false
)

// ============================================================
// FCM PUSH NOTIFICATION MODELS
// ============================================================

data class SaveFCMTokenRequest(
    @SerializedName("uid") val uid: String,
    @SerializedName("fcm_token") val fcmToken: String
)

data class SaveFCMTokenResponse(
    @SerializedName("success") val success: Boolean,
    @SerializedName("data") val data: FCMTokenDataWrapper? = null,
    @SerializedName("error") val error: String? = null
)

data class FCMTokenDataWrapper(
    @SerializedName("message") val message: String,
    @SerializedName("uid") val uid: String
)

// ============================================================
// COMMENTS MODELS
// ============================================================

data class GetCommentsRequest(
    @SerializedName("post_id") val postId: String
)

data class GetCommentsResponse(
    @SerializedName("success") val success: Boolean,
    @SerializedName("data") val data: CommentsDataWrapper? = null,
    @SerializedName("error") val error: String? = null
)

data class CommentsDataWrapper(
    @SerializedName("comments") val comments: List<ApiComment>
)

data class ApiComment(
    @SerializedName("comment_id") val commentId: String,
    @SerializedName("post_id") val postId: String,
    @SerializedName("user_id") val userId: String,
    @SerializedName("username") val username: String,
    @SerializedName("user_photo_base64") val userPhotoBase64: String?,
    @SerializedName("message") val message: String,
    @SerializedName("timestamp") val timestamp: Long
)

data class AddCommentRequest(
    @SerializedName("post_id") val postId: String,
    @SerializedName("user_id") val userId: String,
    @SerializedName("message") val message: String
)

data class AddCommentResponse(
    @SerializedName("success") val success: Boolean,
    @SerializedName("data") val data: AddCommentDataWrapper? = null,
    @SerializedName("error") val error: String? = null
)

data class AddCommentDataWrapper(
    @SerializedName("comment") val comment: ApiComment,
    @SerializedName("comments_count") val commentsCount: Int
)

// ============================================================
// MESSAGING MODELS
// ============================================================

// Get chat users
data class GetChatUsersRequest(
    @SerializedName("user_id") val userId: String
)

data class GetChatUsersResponse(
    @SerializedName("success") val success: Boolean,
    @SerializedName("data") val data: ChatUsersDataWrapper? = null,
    @SerializedName("error") val error: String? = null
)

data class ChatUsersDataWrapper(
    @SerializedName("users") val users: List<ApiChatUser>
)

data class ApiChatUser(
    @SerializedName("uid") val uid: String,
    @SerializedName("username") val username: String,
    @SerializedName("full_name") val fullName: String?,
    @SerializedName("profile_image_url") val profileImageUrl: String?,
    @SerializedName("is_online") val isOnline: Boolean,
    @SerializedName("last_seen") val lastSeen: Long
)

// Get messages
data class GetMessagesRequest(
    @SerializedName("chat_id") val chatId: String,
    @SerializedName("limit") val limit: Int = 50,
    @SerializedName("before_timestamp") val beforeTimestamp: Long = Long.MAX_VALUE
)

data class GetMessagesResponse(
    @SerializedName("success") val success: Boolean,
    @SerializedName("data") val data: MessagesDataWrapper? = null,
    @SerializedName("error") val error: String? = null
)

data class MessagesDataWrapper(
    @SerializedName("messages") val messages: List<ApiMessage>
)

data class ApiMessage(
    @SerializedName("message_id") val messageId: String,
    @SerializedName("chat_id") val chatId: String,
    @SerializedName("sender_id") val senderId: String,
    @SerializedName("sender_username") val senderUsername: String,
    @SerializedName("text") val text: String,
    @SerializedName("timestamp") val timestamp: Long,
    @SerializedName("is_edited") val isEdited: Boolean,
    @SerializedName("is_deleted") val isDeleted: Boolean,
    @SerializedName("deleted_at") val deletedAt: Long,
    @SerializedName("media_type") val mediaType: String,
    @SerializedName("media_url") val mediaUrl: String,
    @SerializedName("media_caption") val mediaCaption: String
)

// Send message
data class SendMessageRequest(
    @SerializedName("message_id") val messageId: String? = null,
    @SerializedName("chat_id") val chatId: String,
    @SerializedName("sender_id") val senderId: String,
    @SerializedName("sender_username") val senderUsername: String,
    @SerializedName("text") val text: String = "",
    @SerializedName("media_type") val mediaType: String = "",
    @SerializedName("media_url") val mediaUrl: String = "",
    @SerializedName("media_caption") val mediaCaption: String = ""
)

data class SendMessageResponse(
    @SerializedName("success") val success: Boolean,
    @SerializedName("data") val data: SendMessageDataWrapper? = null,
    @SerializedName("error") val error: String? = null
)

data class SendMessageDataWrapper(
    @SerializedName("message") val message: ApiMessage
)

// Edit message
data class EditMessageRequest(
    @SerializedName("message_id") val messageId: String,
    @SerializedName("text") val text: String
)

data class EditMessageResponse(
    @SerializedName("success") val success: Boolean,
    @SerializedName("data") val data: EditMessageDataWrapper? = null,
    @SerializedName("error") val error: String? = null
)

data class EditMessageDataWrapper(
    @SerializedName("message_id") val messageId: String,
    @SerializedName("text") val text: String,
    @SerializedName("is_edited") val isEdited: Boolean
)

// Delete message
data class DeleteMessageRequest(
    @SerializedName("message_id") val messageId: String
)

data class DeleteMessageResponse(
    @SerializedName("success") val success: Boolean,
    @SerializedName("data") val data: DeleteMessageDataWrapper? = null,
    @SerializedName("error") val error: String? = null
)

data class DeleteMessageDataWrapper(
    @SerializedName("message_id") val messageId: String,
    @SerializedName("is_deleted") val isDeleted: Boolean,
    @SerializedName("deleted_at") val deletedAt: Long
)

// Update status
data class UpdateStatusRequest(
    @SerializedName("user_id") val userId: String,
    @SerializedName("is_online") val isOnline: Boolean
)

data class UpdateStatusResponse(
    @SerializedName("success") val success: Boolean,
    @SerializedName("data") val data: UpdateStatusDataWrapper? = null,
    @SerializedName("error") val error: String? = null
)

data class UpdateStatusDataWrapper(
    @SerializedName("user_id") val userId: String,
    @SerializedName("is_online") val isOnline: Boolean,
    @SerializedName("last_seen") val lastSeen: Long
)

// Poll new messages
data class PollNewMessagesRequest(
    @SerializedName("chat_id") val chatId: String,
    @SerializedName("since_timestamp") val sinceTimestamp: Long
)

data class PollNewMessagesResponse(
    @SerializedName("success") val success: Boolean,
    @SerializedName("data") val data: PollMessagesDataWrapper? = null,
    @SerializedName("error") val error: String? = null
)

data class PollMessagesDataWrapper(
    @SerializedName("messages") val messages: List<ApiMessage>,
    @SerializedName("count") val count: Int
)

