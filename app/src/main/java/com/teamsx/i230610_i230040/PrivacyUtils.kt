// COMPLETE PRIVACY & SECURITY UTILITIES
package com.teamsx.i230610_i230040

import android.content.ContentResolver
import android.content.Context
import android.database.ContentObserver
import android.os.Handler
import android.os.Looper
import android.provider.MediaStore
import android.widget.Toast
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase

class ScreenshotDetector(private val context: Context) {

    private val contentResolver: ContentResolver = context.contentResolver
    private var screenshotObserver: ScreenshotObserver? = null
    private var onScreenshotTaken: (() -> Unit)? = null
    private val currentUserId = FirebaseAuth.getInstance().currentUser?.uid ?: ""

    fun startDetection(onScreenshot: () -> Unit) {
        onScreenshotTaken = onScreenshot

        val screenshotsUri = MediaStore.Images.Media.EXTERNAL_CONTENT_URI
        screenshotObserver = ScreenshotObserver(Handler(Looper.getMainLooper())) {
            handleScreenshotDetected()
        }

        contentResolver.registerContentObserver(
            screenshotsUri,
            true,
            screenshotObserver!!
        )
    }

    fun stopDetection() {
        screenshotObserver?.let {
            try {
                contentResolver.unregisterContentObserver(it)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    private fun handleScreenshotDetected() {
        onScreenshotTaken?.invoke()
        Toast.makeText(context, "Screenshot detected", Toast.LENGTH_SHORT).show()

        val screenshotAlert = ScreenshotAlert(
            alertId = FirebaseDatabase.getInstance().reference.push().key ?: "",
            messageId = "",
            screenshotTakerUserId = currentUserId,
            messageOwnerUserId = "",
            timestamp = System.currentTimeMillis(),
            notificationSent = true
        )

        FirebaseDatabase.getInstance().reference
            .child("screenshots")
            .push()
            .setValue(screenshotAlert)
    }
}

class ScreenshotObserver(handler: Handler, private val callback: () -> Unit) : ContentObserver(handler) {

    private var lastScreenshotTime: Long = 0

    override fun onChange(selfChange: Boolean) {
        super.onChange(selfChange)
        val currentTime = System.currentTimeMillis()

        if (currentTime - lastScreenshotTime > 1000) {
            lastScreenshotTime = currentTime
            callback.invoke()
        }
    }
}

class UserStatusManager(private val context: Context) {

    private val currentUserId = FirebaseAuth.getInstance().currentUser?.uid ?: ""
    private val database = FirebaseDatabase.getInstance().reference.child("users").child(currentUserId)

    fun setUserOnline() {
        database.child("isOnline").setValue(true)
        database.child("lastSeen").setValue(System.currentTimeMillis())
    }

    fun setUserOffline() {
        database.child("isOnline").setValue(false)
        database.child("lastSeen").setValue(System.currentTimeMillis())
    }

    fun getUserStatus(userId: String, callback: (Boolean) -> Unit) {
        FirebaseDatabase.getInstance().reference.child("users").child(userId)
            .child("isOnline")
            .get()
            .addOnSuccessListener { snapshot ->
                val isOnline = snapshot.getValue(Boolean::class.java) ?: false
                callback(isOnline)
            }
            .addOnFailureListener {
                callback(false)
            }
    }

    fun getLastSeen(userId: String, callback: (Long) -> Unit) {
        FirebaseDatabase.getInstance().reference.child("users").child(userId)
            .child("lastSeen")
            .get()
            .addOnSuccessListener { snapshot ->
                val lastSeen = snapshot.getValue(Long::class.java) ?: 0L
                callback(lastSeen)
            }
            .addOnFailureListener {
                callback(0L)
            }
    }
}

class MediaUploadManager(private val context: Context) {

    private val storage = com.google.firebase.storage.FirebaseStorage.getInstance()

    fun uploadImage(
        imageUri: android.net.Uri,
        chatId: String,
        messageId: String,
        onSuccess: (String) -> Unit,
        onFailure: (Exception) -> Unit
    ) {
        val fileName = "messages/$chatId/${messageId}.jpg"
        val fileReference = storage.reference.child(fileName)

        fileReference.putFile(imageUri)
            .addOnSuccessListener {
                it.storage.downloadUrl
                    .addOnSuccessListener { downloadUri ->
                        onSuccess(downloadUri.toString())
                    }
                    .addOnFailureListener { exception ->
                        onFailure(exception)
                    }
            }
            .addOnFailureListener { exception ->
                onFailure(exception)
            }
    }

    fun deleteImage(
        chatId: String,
        messageId: String,
        onSuccess: () -> Unit,
        onFailure: (Exception) -> Unit
    ) {
        val fileName = "messages/$chatId/${messageId}.jpg"
        storage.reference.child(fileName)
            .delete()
            .addOnSuccessListener {
                onSuccess()
            }
            .addOnFailureListener { exception ->
                onFailure(exception)
            }
    }
}

class NotificationManager(private val context: Context) {

    fun sendScreenshotNotification(screenshotTakerName: String) {
        val message = "$screenshotTakerName took a screenshot"
        Toast.makeText(context, message, Toast.LENGTH_LONG).show()
    }

    fun sendOnlineStatusNotification(userName: String, isOnline: Boolean) {
        val message = if (isOnline) "$userName is now online" else "$userName is now offline"
        Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
    }

    fun sendMediaReceivedNotification(senderName: String, mediaType: String) {
        val typeLabel = if (mediaType == "image") "photo" else "post"
        val message = "$senderName sent you a $typeLabel"
        Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
    }
}

