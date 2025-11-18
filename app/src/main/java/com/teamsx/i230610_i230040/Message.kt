// 1. ENHANCED MESSAGE DATA CLASS
package com.teamsx.i230610_i230040

data class Message(
    val messageId: String = "",
    val senderId: String = "",
    val senderUsername: String = "",
    val text: String = "",
    val timestamp: Long = 0L,
    val isEdited: Boolean = false,
    val isDeleted: Boolean = false,
    val deletedAt: Long = 0L,
    // New fields for media support
    val mediaType: String = "", // "image", "post", or ""
    val mediaUrl: String = "",
    val mediaCaption: String = "",
    // New fields for features
    val senderOnlineStatus: Boolean = false,
    val isScreenshotTaken: Boolean = false,
    val screenshotNotificationSent: Boolean = false
)

// 2. MEDIA MESSAGE DATA CLASS
data class MediaMessage(
    val messageId: String = "",
    val senderId: String = "",
    val senderUsername: String = "",
    val mediaType: String = "", // "image" or "post"
    val mediaUrl: String = "",
    val caption: String = "",
    val timestamp: Long = 0L,
    val isDeleted: Boolean = false
)

// 3. USER ONLINE STATUS DATA CLASS
data class UserStatus(
    val userId: String = "",
    val isOnline: Boolean = false,
    val lastSeen: Long = 0L
)

// 4. SCREENSHOT DETECTION DATA CLASS
data class ScreenshotAlert(
    val alertId: String = "",
    val messageId: String = "",
    val screenshotTakerUserId: String = "",
    val messageOwnerUserId: String = "",
    val timestamp: Long = 0L,
    val notificationSent: Boolean = false
)