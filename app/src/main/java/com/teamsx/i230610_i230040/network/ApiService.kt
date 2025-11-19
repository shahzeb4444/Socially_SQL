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

    // Stories
    @POST("stories/create.php")
    suspend fun createStory(@Body request: CreateStoryRequest): Response<CreateStoryResponse>

    @POST("stories/get_feed.php")
    suspend fun getFeedStories(@Body request: GetFeedRequest): Response<GetFeedStoriesResponse>

    @POST("stories/mark_viewed.php")
    suspend fun markStoryViewed(@Body request: MarkStoryViewedRequest): Response<BaseResponse>
}

