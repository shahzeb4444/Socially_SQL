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
import com.teamsx.i230610_i230040.repository.MessageRepository
import com.teamsx.i230610_i230040.utils.NetworkMonitor
import com.teamsx.i230610_i230040.database.entity.MessageEntity
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
    private lateinit var messageRepository: MessageRepository
    private lateinit var networkMonitor: NetworkMonitor

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

    // Status polling
    private val statusPollHandler = Handler(Looper.getMainLooper())
    private val statusPollInterval = 5000L // Poll status every 5 seconds
    private var isStatusPolling = false

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

        // Initialize offline support components
        messageRepository = MessageRepository(this)
        networkMonitor = NetworkMonitor(this)

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
                showVanishModeDialog(messageText, isMedia = false)
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
        startStatusPolling()
        observeNetworkChanges()
        observeMessagesFromDatabase()
    }

    // Observe network state changes
    private fun observeNetworkChanges() {
        networkMonitor.observe(this) { isOnline ->
            Log.d("socialhomescreenchat", "Network state: ${if (isOnline) "ONLINE" else "OFFLINE"}")
            if (isOnline) {
                // Sync messages when coming back online
                syncMessagesWithServer()
            }
        }
    }

    // Observe messages from local database (auto-updates UI)
    private fun observeMessagesFromDatabase() {
        messageRepository.getMessagesForChat(chatId).observe(this) { entities ->
            // Convert entities to UI messages
            messagesList.clear()
            entities.forEach { entity ->
                // Skip messages that have vanished for this user
                val vanishedForUsers = entity.vanishedFor.split(",").map { it.trim() }
                if (!vanishedForUsers.contains(currentUserId)) {
                    val msg = Message(
                        messageId = entity.messageId,
                        senderId = entity.senderId,
                        senderUsername = entity.senderUsername,
                        text = entity.text,
                        timestamp = entity.timestamp,
                        isEdited = entity.isEdited,
                        isDeleted = entity.isDeleted,
                        deletedAt = entity.deletedAt,
                        mediaType = entity.mediaType,
                        mediaUrl = entity.mediaUrl,
                        mediaCaption = entity.mediaCaption,
                        isVanishMode = entity.isVanishMode,
                        viewedBy = entity.viewedBy,
                        vanishedFor = entity.vanishedFor
                    )
                    messagesList.add(msg)
                    if (msg.timestamp > lastMessageTimestamp) {
                        lastMessageTimestamp = msg.timestamp
                    }
                }
            }

            messageAdapter.notifyDataSetChanged()
            if (messagesList.isNotEmpty()) {
                recyclerView.scrollToPosition(messagesList.size - 1)
            }
        }
    }

    // Sync messages with server (fetch and update local database)
    private fun syncMessagesWithServer() {
        lifecycleScope.launch {
            try {
                messageRepository.fetchMessagesFromServer(chatId, currentUserId)
            } catch (e: Exception) {
                Log.e("socialhomescreenchat", "Error syncing messages", e)
            }
        }
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

    // Load initial messages - now uses repository for offline support
    private fun loadInitialMessages() {
        lifecycleScope.launch {
            try {
                // Fetch from server and update local database
                messageRepository.fetchMessagesFromServer(chatId, currentUserId)

                // Mark messages as viewed when loading
                markMessagesAsViewed()
            } catch (e: Exception) {
                Log.e("socialhomescreenchat", "Error loading messages", e)
                // Messages will still load from local database via LiveData observer
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
                val request = PollNewMessagesRequest(chatId, lastMessageTimestamp, viewerId = currentUserId)
                val response = RetrofitInstance.apiService.pollNewMessages(request)

                if (response.isSuccessful && response.body()?.success == true) {
                    val newMessages = response.body()?.data?.messages ?: emptyList()

                    newMessages.forEach { apiMsg ->
                        // Skip messages that have vanished for this user
                        val vanishedForUsers = apiMsg.vanishedFor.split(",").map { it.trim() }
                        if (vanishedForUsers.contains(currentUserId)) {
                            // Remove from list if it exists
                            val existingIndex = messagesList.indexOfFirst { it.messageId == apiMsg.messageId }
                            if (existingIndex != -1) {
                                messagesList.removeAt(existingIndex)
                                messageAdapter.notifyItemRemoved(existingIndex)
                            }
                            return@forEach
                        }

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
                                mediaCaption = apiMsg.mediaCaption,
                                isVanishMode = apiMsg.isVanishMode,
                                viewedBy = apiMsg.viewedBy,
                                vanishedFor = apiMsg.vanishedFor
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
                                mediaCaption = apiMsg.mediaCaption,
                                isVanishMode = apiMsg.isVanishMode,
                                viewedBy = apiMsg.viewedBy,
                                vanishedFor = apiMsg.vanishedFor
                            )
                            messagesList.add(msg)
                            messageAdapter.notifyItemInserted(messagesList.size - 1)
                            recyclerView.scrollToPosition(messagesList.size - 1)
                        }

                        if (apiMsg.timestamp > lastMessageTimestamp) {
                            lastMessageTimestamp = apiMsg.timestamp
                        }
                    }

                    // Mark new messages as viewed
                    if (newMessages.isNotEmpty()) {
                        markMessagesAsViewed()
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
        isStatusPolling = false
        statusPollHandler.removeCallbacksAndMessages(null)
        updateOnlineStatus(false)
        screenshotDetector?.stopDetection()

        // Trigger vanish for messages when user exits chat
        triggerVanishOnExit()
    }

    // Mark messages as viewed when user is in chat
    private fun markMessagesAsViewed() {
        lifecycleScope.launch {
            try {
                val request = MarkMessagesViewedRequest(chatId, currentUserId)
                RetrofitInstance.apiService.markMessagesViewed(request)
            } catch (e: Exception) {
                Log.e("socialhomescreenchat", "Error marking messages as viewed", e)
            }
        }
    }

    // Trigger vanish for viewed messages when user exits chat
    private fun triggerVanishOnExit() {
        lifecycleScope.launch {
            try {
                val request = TriggerVanishRequest(chatId, currentUserId)
                val response = RetrofitInstance.apiService.triggerVanish(request)

                if (response.isSuccessful && response.body()?.success == true) {
                    val vanishedCount = response.body()?.data?.vanishedCount ?: 0
                    Log.d("socialhomescreenchat", "Vanished $vanishedCount messages on exit")
                }
            } catch (e: Exception) {
                Log.e("socialhomescreenchat", "Error triggering vanish", e)
            }
        }
    }

    // Start polling for other user's online status
    private fun startStatusPolling() {
        if (isStatusPolling || otherUserId.isEmpty()) return
        isStatusPolling = true

        // Initial check
        checkUserStatus()
        updateOwnHeartbeat()

        // Poll every 5 seconds
        statusPollHandler.postDelayed(object : Runnable {
            override fun run() {
                checkUserStatus()
                updateOwnHeartbeat() // Update own last_seen as heartbeat
                if (isStatusPolling) {
                    statusPollHandler.postDelayed(this, statusPollInterval)
                }
            }
        }, statusPollInterval)
    }

    // Update own last_seen timestamp as heartbeat
    private fun updateOwnHeartbeat() {
        lifecycleScope.launch {
            try {
                val request = UpdateStatusRequest(currentUserId, true)
                RetrofitInstance.apiService.updateStatus(request)
            } catch (e: Exception) {
                Log.e("socialhomescreenchat", "Error updating heartbeat", e)
            }
        }
    }

    // Check if other user is online
    private fun checkUserStatus() {
        if (otherUserId.isEmpty()) return

        lifecycleScope.launch {
            try {
                val request = GetUserStatusRequest(otherUserId)
                val response = RetrofitInstance.apiService.getUserStatus(request)

                if (response.isSuccessful && response.body()?.success == true) {
                    val statusData = response.body()?.data
                    if (statusData != null) {
                        updateStatusUI(statusData.isOnline, statusData.lastSeen)
                    }
                }
            } catch (e: Exception) {
                Log.e("socialhomescreenchat", "Error checking user status", e)
            }
        }
    }

    // Update the status UI (Online/Offline)
    private fun updateStatusUI(isOnline: Boolean, lastSeen: Long) {
        runOnUiThread {
            if (isOnline) {
                onlineStatusText.text = "Online"
                onlineStatusText.setTextColor(Color.parseColor("#4CAF50")) // Green
            } else {
                // Calculate time ago
                val timeAgo = getTimeAgo(lastSeen)
                onlineStatusText.text = if (timeAgo.isNotEmpty()) "Last seen $timeAgo" else "Offline"
                onlineStatusText.setTextColor(Color.parseColor("#9E9E9E")) // Gray
            }
        }
    }

    // Get human-readable time ago
    private fun getTimeAgo(timestamp: Long): String {
        if (timestamp == 0L) return ""

        val now = System.currentTimeMillis()
        val diff = now - timestamp

        return when {
            diff < 60000 -> "just now" // Less than 1 minute
            diff < 3600000 -> "${diff / 60000}m ago" // Less than 1 hour
            diff < 86400000 -> "${diff / 3600000}h ago" // Less than 1 day
            diff < 604800000 -> "${diff / 86400000}d ago" // Less than 1 week
            else -> "a while ago"
        }
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

        val caption = messageInput.text.toString().trim()
        showVanishModeDialog("", isMedia = true, mediaType = type, mediaUrl = base64, caption = caption)
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

    private fun showVanishModeDialog(text: String, isMedia: Boolean = false, mediaType: String = "", mediaUrl: String = "", caption: String = "") {
        val dialog = androidx.appcompat.app.AlertDialog.Builder(this)
        dialog.setTitle("Send Message")
            .setMessage("Would you like to send this message in vanish mode?\n\nVanish mode: Message will disappear from recipient's chat after they view it and close the chat.")
            .setPositiveButton("Vanish Mode") { _, _ ->
                if (isMedia) {
                    sendMediaMessage(mediaType, mediaUrl, caption, vanishMode = true)
                } else {
                    sendMessage(text, vanishMode = true)
                    messageInput.text.clear()
                }
            }
            .setNegativeButton("Normal Mode") { _, _ ->
                if (isMedia) {
                    sendMediaMessage(mediaType, mediaUrl, caption, vanishMode = false)
                } else {
                    sendMessage(text, vanishMode = false)
                    messageInput.text.clear()
                }
            }
            .setNeutralButton("Cancel", null)
            .show()
    }

    private fun sendMessage(text: String, vanishMode: Boolean = false) {
        lifecycleScope.launch {
            try {
                // Use repository for offline-first sending
                messageRepository.sendMessage(
                    chatId = chatId,
                    senderId = currentUserId,
                    senderUsername = currentUsername,
                    text = text,
                    isVanishMode = vanishMode
                )

                // Message saved locally and queued for sync
                if (vanishMode) {
                    Toast.makeText(this@socialhomescreenchat, "Sent in vanish mode 👻", Toast.LENGTH_SHORT).show()
                }

                // UI will update automatically via LiveData observer
            } catch (e: Exception) {
                Log.e("socialhomescreenchat", "Error sending message", e)
                Toast.makeText(this@socialhomescreenchat, "Error: ${e.message}", Toast.LENGTH_LONG).show()
            }
        }
    }

    private fun sendMediaMessage(mediaType: String, mediaUrl: String, caption: String, vanishMode: Boolean = false) {
        lifecycleScope.launch {
            try {
                // Use repository for offline-first media sending
                messageRepository.sendMessage(
                    chatId = chatId,
                    senderId = currentUserId,
                    senderUsername = currentUsername,
                    text = "",
                    mediaType = mediaType,
                    mediaUrl = mediaUrl,
                    mediaCaption = caption,
                    isVanishMode = vanishMode
                )

                Toast.makeText(this@socialhomescreenchat, if (vanishMode) "Image sent in vanish mode 👻" else "Image sent", Toast.LENGTH_SHORT).show()
                messageInput.text.clear()

                // UI will update automatically via LiveData observer
            } catch (e: Exception) {
                Log.e("socialhomescreenchat", "Error sending image", e)
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
                            // Use repository for offline-first editing
                            messageRepository.editMessage(message.messageId, newText)
                            Toast.makeText(this@socialhomescreenchat, "Message edited", Toast.LENGTH_SHORT).show()

                            // UI will update automatically via LiveData observer
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
                // Use repository for offline-first deletion
                messageRepository.deleteMessage(message.messageId)
                Toast.makeText(this@socialhomescreenchat, "Message deleted", Toast.LENGTH_SHORT).show()

                // UI will update automatically via LiveData observer
            } catch (e: Exception) {
                Log.e("socialhomescreenchat", "Error deleting message", e)
                Toast.makeText(this@socialhomescreenchat, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }
}