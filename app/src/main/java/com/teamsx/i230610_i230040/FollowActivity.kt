package com.teamsx.i230610_i230040

data class FollowActivity(
    val fromUid: String = "",
    val fromUsername: String = "",
    val fromPhotoBase64: String? = null,
    val type: String = "followed_you",   // future-proof for other types
    val timestamp: Long = 0L
)
