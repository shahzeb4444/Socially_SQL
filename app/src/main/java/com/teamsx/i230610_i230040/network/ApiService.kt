package com.teamsx.i230610_i230040.network

import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.POST

interface ApiService {

    @POST("auth/register.php")
    suspend fun register(@Body request: RegisterRequest): Response<RegisterResponse>

    @POST("auth/login.php")
    suspend fun login(@Body request: LoginRequest): Response<LoginResponse>

    // Posts
    @POST("posts/create.php")
    suspend fun createPost(@Body request: CreatePostRequest): Response<CreatePostResponse>

    @POST("posts/get_feed.php")
    suspend fun getFeedPosts(@Body request: GetFeedRequest): Response<GetFeedResponse>

    @POST("posts/toggle_like.php")
    suspend fun toggleLike(@Body request: ToggleLikeRequest): Response<ToggleLikeResponse>

    @POST("posts/get_user_posts.php")
    suspend fun getUserPosts(@Body request: GetUserPostsRequest): Response<GetUserPostsResponse>

    @POST("posts/get_post.php")
    suspend fun getPost(@Body request: GetPostRequest): Response<GetPostResponse>

    // Stories
    @POST("stories/create.php")
    suspend fun createStory(@Body request: CreateStoryRequest): Response<CreateStoryResponse>

    @POST("stories/get_feed.php")
    suspend fun getFeedStories(@Body request: GetFeedRequest): Response<GetFeedStoriesResponse>

    @POST("stories/get_user_stories.php")
    suspend fun getUserStories(@Body request: GetUserStoriesRequest): Response<GetUserStoriesResponse>

    @POST("stories/mark_viewed.php")
    suspend fun markStoryViewed(@Body request: MarkStoryViewedRequest): Response<BaseResponse>

    @POST("stories/delete.php")
    suspend fun deleteStory(@Body request: DeleteStoryRequest): Response<BaseResponse>

    @POST("stories/cleanup_expired.php")
    suspend fun cleanupExpiredStories(): Response<BaseResponse>

    // Follow System
    @POST("follow/send_request.php")
    suspend fun sendFollowRequest(@Body request: SendFollowRequest): Response<FollowActionResponse>

    @POST("follow/cancel_request.php")
    suspend fun cancelFollowRequest(@Body request: SendFollowRequest): Response<FollowActionResponse>

    @POST("follow/accept_request.php")
    suspend fun acceptFollowRequest(@Body request: SendFollowRequest): Response<FollowActionResponse>

    @POST("follow/reject_request.php")
    suspend fun rejectFollowRequest(@Body request: SendFollowRequest): Response<FollowActionResponse>

    @POST("follow/unfollow.php")
    suspend fun unfollowUser(@Body request: SendFollowRequest): Response<FollowActionResponse>

    @POST("follow/get_followers.php")
    suspend fun getFollowers(@Body request: GetFollowersRequest): Response<GetFollowersResponse>

    @POST("follow/get_following.php")
    suspend fun getFollowing(@Body request: GetFollowersRequest): Response<GetFollowingResponse>

    @POST("follow/get_requests.php")
    suspend fun getFollowRequests(@Body request: GetFollowRequestsRequest): Response<GetFollowRequestsResponse>

    // User Profile & Search
    @POST("users/search.php")
    suspend fun searchUsers(@Body request: SearchUsersRequest): Response<SearchUsersResponse>

    @POST("user/get_profile.php")
    suspend fun getUserProfile(@Body request: GetUserProfileRequest): Response<GetUserProfileResponse>

    @POST("user/update_profile.php")
    suspend fun updateUserProfile(@Body request: UpdateProfileRequest): Response<UpdateProfileResponse>

    // Push Notifications
    @POST("notifications/save_fcm_token.php")
    suspend fun saveFCMToken(@Body request: SaveFCMTokenRequest): Response<SaveFCMTokenResponse>

    // Comments
    @POST("comments/get_comments.php")
    suspend fun getComments(@Body request: GetCommentsRequest): Response<GetCommentsResponse>

    @POST("comments/add_comment.php")
    suspend fun addComment(@Body request: AddCommentRequest): Response<AddCommentResponse>

    // Messaging
    @POST("messages/get_chat_users.php")
    suspend fun getChatUsers(@Body request: GetChatUsersRequest): Response<GetChatUsersResponse>

    @POST("messages/get_messages.php")
    suspend fun getMessages(@Body request: GetMessagesRequest): Response<GetMessagesResponse>

    @POST("messages/send_message.php")
    suspend fun sendMessage(@Body request: SendMessageRequest): Response<SendMessageResponse>

    @POST("messages/edit_message.php")
    suspend fun editMessage(@Body request: EditMessageRequest): Response<EditMessageResponse>

    @POST("messages/delete_message.php")
    suspend fun deleteMessage(@Body request: DeleteMessageRequest): Response<DeleteMessageResponse>

    @POST("messages/update_status.php")
    suspend fun updateStatus(@Body request: UpdateStatusRequest): Response<UpdateStatusResponse>

    @POST("messages/poll_new_messages.php")
    suspend fun pollNewMessages(@Body request: PollNewMessagesRequest): Response<PollNewMessagesResponse>
}
