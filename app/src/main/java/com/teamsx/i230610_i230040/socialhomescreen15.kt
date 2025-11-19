package com.teamsx.i230610_i230040

import android.content.Intent
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.activity.OnBackPressedCallback
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProvider
import com.teamsx.i230610_i230040.network.Resource
import com.teamsx.i230610_i230040.utils.UserPreferences
import com.teamsx.i230610_i230040.viewmodels.StoryViewModel

class socialhomescreen15 : AppCompatActivity() {

    private val userPreferences by lazy { UserPreferences(this) }
    private lateinit var storyViewModel: StoryViewModel

    private lateinit var imageContainer: FrameLayout
    private lateinit var selectedImage: ImageView
    private lateinit var placeholderText: TextView
    private lateinit var cancelIcon: ImageView
    private lateinit var yourStoryBtn: ImageView
    private lateinit var closeFriendsBtn: ImageView

    private var selectedImageBase64: String? = null

    private val pickImage = registerForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        uri ?: return@registerForActivityResult

        try {
            contentResolver.openInputStream(uri)?.use { input ->
                val bitmap = BitmapFactory.decodeStream(input)
                val resized = ImageUtils.resizeBitmap(bitmap, 1080)
                selectedImageBase64 = ImageUtils.bitmapToBase64(resized, 70)

                selectedImage.setImageBitmap(resized)
                selectedImage.visibility = View.VISIBLE
                placeholderText.visibility = View.GONE
            }
        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(this, "Failed to load image", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_socialhomescreen15)

        storyViewModel = ViewModelProvider(this)[StoryViewModel::class.java]

        // Handle back press
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                finish()
            }
        })

        initViews()
        setupClickListeners()
        observeViewModel()
    }

    private fun initViews() {
        imageContainer = findViewById(R.id.imageContainer)
        selectedImage = findViewById(R.id.selectedImage)
        placeholderText = findViewById(R.id.placeholderText)
        cancelIcon = findViewById(R.id.redcross)
        yourStoryBtn = findViewById(R.id.yourstory)
        closeFriendsBtn = findViewById(R.id.closefriends)
    }

    private fun setupClickListeners() {
        imageContainer.setOnClickListener {
            pickImage.launch("image/*")
        }

        cancelIcon.setOnClickListener {
            finish()
        }

        yourStoryBtn.setOnClickListener {
            if (selectedImageBase64 != null) {
                postStory(isCloseFriendsOnly = false)
            } else {
                Toast.makeText(this, "Please select a photo first", Toast.LENGTH_SHORT).show()
            }
        }

        closeFriendsBtn.setOnClickListener {
            if (selectedImageBase64 != null) {
                postStory(isCloseFriendsOnly = true)
            } else {
                Toast.makeText(this, "Please select a photo first", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun observeViewModel() {

        storyViewModel.createStoryState.observe(this) { resource ->
            when (resource) {
                is Resource.Loading<*> -> {
                    yourStoryBtn.isEnabled = false
                    closeFriendsBtn.isEnabled = false
                }
                is Resource.Success<*> -> {
                    yourStoryBtn.isEnabled = true
                    closeFriendsBtn.isEnabled = true
                    Toast.makeText(this, "Story posted!", Toast.LENGTH_SHORT).show()

                    startActivity(
                        Intent(this, HomeActivity::class.java).apply {
                            putExtra(HomeActivity.EXTRA_START_DEST, R.id.nav_home)
                            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
                        }
                    )
                    finish()
                }
                is Resource.Error<*> -> {
                    yourStoryBtn.isEnabled = true
                    closeFriendsBtn.isEnabled = true
                    Toast.makeText(this, "Failed to post story: ${resource.message}", Toast.LENGTH_SHORT).show()
                }
                null -> {
                    // Initial state, do nothing
                }
            }
        }
    }

    private fun postStory(isCloseFriendsOnly: Boolean) {
        val currentUser = userPreferences.getUser()
        if (currentUser == null) {
            Toast.makeText(this, "Please log in first", Toast.LENGTH_SHORT).show()
            return
        }

        val imageBase64 = selectedImageBase64
        if (imageBase64 == null) {
            Toast.makeText(this, "Please select a photo first", Toast.LENGTH_SHORT).show()
            return
        }

        val storyId = StoryUtils.generateStoryId()
        val timestamp = System.currentTimeMillis()
        val expiresAt = StoryUtils.getExpiryTime()

        // Create story via API
        storyViewModel.createStory(
            storyId = storyId,
            userId = currentUser.uid,
            imageBase64 = imageBase64,
            timestamp = timestamp,
            expiresAt = expiresAt,
            isCloseFriendsOnly = isCloseFriendsOnly
        )
    }
}