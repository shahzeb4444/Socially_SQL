package com.teamsx.i230610_i230040

data class FollowRequest(
    val fromUid: String = "",
    val fromUsername: String = "",
    val fromPhotoBase64: String? = null,
    val timestamp: Long = 0L
)
