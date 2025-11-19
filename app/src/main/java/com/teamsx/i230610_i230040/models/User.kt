package com.teamsx.i230610_i230040.models

data class User(
    val uid: String,
    val username: String,
    val email: String,
    val fullName: String,
    val bio: String?,
    val profileImageUrl: String?,
    val coverImageUrl: String?,
    val createdAt: String
)
