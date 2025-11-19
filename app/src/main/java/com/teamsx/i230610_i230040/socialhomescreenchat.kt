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
import android.provider.MediaStore
import android.text.format.DateUtils
import android.util.Base64
import android.widget.EditText
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.database.ChildEventListener
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ServerValue
import com.google.firebase.database.ValueEventListener
import com.teamsx.i230610_i230040.utils.UserPreferences
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

    private val db by lazy { FirebaseDatabase.getInstance().reference }
    private val userPreferences by lazy { UserPreferences(this) }

    private lateinit var chatId: String
    private lateinit var otherUserName: String
    private lateinit var otherUserId: String
    private val currentUserId: String by lazy { userPreferences.getUser()?.uid ?: "" }
    private val currentUsername: String by lazy { userPreferences.getUser()?.username ?: "Anonymous" }

    // Database reference for chat messages
    private val database: DatabaseReference by lazy {
        FirebaseDatabase.getInstance().reference.child("chats").child(chatId)
    }

    private var photoUri: Uri? = null
    private var otherUserStatusListener: ValueEventListener? = null
    private var screenshotDetector: ScreenshotDetector? = null
    private var pendingCallType: CallType? = null

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

        // Database reference
        if (currentUserId.isNotEmpty()) {
            val userStatusRef = db.child("users").child(currentUserId)
            userStatusRef.child("isOnline").onDisconnect().setValue(false)
            userStatusRef.child("lastSeen").onDisconnect().setValue(ServerValue.TIMESTAMP)
        }

        // Screenshot detection
        screenshotDetector = ScreenshotDetector(this)
        screenshotDetector?.startDetection {
            Toast.makeText(this@socialhomescreenchat, "Screenshot detected!", Toast.LENGTH_SHORT).show()
            sendScreenshotNotice() // <-- add notice into this chat
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

        // Real-time load
        loadMessagesRealTime()

        // Monitor other user's online status
        monitorUserStatus()
    }

    // ===== Screenshot notice to chat =====
    private fun sendScreenshotNotice() {
        val now = System.currentTimeMillis()
        if (now - lastScreenshotNotifyAt < 8000) return // throttle 8s
        lastScreenshotNotifyAt = now

        val messageId = database.push().key ?: return
        val msg = Message(
            messageId = messageId,
            senderId = currentUserId,
            senderUsername = currentUsername,
            text = "⚠️ Screenshot was detected.",
            timestamp = now,
            isEdited = false,
            isDeleted = false,
            mediaType = "",
            mediaUrl = "",
            mediaCaption = ""
        )
        database.child(messageId).setValue(msg)
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

    // ===== Image → Base64 and send to Realtime DB =====

    private fun encodeAndSendImage(uri: Uri, type: String) {
        val messageId = database.push().key ?: return
        val timestamp = System.currentTimeMillis()

        val base64 = uriToBase64(uri, maxSide = 1280, jpegQuality = 80)
        if (base64 == null) {
            Toast.makeText(this, "Failed to read image", Toast.LENGTH_LONG).show()
            return
        }

        val msg = Message(
            messageId = messageId,
            senderId = currentUserId,
            senderUsername = currentUsername,
            text = "",
            timestamp = timestamp,
            mediaType = type,          // "image"
            mediaUrl = base64,         // base64 here
            mediaCaption = messageInput.text.toString().trim(),
            isEdited = false,
            isDeleted = false
        )

        database.child(messageId).setValue(msg)
            .addOnSuccessListener {
                Toast.makeText(this, "Image sent", Toast.LENGTH_SHORT).show()
                messageInput.text.clear()
            }
            .addOnFailureListener { e ->
                Toast.makeText(this, "Failed: ${e.message}", Toast.LENGTH_LONG).show()
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
        val messageId = database.push().key ?: return
        val timestamp = System.currentTimeMillis()

        val message = Message(
            messageId = messageId,
            senderId = currentUserId,
            senderUsername = currentUsername,
            text = text,
            timestamp = timestamp,
            isEdited = false,
            isDeleted = false
        )

        database.child(messageId).setValue(message)
            .addOnSuccessListener { Toast.makeText(this, "Message sent", Toast.LENGTH_SHORT).show() }
            .addOnFailureListener { e ->
                Toast.makeText(this, "Failed: ${e.message}", Toast.LENGTH_LONG).show()
                android.util.Log.e("SendMessage", "Error: ", e)
            }
    }

    // ===== Realtime load =====

    private fun loadMessagesRealTime() {
        database.addChildEventListener(object : ChildEventListener {
            override fun onChildAdded(snapshot: DataSnapshot, previousChildName: String?) {
                val message = snapshot.getValue(Message::class.java) ?: return
                messagesList.add(message)
                messageAdapter.notifyItemInserted(messagesList.size - 1)
                recyclerView.scrollToPosition(messagesList.size - 1)
            }

            override fun onChildChanged(snapshot: DataSnapshot, previousChildName: String?) {
                val updated = snapshot.getValue(Message::class.java) ?: return
                val idx = messagesList.indexOfFirst { it.messageId == updated.messageId }
                if (idx != -1) {
                    messagesList[idx] = updated
                    messageAdapter.notifyItemChanged(idx)
                }
            }

            override fun onChildRemoved(snapshot: DataSnapshot) {}
            override fun onChildMoved(snapshot: DataSnapshot, previousChildName: String?) {}
            override fun onCancelled(error: DatabaseError) {
                Toast.makeText(this@socialhomescreenchat, "Error loading messages", Toast.LENGTH_SHORT).show()
            }
        })
    }

    // ===== Presence =====

    private fun monitorUserStatus() {
        if (otherUserId.isEmpty()) return

        val listener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val isOnline = parseOnlineValue(snapshot.child("isOnline").value)
                val lastSeen = parseLastSeenValue(snapshot.child("lastSeen").value)
                updateOnlineStatusUI(isOnline, lastSeen)
            }

            override fun onCancelled(error: DatabaseError) {}
        }
        FirebaseDatabase.getInstance().reference.child("users").child(otherUserId)
            .addValueEventListener(listener)
        otherUserStatusListener = listener
    }

    private fun updateOnlineStatusUI(isOnline: Boolean, lastSeen: Long) {
        if (!::onlineStatusText.isInitialized) return

        if (isOnline) {
            onlineStatusText.text = STATUS_ONLINE_LABEL
            onlineStatusText.setTextColor(STATUS_ONLINE_COLOR)
        } else {
            val statusText = if (lastSeen > 0L) {
                val relativeTime = DateUtils.getRelativeTimeSpanString(
                    lastSeen,
                    System.currentTimeMillis(),
                    DateUtils.MINUTE_IN_MILLIS
                ).toString()
                String.format(Locale.getDefault(), STATUS_LAST_SEEN_FORMAT, relativeTime)
            } else {
                STATUS_OFFLINE_LABEL
            }
            onlineStatusText.text = statusText
            onlineStatusText.setTextColor(STATUS_OFFLINE_COLOR)
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
                    val updated = message.copy(text = newText, isEdited = true)
                    database.child(message.messageId).setValue(updated)
                        .addOnSuccessListener {
                            Toast.makeText(this@socialhomescreenchat, "Message edited", Toast.LENGTH_SHORT).show()
                        }
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun deleteMessage(message: Message) {
        val updated = message.copy(
            isDeleted = true,
            text = "[This message was deleted]",
            deletedAt = System.currentTimeMillis()
        )
        database.child(message.messageId).setValue(updated)
            .addOnSuccessListener { Toast.makeText(this, "Message deleted", Toast.LENGTH_SHORT).show() }
    }

    override fun onDestroy() {
        super.onDestroy()
        screenshotDetector?.stopDetection()
        otherUserStatusListener?.let {
            if (otherUserId.isNotEmpty()) {
                FirebaseDatabase.getInstance().reference.child("users").child(otherUserId)
                    .removeEventListener(it)
            }
        }
        otherUserStatusListener = null
    }
}