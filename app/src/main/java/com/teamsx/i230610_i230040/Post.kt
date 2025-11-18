package com.teamsx.i230610_i230040

data class Post(
    val postId: String = "",
    val userId: String = "",
    val username: String = "",
    val userPhotoBase64: String? = null,
    val location: String = "",
    val description: String = "",
    val images: List<String> = emptyList(), // Base64 encoded images (1-3)
    val timestamp: Long = 0L,
    val likes: Map<String, Boolean> = emptyMap(), // userId -> true
    val commentsCount: Int = 0
) {
    fun getLikesCount(): Int = likes.size

    fun isLikedBy(userId: String): Boolean = likes.containsKey(userId)

    fun getFirstImage(): String = images.firstOrNull() ?: ""
}

data class Comment(
    val commentId: String = "",
    val postId: String = "",
    val userId: String = "",
    val username: String = "",
    val userPhotoBase64: String? = null,
    val message: String = "",
    val timestamp: Long = 0L
)