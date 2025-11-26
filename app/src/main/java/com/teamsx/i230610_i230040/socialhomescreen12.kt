package com.teamsx.i230610_i230040

import android.content.Intent
import android.graphics.BitmapFactory
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Base64
import android.view.View
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProvider
import com.google.android.material.imageview.ShapeableImageView
import com.teamsx.i230610_i230040.network.Resource
import com.teamsx.i230610_i230040.utils.UserPreferences
import com.teamsx.i230610_i230040.viewmodels.StoryViewModel

class socialhomescreen12 : AppCompatActivity() {

    private val userPreferences by lazy { UserPreferences(this) }
    private lateinit var storyViewModel: StoryViewModel

    private lateinit var storyImage: ImageView
    private lateinit var userProfileImage: ShapeableImageView
    private lateinit var usernameText: TextView
    private lateinit var timeAgoText: TextView
    private lateinit var closeButton: ImageView
    private lateinit var progressBarsContainer: LinearLayout
    private lateinit var leftTouchArea: View
    private lateinit var rightTouchArea: View

    private var allStoryGroups = mutableListOf<StoryGroup>()
    private var currentGroupIndex = 0
    private var currentStoryIndex = 0

    private val progressBars = mutableListOf<View>()
    private val handler = Handler(Looper.getMainLooper())
    private var autoProgressRunnable: Runnable? = null

