package com.teamsx.i230610_i230040.services

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import com.teamsx.i230610_i230040.HomeActivity
import com.teamsx.i230610_i230040.R
import com.teamsx.i230610_i230040.network.RetrofitInstance
import com.teamsx.i230610_i230040.network.SaveFCMTokenRequest
import com.teamsx.i230610_i230040.utils.UserPreferences
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class MyFirebaseMessagingService : FirebaseMessagingService() {

    companion object {
        private const val TAG = "FCMService"
        private const val CHANNEL_ID = "socially_notifications"
        private const val CHANNEL_NAME = "Socially Notifications"
    }

    override fun onNewToken(token: String) {
        super.onNewToken(token)
        Log.d(TAG, "New FCM token: $token")

        // Save token to server
        val userPreferences = UserPreferences(this)
        val currentUser = userPreferences.getUser()

        if (currentUser != null) {
            saveFCMTokenToServer(currentUser.uid, token)
        }

        // Save locally for later use
        userPreferences.saveFCMToken(token)
    }

    override fun onMessageReceived(message: RemoteMessage) {
        super.onMessageReceived(message)
        Log.d(TAG, "Message received from: ${message.from}")

        // Handle notification payload
        message.notification?.let { notification ->
            val title = notification.title ?: "Socially"
            val body = notification.body ?: ""
            val type = message.data["type"] ?: ""

            showNotification(title, body, type, message.data)
        }

        // Handle data payload (when app is in foreground)
        if (message.data.isNotEmpty()) {
            Log.d(TAG, "Message data: ${message.data}")
            val type = message.data["type"] ?: ""
            val title = message.data["title"] ?: "Socially"
            val body = message.data["message"] ?: message.data["body"] ?: ""

            if (message.notification == null) {
                // No notification payload, create one from data
                showNotification(title, body, type, message.data)
            }
        }
    }

    private fun showNotification(title: String, body: String, type: String, data: Map<String, String>) {
        createNotificationChannel()

        // Create intent based on notification type
        val intent = Intent(this, HomeActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP

            when (type) {
                "follow_request", "follow_accepted" -> {
                    putExtra("open_notifications", true)
                    putExtra("notification_type", type)
                }
            }

            // Add all data as extras
            data.forEach { (key, value) ->
                putExtra(key, value)
            }
        }

        val pendingIntent = PendingIntent.getActivity(
            this,
            System.currentTimeMillis().toInt(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notificationBuilder = NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle(title)
            .setContentText(body)
            .setSmallIcon(R.drawable.ic_notification)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setDefaults(NotificationCompat.DEFAULT_ALL)

        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.notify(System.currentTimeMillis().toInt(), notificationBuilder.build())
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                CHANNEL_NAME,
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Notifications for follow requests, likes, comments, etc."
                enableLights(true)
                enableVibration(true)
            }

            val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }

    private fun saveFCMTokenToServer(uid: String, token: String) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val request = SaveFCMTokenRequest(uid, token)
                val response = RetrofitInstance.apiService.saveFCMToken(request)

                if (response.isSuccessful && response.body()?.success == true) {
                    Log.d(TAG, "FCM token saved to server successfully")
                } else {
                    Log.e(TAG, "Failed to save FCM token: ${response.body()?.error}")
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error saving FCM token", e)
            }
        }
    }
}

