package com.teamsx.i230610_i230040

/**
 * User profile shape stored under /users/{uid} in Realtime Database.
 * Defaults are required so Firebase can deserialize it.
 */
data class UserProfile(
    val username: String = "",          // store LOWERCASE for uniqueness
    val firstName: String = "",
    val lastName: String = "",
    val dob: String = "",               // e.g., "2003-05-12"
    val email: String = "",
    val photoBase64: String? = null,    // optional; null if no photo picked
    val bio: String = "",               // NEW: user bio
    val postsCount: Int = 0,            // NEW: number of posts
    val followersCount: Int = 0,        // NEW: number of followers
    val followingCount: Int = 0,        // NEW: number of following
    val isProfileComplete: Boolean = true
) {
    /** Use for updateChildren() fan-out writes. */
    fun toMap(): Map<String, Any?> = mapOf(
        "username" to username,
        "firstName" to firstName,
        "lastName" to lastName,
        "dob" to dob,
        "email" to email,
        "photoBase64" to photoBase64,
        "bio" to bio,
        "postsCount" to postsCount,
        "followersCount" to followersCount,
        "followingCount" to followingCount,
        "isProfileComplete" to isProfileComplete
    )

    companion object {
        /** Normalize before saving so usernames are unique and searchable. */
        fun normalizeUsername(raw: String) = raw.trim().lowercase()
    }
}