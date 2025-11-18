package com.teamsx.i230610_i230040

data class Story(
    val storyId: String = "",
    val userId: String = "",
    val username: String = "",
    val userPhotoBase64: String? = null,
    val imageBase64: String = "",
    val timestamp: Long = 0L,
    val expiresAt: Long = 0L,
    val viewedBy: Map<String, Boolean> = emptyMap(), // uid -> true
    val isCloseFreindsOnly: Boolean = false
) {
    // Helper to check if story is still valid (within 24 hours)
    fun isValid(): Boolean {
        return System.currentTimeMillis() < expiresAt
    }

    // Helper to check if viewed by specific user
    fun isViewedBy(userId: String): Boolean {
        return viewedBy.containsKey(userId)
    }
}

data class StoryGroup(
    val userId: String,
    val username: String,
    val userPhotoBase64: String?,
    val stories: List<Story>,
    val hasUnviewedStories: Boolean
)