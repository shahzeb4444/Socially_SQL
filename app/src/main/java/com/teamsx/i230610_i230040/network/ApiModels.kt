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
    @SerializedName("full_name") val fullName: String
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

