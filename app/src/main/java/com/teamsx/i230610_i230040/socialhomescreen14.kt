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
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProvider
import com.google.android.material.imageview.ShapeableImageView
import com.teamsx.i230610_i230040.network.Resource
import com.teamsx.i230610_i230040.utils.UserPreferences
import com.teamsx.i230610_i230040.viewmodels.StoryViewModel

class socialhomescreen14 : AppCompatActivity() {

    private val userPreferences by lazy { UserPreferences(this) }
    private lateinit var storyViewModel: StoryViewModel

    private lateinit var storyImage: ImageView
    private lateinit var userProfileImage: ShapeableImageView
    private lateinit var usernameText: TextView
    private lateinit var timeAgoText: TextView
    private lateinit var closeButton: ImageView
    private lateinit var deleteButton: ImageView
    private lateinit var progressBarsContainer: LinearLayout
    private lateinit var leftTouchArea: View
    private lateinit var rightTouchArea: View

    private var myStories = mutableListOf<Story>()
    private var currentStoryIndex = 0

    private val progressBars = mutableListOf<View>()
    private val handler = Handler(Looper.getMainLooper())
    private var autoProgressRunnable: Runnable? = null

    private val STORY_DURATION = 5000L // 5 seconds per story

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_socialhomescreen14)

        storyViewModel = ViewModelProvider(this)[StoryViewModel::class.java]

        initViews()
        setupClickListeners()
        observeViewModel()
        loadMyStories()

        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                goBackToHome()
            }
        })
    }

    private fun observeViewModel() {
        storyViewModel.userStoriesState.observe(this) { resource ->
            when (resource) {
                is Resource.Success<*> -> {
                    @Suppress("UNCHECKED_CAST")
                    val stories = resource.data as? List<Story> ?: emptyList()
                    myStories.clear()
                    myStories.addAll(stories.filter { it.isValid() })

                    if (myStories.isEmpty()) {
                        Toast.makeText(this, "You have no active stories", Toast.LENGTH_SHORT).show()
                        goBackToHome()
                        return@observe
                    }

                    myStories.sortBy { it.timestamp }
                    loadUserProfile()
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

        storyViewModel.deleteStoryState.observe(this) { resource ->
            when (resource) {
                is Resource.Success<*> -> {
                    Toast.makeText(this, "Story deleted", Toast.LENGTH_SHORT).show()

                    // Remove from local list
                    if (currentStoryIndex < myStories.size) {
                        myStories.removeAt(currentStoryIndex)
                    }

                    if (myStories.isEmpty()) {
                        goBackToHome()
                    } else {
                        if (currentStoryIndex >= myStories.size) {
                            currentStoryIndex = myStories.size - 1
                        }
                        displayStory()
                    }

                    storyViewModel.clearDeleteStoryState()
                }
                is Resource.Error<*> -> {
                    Toast.makeText(this, "Failed to delete story: ${resource.message}", Toast.LENGTH_SHORT).show()
                    startAutoProgress()
                    storyViewModel.clearDeleteStoryState()
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
        deleteButton = findViewById(R.id.deleteButton)
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

        deleteButton.setOnClickListener {
            confirmDeleteStory()
        }
    }

    private fun loadMyStories() {
        val currentUser = userPreferences.getUser()
        if (currentUser == null) {
            Toast.makeText(this, "Please log in", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        storyViewModel.getUserStories(currentUser.uid, currentUser.uid)
    }

    private fun loadUserProfile() {
        val currentUser = userPreferences.getUser() ?: return

        usernameText.text = "Your Story"

        // Load profile image
        val photoBase64 = currentUser.profileImageUrl
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

        displayStory()
    }

    private fun displayStory() {
        if (myStories.isEmpty() || currentStoryIndex >= myStories.size) {
            goBackToHome()
            return
        }

        val currentStory = myStories[currentStoryIndex]

        // Update time ago
        timeAgoText.text = getTimeAgo(currentStory.timestamp)

        // Load story image
        loadStoryImage(currentStory.imageBase64)

        // Setup progress bars
        setupProgressBars(myStories.size)

        // Start auto-progress
        startAutoProgress()
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
        }
    }

    private fun goToNextStory() {
        stopAutoProgress()

        if (currentStoryIndex < myStories.size - 1) {
            currentStoryIndex++
            displayStory()
        } else {
            // All stories finished
            goBackToHome()
        }
    }

    private fun confirmDeleteStory() {
        stopAutoProgress()

        AlertDialog.Builder(this)
            .setTitle("Delete Story")
            .setMessage("Are you sure you want to delete this story?")
            .setPositiveButton("Delete") { _, _ ->
                deleteCurrentStory()
            }
            .setNegativeButton("Cancel") { dialog, _ ->
                dialog.dismiss()
                startAutoProgress()
            }
            .setOnCancelListener {
                startAutoProgress()
            }
            .show()
    }

    private fun deleteCurrentStory() {
        val currentUser = userPreferences.getUser() ?: return
        if (currentStoryIndex >= myStories.size) return

        val currentStory = myStories[currentStoryIndex]
        storyViewModel.deleteStory(currentStory.storyId, currentUser.uid)
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
        if (myStories.isNotEmpty()) {
            startAutoProgress()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        stopAutoProgress()
    }
}