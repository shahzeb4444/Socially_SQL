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
import com.google.android.material.imageview.ShapeableImageView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*

class socialhomescreen12 : AppCompatActivity() {

    private val auth by lazy { FirebaseAuth.getInstance() }
    private val db by lazy { FirebaseDatabase.getInstance().reference }

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

        initViews()
        setupClickListeners()
        loadAllStories()

        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                goBackToHome()
            }
        })
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
        val currentUserId = auth.currentUser?.uid ?: return
        val startUserId = intent.getStringExtra("user_id") ?: return

        // Get following list
        db.child("follows").child(currentUserId).child("following")
            .addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(followingSnapshot: DataSnapshot) {
                    val followingIds = followingSnapshot.children.mapNotNull { it.key }.toMutableList()

                    // Reorder to start with the clicked user
                    followingIds.remove(startUserId)
                    followingIds.add(0, startUserId)

                    loadStoriesForUsers(followingIds, currentUserId)
                }

                override fun onCancelled(error: DatabaseError) {
                    Toast.makeText(this@socialhomescreen12, "Failed to load stories", Toast.LENGTH_SHORT).show()
                    finish()
                }
            })
    }

    private fun loadStoriesForUsers(userIds: List<String>, currentUserId: String) {
        allStoryGroups.clear()
        var processedCount = 0

        for (userId in userIds) {
            db.child("stories").child(userId).addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    val userStories = mutableListOf<Story>()

                    for (storySnap in snapshot.children) {
                        val story = storySnap.getValue(Story::class.java)
                        if (story != null && story.isValid()) {
                            userStories.add(story)
                        }
                    }

                    userStories.sortBy { it.timestamp }

                    if (userStories.isNotEmpty()) {
                        db.child("users").child(userId).get()
                            .addOnSuccessListener { userSnapshot ->
                                val userProfile = userSnapshot.getValue(UserProfile::class.java)

                                val storyGroup = StoryGroup(
                                    userId = userId,
                                    username = userProfile?.username ?: "User",
                                    userPhotoBase64 = userProfile?.photoBase64,
                                    stories = userStories,
                                    hasUnviewedStories = userStories.any { !it.isViewedBy(currentUserId) }
                                )
                                allStoryGroups.add(storyGroup)

                                processedCount++
                                if (processedCount == userIds.size) {
                                    displayStory()
                                }
                            }
                    } else {
                        processedCount++
                        if (processedCount == userIds.size) {
                            if (allStoryGroups.isEmpty()) {
                                Toast.makeText(this@socialhomescreen12, "No stories available", Toast.LENGTH_SHORT).show()
                                finish()
                            } else {
                                displayStory()
                            }
                        }
                    }
                }

                override fun onCancelled(error: DatabaseError) {
                    processedCount++
                    if (processedCount == userIds.size) {
                        if (allStoryGroups.isEmpty()) {
                            finish()
                        } else {
                            displayStory()
                        }
                    }
                }
            })
        }
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
        val currentUserId = auth.currentUser?.uid ?: return

        db.child("stories")
            .child(story.userId)
            .child(story.storyId)
            .child("viewedBy")
            .child(currentUserId)
            .setValue(true)
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