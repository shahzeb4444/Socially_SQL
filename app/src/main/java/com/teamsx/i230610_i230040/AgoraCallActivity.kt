package com.teamsx.i230610_i230040

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.view.SurfaceView
import android.view.View
import android.widget.FrameLayout
import android.widget.ImageButton
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import io.agora.rtc2.ChannelMediaOptions
import io.agora.rtc2.Constants
import io.agora.rtc2.IRtcEngineEventHandler
import io.agora.rtc2.RtcEngine
import io.agora.rtc2.RtcEngineConfig
import io.agora.rtc2.video.VideoCanvas

class AgoraCallActivity : AppCompatActivity() {

    private enum class CallMode { VOICE, VIDEO }

    private val eventHandler = object : IRtcEngineEventHandler() {
        override fun onJoinChannelSuccess(channel: String?, uid: Int, elapsed: Int) {
            runOnUiThread {
                callStatusText.text = getString(R.string.call_connected, channel.orEmpty())
            }
        }

        override fun onUserJoined(uid: Int, elapsed: Int) {
            runOnUiThread {
                callStatusText.text = getString(R.string.remote_user_joined)
                setupRemoteVideo(uid)
            }
        }

        override fun onUserOffline(uid: Int, reason: Int) {
            runOnUiThread {
                callStatusText.text = getString(R.string.remote_user_left)
                removeRemoteVideo()
            }
        }

        override fun onError(err: Int) {
            runOnUiThread {
                Toast.makeText(this@AgoraCallActivity, getString(R.string.call_error, err), Toast.LENGTH_LONG).show()
                finish()
            }
        }
    }

