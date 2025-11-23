package com.teamsx.i230610_i230040

import android.Manifest
import android.content.ContentValues
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Color
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.provider.MediaStore
import android.text.format.DateUtils
import android.util.Base64
import android.util.Log
import android.widget.EditText
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.teamsx.i230610_i230040.utils.UserPreferences
import com.teamsx.i230610_i230040.network.*
import kotlinx.coroutines.launch
import java.util.Locale

class socialhomescreenchat : AppCompatActivity() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var messageInput: EditText
    private lateinit var sendButton: ImageView
    private lateinit var cameraButton: ImageView
    private lateinit var galleryButton: ImageView
    private lateinit var voiceCallButton: ImageView
    private lateinit var videoCallButton: ImageView
    private lateinit var otherUserNameText: TextView
    private lateinit var onlineStatusText: TextView
    private lateinit var messageAdapter: MessageAdapter
    private val messagesList = mutableListOf<Message>()

    private val userPreferences by lazy { UserPreferences(this) }

    private lateinit var chatId: String
    private lateinit var otherUserName: String
    private lateinit var otherUserId: String
    private val currentUserId: String by lazy { userPreferences.getUser()?.uid ?: "" }
    private val currentUsername: String by lazy { userPreferences.getUser()?.username ?: "Anonymous" }

    private var photoUri: Uri? = null
    private var screenshotDetector: ScreenshotDetector? = null
    private var pendingCallType: CallType? = null

    // Polling for new messages
    private var lastMessageTimestamp: Long = 0L
    private val pollHandler = Handler(Looper.getMainLooper())
    private val pollInterval = 2000L // Poll every 2 seconds
    private var isPolling = false

    private companion object {
        private const val STATUS_ONLINE_LABEL = "Online"
        private const val STATUS_OFFLINE_LABEL = "Offline"
        private const val STATUS_LAST_SEEN_FORMAT = "Last seen %s"
        private val STATUS_ONLINE_COLOR = Color.parseColor("#4CAF50")
        private val STATUS_OFFLINE_COLOR = Color.parseColor("#9E9E9E")
    }

    private enum class CallType { VOICE, VIDEO }

    private fun extractOtherUserIdFromChat(chatId: String, selfId: String): String {
        if (chatId.isEmpty() || selfId.isEmpty()) return ""
        return when {
            chatId.startsWith(selfId) -> chatId.removePrefix(selfId)
            chatId.endsWith(selfId) -> chatId.removeSuffix(selfId)
            else -> ""
        }
    }

    private fun parseOnlineValue(rawValue: Any?): Boolean {
        return when (rawValue) {
            is Boolean -> rawValue
            is Number -> rawValue.toInt() != 0
            is String -> rawValue.equals("true", ignoreCase = true) || rawValue == "1"
            else -> false
        }
    }

    private fun parseLastSeenValue(rawValue: Any?): Long {
        return when (rawValue) {
            is Number -> rawValue.toLong()
            is String -> rawValue.toLongOrNull() ?: 0L
            else -> 0L
        }
    }

    // throttle for screenshot messages
    private var lastScreenshotNotifyAt = 0L

    // Camera launcher
    private val cameraLauncher = registerForActivityResult(ActivityResultContracts.TakePicture()) { success ->
        if (success && photoUri != null) {
            encodeAndSendImage(photoUri!!, "image")
        }
    }

    // Gallery launcher
    private val galleryLauncher = registerForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        if (uri != null) {
            encodeAndSendImage(uri, "image")
        }
    }

    // Permissions launcher (camera)
    private val permissionsLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        when {
            permissions[Manifest.permission.CAMERA] == true -> openCamera()
            else -> Toast.makeText(this, "Camera permission denied", Toast.LENGTH_SHORT).show()
        }
    }

    private val callPermissionsLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val granted = permissions.entries.all { it.value }
        val callType = pendingCallType
        pendingCallType = null

        if (granted && callType != null) {
            launchCall(callType)
        } else if (!granted) {
            Toast.makeText(this, getString(R.string.call_permissions_denied), Toast.LENGTH_SHORT).show()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val incomingChatId = intent.getStringExtra("chatId")
        val incomingUserName = intent.getStringExtra("otherUserName")
        val incomingOtherUserId = intent.getStringExtra("otherUserId")

        if (incomingChatId.isNullOrBlank() || incomingUserName.isNullOrBlank()) {
            finish()
            return
        }

        chatId = incomingChatId
        otherUserName = incomingUserName
        otherUserId = incomingOtherUserId ?: ""
        if (otherUserId.isEmpty()) {
            otherUserId = extractOtherUserIdFromChat(chatId, currentUserId)
        }

        enableEdgeToEdge()
        setContentView(R.layout.activity_socialhomescreenchat)

        // Initialize views
        recyclerView = findViewById(R.id.messagesRecyclerView)
        messageInput = findViewById(R.id.messageInput)
        sendButton = findViewById(R.id.sendButton)
        cameraButton = findViewById(R.id.cameraButton)
        galleryButton = findViewById(R.id.galleryButton)
        voiceCallButton = findViewById(R.id.voiceCallButton)
        videoCallButton = findViewById(R.id.videoCallButton)
        otherUserNameText = findViewById(R.id.otherUserNameText)
        onlineStatusText = findViewById(R.id.onlineStatusText)
        otherUserNameText.text = otherUserName
        onlineStatusText.apply {
            text = STATUS_OFFLINE_LABEL
            setTextColor(STATUS_OFFLINE_COLOR)
        }

        // Setup RecyclerView
        recyclerView.layoutManager = LinearLayoutManager(this).apply { stackFromEnd = true }
        messageAdapter = MessageAdapter(messagesList, currentUserId) { message, action ->
            handleMessageAction(message, action)
        }
        recyclerView.adapter = messageAdapter

        // Update user online status
        if (currentUserId.isNotEmpty()) {
            updateOnlineStatus(true)
        }

        // Screenshot detection
        screenshotDetector = ScreenshotDetector(this)
        screenshotDetector?.startDetection {
            Toast.makeText(this@socialhomescreenchat, "Screenshot detected!", Toast.LENGTH_SHORT).show()
            sendScreenshotNotice()
        }

        // Back button
        val backButton = findViewById<ImageView>(R.id.cameralogo)
        backButton.setOnClickListener {
            val intent = Intent(this, socialhomescreen4::class.java)
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP)
            startActivity(intent)
            finish()
        }

        // Send text
        sendButton.setOnClickListener {
            val messageText = messageInput.text.toString().trim()
            if (messageText.isNotEmpty()) {
                sendMessage(messageText)
                messageInput.text.clear()
            }
        }

        // Camera button
        cameraButton.setOnClickListener { checkCameraPermissionAndOpen() }

        // Gallery button
        galleryButton.setOnClickListener { galleryLauncher.launch("image/*") }

        voiceCallButton.setOnClickListener { startCallWithPermissions(CallType.VOICE) }
        videoCallButton.setOnClickListener { startCallWithPermissions(CallType.VIDEO) }

        // Load initial messages and start polling
        loadInitialMessages()
        startMessagePolling()
    }

    // Update user online status via API
    private fun updateOnlineStatus(isOnline: Boolean) {
        lifecycleScope.launch {
            try {
                val request = UpdateStatusRequest(currentUserId, isOnline)
                RetrofitInstance.apiService.updateStatus(request)
            } catch (e: Exception) {
                Log.e("socialhomescreenchat", "Failed to update status", e)
            }
        }
    }

    // Load initial messages
    private fun loadInitialMessages() {
        lifecycleScope.launch {
            try {
                val request = GetMessagesRequest(chatId, limit = 50)
                val response = RetrofitInstance.apiService.getMessages(request)

                if (response.isSuccessful && response.body()?.success == true) {
                    val apiMessages = response.body()?.data?.messages ?: emptyList()

                    messagesList.clear()
                    apiMessages.forEach { apiMsg ->
                        val msg = Message(
                            messageId = apiMsg.messageId,
                            senderId = apiMsg.senderId,
                            senderUsername = apiMsg.senderUsername,
                            text = apiMsg.text,
                            timestamp = apiMsg.timestamp,
                            isEdited = apiMsg.isEdited,
                            isDeleted = apiMsg.isDeleted,
                            deletedAt = apiMsg.deletedAt,
                            mediaType = apiMsg.mediaType,
                            mediaUrl = apiMsg.mediaUrl,
                            mediaCaption = apiMsg.mediaCaption
                        )
                        messagesList.add(msg)
                        if (msg.timestamp > lastMessageTimestamp) {
                            lastMessageTimestamp = msg.timestamp
                        }
                    }

                    messageAdapter.notifyDataSetChanged()
                    if (messagesList.isNotEmpty()) {
                        recyclerView.scrollToPosition(messagesList.size - 1)
                    }
                }
            } catch (e: Exception) {
                Log.e("socialhomescreenchat", "Error loading messages", e)
                Toast.makeText(this@socialhomescreenchat, "Error loading messages", Toast.LENGTH_SHORT).show()
            }
        }
    }

    // Start polling for new messages
    private fun startMessagePolling() {
        if (isPolling) return
        isPolling = true

        pollHandler.postDelayed(object : Runnable {
            override fun run() {
                pollNewMessages()
                if (isPolling) {
                    pollHandler.postDelayed(this, pollInterval)
                }
            }
        }, pollInterval)
    }

    // Poll for new messages
    private fun pollNewMessages() {
        lifecycleScope.launch {
            try {
                val request = PollNewMessagesRequest(chatId, lastMessageTimestamp)
                val response = RetrofitInstance.apiService.pollNewMessages(request)

                if (response.isSuccessful && response.body()?.success == true) {
                    val newMessages = response.body()?.data?.messages ?: emptyList()

                    newMessages.forEach { apiMsg ->
                        // Check if message already exists (in case of edit/delete)
                        val existingIndex = messagesList.indexOfFirst { it.messageId == apiMsg.messageId }

                        if (existingIndex != -1) {
                            // Update existing message
                            val msg = Message(
                                messageId = apiMsg.messageId,
                                senderId = apiMsg.senderId,
                                senderUsername = apiMsg.senderUsername,
                                text = apiMsg.text,
                                timestamp = apiMsg.timestamp,
                                isEdited = apiMsg.isEdited,
                                isDeleted = apiMsg.isDeleted,
                                deletedAt = apiMsg.deletedAt,
                                mediaType = apiMsg.mediaType,
                                mediaUrl = apiMsg.mediaUrl,
                                mediaCaption = apiMsg.mediaCaption
                            )
                            messagesList[existingIndex] = msg
                            messageAdapter.notifyItemChanged(existingIndex)
                        } else {
                            // Add new message
                            val msg = Message(
                                messageId = apiMsg.messageId,
                                senderId = apiMsg.senderId,
                                senderUsername = apiMsg.senderUsername,
                                text = apiMsg.text,
                                timestamp = apiMsg.timestamp,
                                isEdited = apiMsg.isEdited,
                                isDeleted = apiMsg.isDeleted,
                                deletedAt = apiMsg.deletedAt,
                                mediaType = apiMsg.mediaType,
                                mediaUrl = apiMsg.mediaUrl,
                                mediaCaption = apiMsg.mediaCaption
                            )
                            messagesList.add(msg)
                            messageAdapter.notifyItemInserted(messagesList.size - 1)
                            recyclerView.scrollToPosition(messagesList.size - 1)
                        }

                        if (apiMsg.timestamp > lastMessageTimestamp) {
                            lastMessageTimestamp = apiMsg.timestamp
                        }
                    }
                }
            } catch (e: Exception) {
                Log.e("socialhomescreenchat", "Error polling messages", e)
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        isPolling = false
        pollHandler.removeCallbacksAndMessages(null)
        updateOnlineStatus(false)
        screenshotDetector?.stopDetection()
    }

    // ===== Screenshot notice to chat =====
    private fun sendScreenshotNotice() {
        val now = System.currentTimeMillis()
        if (now - lastScreenshotNotifyAt < 8000) return // throttle 8s
        lastScreenshotNotifyAt = now

        lifecycleScope.launch {
            try {
                val request = SendMessageRequest(
                    chatId = chatId,
                    senderId = currentUserId,
                    senderUsername = currentUsername,
                    text = "⚠️ Screenshot was detected."
                )
                RetrofitInstance.apiService.sendMessage(request)
            } catch (e: Exception) {
                Log.e("socialhomescreenchat", "Failed to send screenshot notice", e)
            }
        }
    }

    private fun startCallWithPermissions(type: CallType) {
        val permissions = callPermissionsFor(type)
        val hasPermissions = permissions.all {
            ContextCompat.checkSelfPermission(this, it) == PackageManager.PERMISSION_GRANTED
        }

        if (hasPermissions) {
            launchCall(type)
        } else {
            pendingCallType = type
            callPermissionsLauncher.launch(permissions)
        }
    }

    private fun callPermissionsFor(type: CallType): Array<String> {
        val required = mutableListOf(Manifest.permission.RECORD_AUDIO)
        if (type == CallType.VIDEO) {
            required += Manifest.permission.CAMERA
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            required += Manifest.permission.BLUETOOTH_CONNECT
        }
        return required.toTypedArray()
    }

    private fun launchCall(type: CallType) {
        val intent = when (type) {
            CallType.VOICE -> AgoraCallActivity.voiceCallIntent(this, chatId, otherUserName)
            CallType.VIDEO -> AgoraCallActivity.videoCallIntent(this, chatId, otherUserName)
        }
        startActivity(intent)
    }

    // ===== Permissions & Camera =====

    private fun checkCameraPermissionAndOpen() {
        when {
            ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED -> {
                openCamera()
            }
            else -> {
                permissionsLauncher.launch(arrayOf(Manifest.permission.CAMERA))
            }
        }
    }

    private fun openCamera() {
        val values = ContentValues().apply {
            put(MediaStore.Images.Media.TITLE, "New Picture")
            put(MediaStore.Images.Media.DESCRIPTION, "From Camera")
        }
        photoUri = contentResolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values)
        photoUri?.let { cameraLauncher.launch(it) }
    }

    // ===== Image → Base64 and send via API =====

    private fun encodeAndSendImage(uri: Uri, type: String) {
        val base64 = uriToBase64(uri, maxSide = 1280, jpegQuality = 80)
        if (base64 == null) {
            Toast.makeText(this, "Failed to read image", Toast.LENGTH_LONG).show()
            return
        }

        lifecycleScope.launch {
            try {
                val request = SendMessageRequest(
                    chatId = chatId,
                    senderId = currentUserId,
                    senderUsername = currentUsername,
                    text = "",
                    mediaType = type,
                    mediaUrl = base64,
                    mediaCaption = messageInput.text.toString().trim()
                )

                val response = RetrofitInstance.apiService.sendMessage(request)

                if (response.isSuccessful && response.body()?.success == true) {
                    Toast.makeText(this@socialhomescreenchat, "Image sent", Toast.LENGTH_SHORT).show()
                    messageInput.text.clear()
                } else {
                    Toast.makeText(this@socialhomescreenchat, "Failed to send image", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                Log.e("socialhomescreenchat", "Error sending image", e)
                Toast.makeText(this@socialhomescreenchat, "Error: ${e.message}", Toast.LENGTH_LONG).show()
            }
        }
    }

    private fun uriToBase64(uri: Uri, maxSide: Int = 1280, jpegQuality: Int = 80): String? {
        return try {
            contentResolver.openInputStream(uri).use { input ->
                if (input == null) return null
                val original = BitmapFactory.decodeStream(input) ?: return null
                val scaled = scaleBitmapKeepingAspect(original, maxSide)
                val out = java.io.ByteArrayOutputStream()
                scaled.compress(Bitmap.CompressFormat.JPEG, jpegQuality, out)
                val bytes = out.toByteArray()
                Base64.encodeToString(bytes, Base64.NO_WRAP)
            }
        } catch (_: Exception) {
            null
        }
    }

    private fun scaleBitmapKeepingAspect(src: Bitmap, maxSide: Int): Bitmap {
        val w = src.width
        val h = src.height
        val maxDim = maxOf(w, h)
        if (maxDim <= maxSide) return src
        val scale = maxSide.toFloat() / maxDim.toFloat()
        val newW = (w * scale).toInt()
        val newH = (h * scale).toInt()
        return Bitmap.createScaledBitmap(src, newW, newH, true)
    }

    // ===== Text messages =====

    private fun sendMessage(text: String) {
        lifecycleScope.launch {
            try {
                val request = SendMessageRequest(
                    chatId = chatId,
                    senderId = currentUserId,
                    senderUsername = currentUsername,
                    text = text
                )

                val response = RetrofitInstance.apiService.sendMessage(request)

                if (response.isSuccessful && response.body()?.success == true) {
                    // Message sent successfully - polling will fetch it
                } else {
                    val errorMsg = response.body()?.error ?: "Failed to send message"
                    Toast.makeText(this@socialhomescreenchat, errorMsg, Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                Log.e("socialhomescreenchat", "Error sending message", e)
                Toast.makeText(this@socialhomescreenchat, "Error: ${e.message}", Toast.LENGTH_LONG).show()
            }
        }
    }


    // ===== Edit / Delete =====

    private fun handleMessageAction(message: Message, action: String) {
        when (action) {
            "EDIT" -> if (canEditMessage(message)) editMessage(message)
            "DELETE" -> if (canDeleteMessage(message)) deleteMessage(message)
        }
    }

    private fun canEditMessage(message: Message): Boolean {
        val minutes = (System.currentTimeMillis() - message.timestamp) / 1000 / 60
        return message.senderId == currentUserId && minutes <= 5 && !message.isDeleted && message.mediaType.isEmpty()
    }

    private fun canDeleteMessage(message: Message): Boolean {
        val minutes = (System.currentTimeMillis() - message.timestamp) / 1000 / 60
        return message.senderId == currentUserId && minutes <= 5 && !message.isDeleted
    }

    private fun editMessage(message: Message) {
        val dialog = androidx.appcompat.app.AlertDialog.Builder(this)
        val editText = EditText(this)
        editText.setText(message.text)

        dialog.setTitle("Edit Message")
            .setView(editText)
            .setPositiveButton("Save") { _, _ ->
                val newText = editText.text.toString().trim()
                if (newText.isNotEmpty() && newText != message.text) {
                    lifecycleScope.launch {
                        try {
                            val request = EditMessageRequest(message.messageId, newText)
                            val response = RetrofitInstance.apiService.editMessage(request)

                            if (response.isSuccessful && response.body()?.success == true) {
                                Toast.makeText(this@socialhomescreenchat, "Message edited", Toast.LENGTH_SHORT).show()
                                // Update local message
                                val idx = messagesList.indexOfFirst { it.messageId == message.messageId }
                                if (idx != -1) {
                                    messagesList[idx] = message.copy(text = newText, isEdited = true)
                                    messageAdapter.notifyItemChanged(idx)
                                }
                            } else {
                                Toast.makeText(this@socialhomescreenchat, "Failed to edit message", Toast.LENGTH_SHORT).show()
                            }
                        } catch (e: Exception) {
                            Log.e("socialhomescreenchat", "Error editing message", e)
                            Toast.makeText(this@socialhomescreenchat, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
                        }
                    }
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun deleteMessage(message: Message) {
        lifecycleScope.launch {
            try {
                val request = DeleteMessageRequest(message.messageId)
                val response = RetrofitInstance.apiService.deleteMessage(request)

                if (response.isSuccessful && response.body()?.success == true) {
                    Toast.makeText(this@socialhomescreenchat, "Message deleted", Toast.LENGTH_SHORT).show()
                    // Update local message
                    val idx = messagesList.indexOfFirst { it.messageId == message.messageId }
                    if (idx != -1) {
                        messagesList[idx] = message.copy(
                            isDeleted = true,
                            text = "[This message was deleted]",
                            deletedAt = System.currentTimeMillis()
                        )
                        messageAdapter.notifyItemChanged(idx)
                    }
                } else {
                    Toast.makeText(this@socialhomescreenchat, "Failed to delete message", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                Log.e("socialhomescreenchat", "Error deleting message", e)
                Toast.makeText(this@socialhomescreenchat, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }
}