    private val STORY_DURATION = 5000L // 5 seconds per story

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_socialhomescreen12)

        storyViewModel = ViewModelProvider(this)[StoryViewModel::class.java]

        initViews()
        setupClickListeners()
        observeViewModel()
        loadAllStories()

        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                goBackToHome()
            }
        })
    }

    private fun observeViewModel() {
        storyViewModel.feedStoriesState.observe(this) { resource ->
            when (resource) {
                is Resource.Success<*> -> {
                    @Suppress("UNCHECKED_CAST")
                    val storyGroups = resource.data as? List<StoryGroup> ?: emptyList()

                    allStoryGroups.clear()
                    allStoryGroups.addAll(storyGroups.filter { it.stories.isNotEmpty() })

                    if (allStoryGroups.isEmpty()) {
                        Toast.makeText(this, "No stories available", Toast.LENGTH_SHORT).show()
                        finish()
                        return@observe
                    }

                    // Reorder to start with the clicked user
                    val startUserId = intent.getStringExtra("user_id")
                    if (startUserId != null) {
                        val startIndex = allStoryGroups.indexOfFirst { it.userId == startUserId }
                        if (startIndex > 0) {
                            val startGroup = allStoryGroups.removeAt(startIndex)
                            allStoryGroups.add(0, startGroup)
                        }
                    }

                    displayStory()
                }
                is Resource.Error<*> -> {
                    Toast.makeText(this, "Failed to load stories: ${resource.message}", Toast.LENGTH_SHORT).show()
                    finish()
                }
                is Resource.Loading<*> -> {
                    // Show loading if needed
                }
                null -> {}
            }
        }
    }

    private fun initViews() {
        storyImage = findViewById(R.id.storyImage)
        userProfileImage = findViewById(R.id.userProfileImage)
        usernameText = findViewById(R.id.username)
        timeAgoText = findViewById(R.id.timeAgo)
        closeButton = findViewById(R.id.closeButton)
        progressBarsContainer = findViewById(R.id.progressBarsContainer)
        leftTouchArea = findViewById(R.id.leftTouchArea)
        rightTouchArea = findViewById(R.id.rightTouchArea)
    }

    private fun setupClickListeners() {
        closeButton.setOnClickListener {
            goBackToHome()
        }

        leftTouchArea.setOnClickListener {
            goToPreviousStory()
        }

        rightTouchArea.setOnClickListener {
            goToNextStory()
        }
    }

    private fun loadAllStories() {
        val currentUser = userPreferences.getUser()
        if (currentUser == null) {
            Toast.makeText(this, "Please log in", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        storyViewModel.getFeedStories(currentUser.uid)
    }

    private fun displayStory() {
        if (allStoryGroups.isEmpty() || currentGroupIndex >= allStoryGroups.size) {
            goBackToHome()
            return
        }

        val currentGroup = allStoryGroups[currentGroupIndex]
        if (currentStoryIndex >= currentGroup.stories.size) {
            // Move to next user's stories
            currentGroupIndex++
            currentStoryIndex = 0
            displayStory()
            return
        }

        val currentStory = currentGroup.stories[currentStoryIndex]

        // Update UI
        usernameText.text = currentGroup.username
        timeAgoText.text = getTimeAgo(currentStory.timestamp)

        // Load profile image
        loadProfileImage(currentGroup.userPhotoBase64)

        // Load story image
        loadStoryImage(currentStory.imageBase64)

        // Setup progress bars
        setupProgressBars(currentGroup.stories.size)

        // Mark as viewed
        markStoryAsViewed(currentStory)

        // Start auto-progress
        startAutoProgress()
    }

    private fun loadProfileImage(photoBase64: String?) {
        if (!photoBase64.isNullOrEmpty()) {
            try {
                val bytes = Base64.decode(photoBase64, Base64.DEFAULT)
                val bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
                userProfileImage.setImageBitmap(bitmap)
            } catch (e: Exception) {
                userProfileImage.setImageResource(R.drawable.profile_login_splash_small)
            }
        } else {
            userProfileImage.setImageResource(R.drawable.profile_login_splash_small)
        }
    }

    private fun loadStoryImage(imageBase64: String) {
        try {
            val bytes = Base64.decode(imageBase64, Base64.DEFAULT)
            val bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
            storyImage.setImageBitmap(bitmap)
        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(this, "Failed to load story", Toast.LENGTH_SHORT).show()
        }
    }

    private fun setupProgressBars(count: Int) {
        progressBarsContainer.removeAllViews()
        progressBars.clear()

        for (i in 0 until count) {
            val progressBar = View(this).apply {
                layoutParams = LinearLayout.LayoutParams(
                    0,
                    4,
                    1f
                ).apply {
                    setMargins(2, 0, 2, 0)
                }
                setBackgroundColor(
                    if (i < currentStoryIndex) 0xFFFFFFFF.toInt()
                    else if (i == currentStoryIndex) 0xFFFFFFFF.toInt()
                    else 0x55FFFFFF.toInt()
                )
            }
            progressBarsContainer.addView(progressBar)
            progressBars.add(progressBar)
        }
    }

    private fun updateProgressBars() {
        progressBars.forEachIndexed { index, view ->
            view.setBackgroundColor(
                when {
                    index < currentStoryIndex -> 0xFFFFFFFF.toInt()
                    index == currentStoryIndex -> 0xFFFFFFFF.toInt()
                    else -> 0x55FFFFFF.toInt()
                }
            )
        }
    }

    private fun startAutoProgress() {
        stopAutoProgress()
        autoProgressRunnable = Runnable {
            goToNextStory()
        }
        handler.postDelayed(autoProgressRunnable!!, STORY_DURATION)
    }

    private fun stopAutoProgress() {
        autoProgressRunnable?.let { handler.removeCallbacks(it) }
    }

    private fun goToPreviousStory() {
        stopAutoProgress()

        if (currentStoryIndex > 0) {
            currentStoryIndex--
            displayStory()
        } else if (currentGroupIndex > 0) {
            currentGroupIndex--
            currentStoryIndex = allStoryGroups[currentGroupIndex].stories.size - 1
            displayStory()
        }
    }

    private fun goToNextStory() {
        stopAutoProgress()

        val currentGroup = allStoryGroups[currentGroupIndex]
        if (currentStoryIndex < currentGroup.stories.size - 1) {
            currentStoryIndex++
            displayStory()
        } else {
            // Move to next user
            currentGroupIndex++
            currentStoryIndex = 0

            if (currentGroupIndex >= allStoryGroups.size) {
                goBackToHome()
            } else {
                displayStory()
            }
        }
    }

    private fun markStoryAsViewed(story: Story) {
        val currentUser = userPreferences.getUser() ?: return
        storyViewModel.markStoryViewed(story.storyId, currentUser.uid)
    }

    private fun getTimeAgo(timestamp: Long): String {
        val diff = System.currentTimeMillis() - timestamp
        val hours = diff / (1000 * 60 * 60)
        return when {
            hours < 1 -> "Just now"
            hours < 24 -> "${hours}h"
            else -> "1d"
        }
    }

    private fun goBackToHome() {
        startActivity(
            Intent(this, HomeActivity::class.java).apply {
                putExtra(HomeActivity.EXTRA_START_DEST, R.id.nav_home)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
            }
        )
        finish()
    }

    override fun onPause() {
        super.onPause()
        stopAutoProgress()
    }

    override fun onResume() {
        super.onResume()
        if (allStoryGroups.isNotEmpty()) {
            startAutoProgress()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        stopAutoProgress()
    }
}