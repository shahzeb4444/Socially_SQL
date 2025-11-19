package com.teamsx.i230610_i230040.utils

import android.content.Context
import android.content.SharedPreferences
import com.teamsx.i230610_i230040.models.User

class UserPreferences(context: Context) {

    private val prefs: SharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    companion object {
        private const val PREFS_NAME = "socially_user_prefs"
        private const val KEY_UID = "uid"
        private const val KEY_EMAIL = "email"
        private const val KEY_USERNAME = "username"
        private const val KEY_FULL_NAME = "full_name"
        private const val KEY_BIO = "bio"
        private const val KEY_PROFILE_IMAGE = "profile_image_url"
        private const val KEY_COVER_IMAGE = "cover_image_url"
        private const val KEY_CREATED_AT = "created_at"
        private const val KEY_IS_LOGGED_IN = "is_logged_in"
        private const val KEY_FCM_TOKEN = "fcm_token"
    }

    fun saveUser(user: User) {
        prefs.edit().apply {
            putString(KEY_UID, user.uid)
            putString(KEY_EMAIL, user.email)
            putString(KEY_USERNAME, user.username)
            putString(KEY_FULL_NAME, user.fullName)
            putString(KEY_BIO, user.bio)
            putString(KEY_PROFILE_IMAGE, user.profileImageUrl)
            putString(KEY_COVER_IMAGE, user.coverImageUrl)
            putString(KEY_CREATED_AT, user.createdAt)
            putBoolean(KEY_IS_LOGGED_IN, true)
            apply()
        }
    }

    fun getUser(): User? {
        if (!isLoggedIn()) return null

        val uid = prefs.getString(KEY_UID, null) ?: return null
        val email = prefs.getString(KEY_EMAIL, null) ?: return null
        val username = prefs.getString(KEY_USERNAME, null) ?: return null
        val fullName = prefs.getString(KEY_FULL_NAME, "") ?: ""
        val bio = prefs.getString(KEY_BIO, null)
        val profileImage = prefs.getString(KEY_PROFILE_IMAGE, null)
        val coverImage = prefs.getString(KEY_COVER_IMAGE, null)
        val createdAt = prefs.getString(KEY_CREATED_AT, "") ?: ""

        return User(
            uid = uid,
            email = email,
            username = username,
            fullName = fullName,
            bio = bio,
            profileImageUrl = profileImage,
            coverImageUrl = coverImage,
            createdAt = createdAt
        )
    }

    fun isLoggedIn(): Boolean {
        return prefs.getBoolean(KEY_IS_LOGGED_IN, false)
    }

    fun clearUser() {
        prefs.edit().clear().apply()
    }

    fun getUid(): String? {
        return prefs.getString(KEY_UID, null)
    }

    fun getUsername(): String? {
        return prefs.getString(KEY_USERNAME, null)
    }

    fun getEmail(): String? {
        return prefs.getString(KEY_EMAIL, null)
    }

    fun saveFCMToken(token: String) {
        prefs.edit().putString(KEY_FCM_TOKEN, token).apply()
    }

    fun getFCMToken(): String? {
        return prefs.getString(KEY_FCM_TOKEN, null)
    }
}

