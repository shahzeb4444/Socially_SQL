package com.teamsx.i230610_i230040

import android.content.Intent
import android.graphics.Bitmap
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
import com.google.firebase.database.FirebaseDatabase
import com.teamsx.i230610_i230040.utils.UserPreferences

class socialhomescreen15 : AppCompatActivity() {

    private val db by lazy { FirebaseDatabase.getInstance().reference }
    private val userPreferences by lazy { UserPreferences(this) }

    private lateinit var imageContainer: FrameLayout
    private lateinit var selectedImage: ImageView
    private lateinit var placeholderText: TextView
    private lateinit var cancelIcon: ImageView
    private lateinit var yourStoryBtn: ImageView
    private lateinit var closeFriendsBtn: ImageView

    private var selectedImageBase64: String? = null
    private var isPosting = false

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

        // Handle back press
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                finish()
            }
        })

        initViews()
        setupClickListeners()
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
            if (selectedImageBase64 != null && !isPosting) {
                postStory(isCloseFriendsOnly = false)
            } else if (selectedImageBase64 == null) {
                Toast.makeText(this, "Please select a photo first", Toast.LENGTH_SHORT).show()
            }
        }

        closeFriendsBtn.setOnClickListener {
            if (selectedImageBase64 != null && !isPosting) {
                postStory(isCloseFriendsOnly = true)
            } else if (selectedImageBase64 == null) {
                Toast.makeText(this, "Please select a photo first", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun postStory(isCloseFriendsOnly: Boolean) {
        val currentUser = userPreferences.getUser()
        if (currentUser == null) {
            Toast.makeText(this, "Please log in first", Toast.LENGTH_SHORT).show()
            return
        }

        val uid = currentUser.uid
        isPosting = true

        // Get user profile from Firebase
        db.child("users").child(uid).get()
            .addOnSuccessListener { snapshot ->
                val userProfile = snapshot.getValue(UserProfile::class.java)

                if (userProfile == null) {
                    Toast.makeText(this, "User profile not found", Toast.LENGTH_SHORT).show()
                    isPosting = false
                    return@addOnSuccessListener
                }

                val storyId = StoryUtils.generateStoryId()
                val story = Story(
                    storyId = storyId,
                    userId = uid,
                    username = userProfile.username,
                    userPhotoBase64 = userProfile.photoBase64 ?: "",
                    imageBase64 = selectedImageBase64 ?: "",
                    timestamp = System.currentTimeMillis(),
                    expiresAt = StoryUtils.getExpiryTime(),
                    viewedBy = emptyMap(),
                    isCloseFreindsOnly = isCloseFriendsOnly
                )

                // Post story to Firebase
                db.child("stories").child(uid).child(storyId).setValue(story)
                    .addOnSuccessListener {
                        Toast.makeText(
                            this,
                            if (isCloseFriendsOnly) "Posted to Close Friends!" else "Story posted!",
                            Toast.LENGTH_SHORT
                        ).show()

                        startActivity(
                            Intent(this, HomeActivity::class.java).apply {
                                putExtra(HomeActivity.EXTRA_START_DEST, R.id.nav_home)
                                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
                            }
                        )
                        finish()
                    }
                    .addOnFailureListener { error ->
                        Toast.makeText(this, "Failed to post story: ${error.message}", Toast.LENGTH_SHORT).show()
                        isPosting = false
                    }
            }
            .addOnFailureListener { error ->
                Toast.makeText(this, "Failed to load user profile: ${error.message}", Toast.LENGTH_SHORT).show()
                isPosting = false
            }
    }
}