    private val permissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val allGranted = permissions.entries.all { it.value }
        if (allGranted) {
            initializeAndJoinChannel()
        } else {
            Toast.makeText(this, R.string.call_permissions_denied, Toast.LENGTH_LONG).show()
            finish()
        }
    }

    private lateinit var callMode: CallMode
    private lateinit var channelName: String
    private lateinit var callStatusText: TextView
    private lateinit var remoteVideoContainer: FrameLayout
    private lateinit var localVideoContainer: FrameLayout
    private lateinit var muteButton: ImageButton
    private lateinit var videoButton: ImageButton
    private lateinit var switchCameraButton: ImageButton
    private lateinit var endCallButton: ImageButton

    private var rtcEngine: RtcEngine? = null
    private var localSurfaceView: SurfaceView? = null
    private var remoteSurfaceView: SurfaceView? = null
    private var isAudioMuted = false
    private var isVideoEnabled = true

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_agora_call)

        callStatusText = findViewById(R.id.callStatusText)
        remoteVideoContainer = findViewById(R.id.remoteVideoContainer)
        localVideoContainer = findViewById(R.id.localVideoContainer)
        muteButton = findViewById(R.id.muteAudioButton)
        videoButton = findViewById(R.id.toggleVideoButton)
        switchCameraButton = findViewById(R.id.switchCameraButton)
        endCallButton = findViewById(R.id.endCallButton)
        val titleText: TextView = findViewById(R.id.callTitleText)

        val incomingChannel = intent.getStringExtra(EXTRA_CHANNEL_NAME)
        val incomingMode = intent.getStringExtra(EXTRA_CALL_MODE)
        val otherUserName = intent.getStringExtra(EXTRA_REMOTE_NAME).orEmpty()

        if (incomingChannel.isNullOrBlank() || incomingMode.isNullOrBlank()) {
            Toast.makeText(this, R.string.call_missing_parameters, Toast.LENGTH_LONG).show()
            finish()
            return
        }

        channelName = incomingChannel
        callMode = if (incomingMode == MODE_VOICE) CallMode.VOICE else CallMode.VIDEO
        isVideoEnabled = callMode == CallMode.VIDEO

        titleText.text = if (otherUserName.isNotEmpty()) {
            getString(
                if (callMode == CallMode.VIDEO) R.string.video_call_with else R.string.voice_call_with,
                otherUserName
            )
        } else {
            getString(if (callMode == CallMode.VIDEO) R.string.video_call else R.string.voice_call)
        }

        setupControls()
        updateVideoControlsVisibility()

        if (hasAllPermissions()) {
            initializeAndJoinChannel()
        } else {
            requestPermissionsForCall()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        leaveChannel()
        RtcEngine.destroy()
        rtcEngine = null
    }

    private fun hasAllPermissions(): Boolean {
        val permissions = mutableListOf(Manifest.permission.RECORD_AUDIO)
        if (callMode == CallMode.VIDEO) {
            permissions += Manifest.permission.CAMERA
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            permissions += Manifest.permission.BLUETOOTH_CONNECT
        }
        return permissions.all { ContextCompat.checkSelfPermission(this, it) == PackageManager.PERMISSION_GRANTED }
    }

    private fun requestPermissionsForCall() {
        val permissions = mutableListOf(Manifest.permission.RECORD_AUDIO)
        if (callMode == CallMode.VIDEO) {
            permissions += Manifest.permission.CAMERA
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            permissions += Manifest.permission.BLUETOOTH_CONNECT
        }
        permissionLauncher.launch(permissions.toTypedArray())
    }

    private fun initializeAndJoinChannel() {
        if (rtcEngine != null) return

        val config = RtcEngineConfig().apply {
            mContext = applicationContext
            mAppId = APP_ID
            mEventHandler = eventHandler
        }

        rtcEngine = try {
            RtcEngine.create(config)
        } catch (e: Exception) {
            Toast.makeText(this, getString(R.string.call_initialization_failed, e.localizedMessage), Toast.LENGTH_LONG).show()
            finish()
            return
        }

        rtcEngine?.setChannelProfile(Constants.CHANNEL_PROFILE_COMMUNICATION)
        rtcEngine?.enableAudio()

        if (callMode == CallMode.VIDEO) {
            rtcEngine?.enableVideo()
            setupLocalVideo()
        } else {
            rtcEngine?.disableVideo()
        }

        val options = ChannelMediaOptions().apply {
            channelProfile = Constants.CHANNEL_PROFILE_COMMUNICATION
            clientRoleType = Constants.CLIENT_ROLE_BROADCASTER
            autoSubscribeAudio = true
            autoSubscribeVideo = callMode == CallMode.VIDEO
        }

        val result = rtcEngine?.joinChannel(null, channelName, 0, options) ?: -1
        if (result < 0) {
            Toast.makeText(this, getString(R.string.call_join_failed, result), Toast.LENGTH_LONG).show()
            finish()
        } else {
            callStatusText.text = getString(R.string.joining_channel, channelName)
        }
    }

    private fun setupControls() {
        muteButton.setOnClickListener {
            isAudioMuted = !isAudioMuted
            rtcEngine?.muteLocalAudioStream(isAudioMuted)
            muteButton.setImageResource(if (isAudioMuted) R.drawable.ic_mic_off else R.drawable.ic_mic_on)
        }

        videoButton.setOnClickListener {
            if (callMode == CallMode.VOICE) return@setOnClickListener
            isVideoEnabled = !isVideoEnabled
            rtcEngine?.muteLocalVideoStream(!isVideoEnabled)
            if (isVideoEnabled) {
                setupLocalVideo()
            } else {
                localVideoContainer.removeAllViews()
            }
            localVideoContainer.isVisible = isVideoEnabled
            videoButton.setImageResource(if (isVideoEnabled) R.drawable.ic_video_on else R.drawable.ic_video_off)
        }

        switchCameraButton.setOnClickListener {
            rtcEngine?.switchCamera()
        }

        endCallButton.setOnClickListener {
            finish()
        }
    }

    private fun updateVideoControlsVisibility() {
        val videoControlsVisible = callMode == CallMode.VIDEO
        remoteVideoContainer.isVisible = videoControlsVisible
        localVideoContainer.isVisible = videoControlsVisible
        videoButton.visibility = if (videoControlsVisible) View.VISIBLE else View.GONE
        switchCameraButton.visibility = if (videoControlsVisible) View.VISIBLE else View.GONE
    }

    private fun setupLocalVideo() {
        if (localVideoContainer.childCount == 0) {
            localSurfaceView = SurfaceView(this)
            localSurfaceView?.setZOrderMediaOverlay(true)
            localVideoContainer.addView(localSurfaceView)
        }
        localVideoContainer.isVisible = true
        rtcEngine?.setupLocalVideo(VideoCanvas(localSurfaceView, VideoCanvas.RENDER_MODE_HIDDEN, 0))
    }

    private fun setupRemoteVideo(uid: Int) {
        if (callMode != CallMode.VIDEO) return
        if (remoteVideoContainer.childCount == 0) {
            remoteSurfaceView = SurfaceView(this)
            remoteVideoContainer.addView(remoteSurfaceView)
        }
        remoteVideoContainer.isVisible = true
        rtcEngine?.setupRemoteVideo(VideoCanvas(remoteSurfaceView, VideoCanvas.RENDER_MODE_FIT, uid))
    }

    private fun removeRemoteVideo() {
        remoteVideoContainer.removeAllViews()
        remoteSurfaceView = null
        remoteVideoContainer.isVisible = false
    }

    private fun leaveChannel() {
        rtcEngine?.leaveChannel()
        localVideoContainer.removeAllViews()
        remoteVideoContainer.removeAllViews()
        localVideoContainer.isVisible = false
        remoteVideoContainer.isVisible = false
        localSurfaceView = null
        remoteSurfaceView = null
    }

    companion object {
        private const val APP_ID = "1b54fe23d3294a2f8df7674f060a0da3"
        private const val EXTRA_CHANNEL_NAME = "extra_channel_name"
        private const val EXTRA_CALL_MODE = "extra_call_mode"
        private const val EXTRA_REMOTE_NAME = "extra_remote_name"
        private const val MODE_VOICE = "voice"
        private const val MODE_VIDEO = "video"

        fun voiceCallIntent(context: Context, channelName: String, remoteName: String?): Intent {
            return Intent(context, AgoraCallActivity::class.java).apply {
                putExtra(EXTRA_CHANNEL_NAME, channelName)
                putExtra(EXTRA_CALL_MODE, MODE_VOICE)
                putExtra(EXTRA_REMOTE_NAME, remoteName)
            }
        }

        fun videoCallIntent(context: Context, channelName: String, remoteName: String?): Intent {
            return Intent(context, AgoraCallActivity::class.java).apply {
                putExtra(EXTRA_CHANNEL_NAME, channelName)
                putExtra(EXTRA_CALL_MODE, MODE_VIDEO)
                putExtra(EXTRA_REMOTE_NAME, remoteName)
            }
        }
    }
}