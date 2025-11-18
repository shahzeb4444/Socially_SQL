package com.teamsx.i230610_i230040

import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.widget.*
import androidx.activity.OnBackPressedCallback
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase

class CreatePostScreen : AppCompatActivity() {

    private val auth by lazy { FirebaseAuth.getInstance() }
    private val db by lazy { FirebaseDatabase.getInstance().reference }

    private lateinit var btnBack: ImageView
    private lateinit var btnPost: TextView
    private lateinit var btnSelectPhotos: Button
    private lateinit var etLocation: EditText
    private lateinit var etDescription: EditText
    private lateinit var selectedImagesContainer: LinearLayout

    private val selectedImages = mutableListOf<String>() // Base64 strings
    private val MAX_IMAGES = 3

    private val pickMultipleImages = registerForActivityResult(
        ActivityResultContracts.GetMultipleContents()
    ) { uris: List<Uri> ->
        if (uris.isEmpty()) return@registerForActivityResult

        val availableSlots = MAX_IMAGES - selectedImages.size
        val urisToProcess = uris.take(availableSlots)

        urisToProcess.forEach { uri ->
            try {
                contentResolver.openInputStream(uri)?.use { input ->
                    val bitmap = BitmapFactory.decodeStream(input)
                    val resized = ImageUtils.resizeBitmap(bitmap, 1080)
                    val base64 = ImageUtils.bitmapToBase64(resized, 70)
                    selectedImages.add(base64)
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        updateSelectedImagesDisplay()
        updateSelectButton()

        if (selectedImages.size >= MAX_IMAGES) {
            Toast.makeText(this, "Maximum 3 photos selected", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_create_post_screen)

        initViews()
        setupClickListeners()

        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                finish()
            }
        })
    }

    private fun initViews() {
        btnBack = findViewById(R.id.btnBack)
        btnPost = findViewById(R.id.btnPost)
        btnSelectPhotos = findViewById(R.id.btnSelectPhotos)
        etLocation = findViewById(R.id.etLocation)
        etDescription = findViewById(R.id.etDescription)
        selectedImagesContainer = findViewById(R.id.selectedImagesContainer)
    }

    private fun setupClickListeners() {
        btnBack.setOnClickListener {
            finish()
        }

        btnSelectPhotos.setOnClickListener {
            if (selectedImages.size < MAX_IMAGES) {
                pickMultipleImages.launch("image/*")
            } else {
                Toast.makeText(this, "Maximum 3 photos allowed", Toast.LENGTH_SHORT).show()
            }
        }

        btnPost.setOnClickListener {
            createPost()
        }
    }

    private fun updateSelectedImagesDisplay() {
        selectedImagesContainer.removeAllViews()

        selectedImages.forEachIndexed { index, base64 ->
            val frameLayout = FrameLayout(this).apply {
                layoutParams = LinearLayout.LayoutParams(200, 200).apply {
                    marginEnd = 16
                }
            }

            val imageView = ImageView(this).apply {
                layoutParams = FrameLayout.LayoutParams(
                    FrameLayout.LayoutParams.MATCH_PARENT,
                    FrameLayout.LayoutParams.MATCH_PARENT
                )
                scaleType = ImageView.ScaleType.CENTER_CROP
                try {
                    val bitmap = ImageUtils.base64ToBitmap(base64)
                    setImageBitmap(bitmap)
                } catch (e: Exception) {
                    setImageResource(R.drawable.profile_login_splash_small)
                }
            }

            val removeButton = ImageView(this).apply {
                layoutParams = FrameLayout.LayoutParams(40, 40).apply {
                    gravity = android.view.Gravity.TOP or android.view.Gravity.END
                    setMargins(0, 8, 8, 0)
                }
                setImageResource(R.drawable.whitecancelicon)
                setBackgroundResource(R.drawable.greycircle2)
                scaleType = ImageView.ScaleType.CENTER
                setOnClickListener {
                    selectedImages.removeAt(index)
                    updateSelectedImagesDisplay()
                    updateSelectButton()
                }
            }

            frameLayout.addView(imageView)
            frameLayout.addView(removeButton)
            selectedImagesContainer.addView(frameLayout)
        }
    }

    private fun updateSelectButton() {
        btnSelectPhotos.text = "Select Photos (${selectedImages.size}/$MAX_IMAGES)"
    }

    private fun createPost() {
        if (selectedImages.isEmpty()) {
            Toast.makeText(this, "Please select at least one photo", Toast.LENGTH_SHORT).show()
            return
        }

        val description = etDescription.text.toString().trim()
        if (description.isEmpty()) {
            Toast.makeText(this, "Please write a description", Toast.LENGTH_SHORT).show()
            return
        }

        val currentUser = auth.currentUser
        if (currentUser == null) {
            Toast.makeText(this, "Please log in first", Toast.LENGTH_SHORT).show()
            return
        }

        btnPost.isEnabled = false
        Toast.makeText(this, "Creating post...", Toast.LENGTH_SHORT).show()

        val uid = currentUser.uid

        // Get user info
        db.child("users").child(uid).get()
            .addOnSuccessListener { snapshot ->
                val userProfile = snapshot.getValue(UserProfile::class.java)

                if (userProfile == null) {
                    Toast.makeText(this, "User profile not found", Toast.LENGTH_SHORT).show()
                    btnPost.isEnabled = true
                    return@addOnSuccessListener
                }

                val postId = PostUtils.generatePostId()
                val location = etLocation.text.toString().trim().ifEmpty { "" }

                val post = Post(
                    postId = postId,
                    userId = uid,
                    username = userProfile.username,
                    userPhotoBase64 = userProfile.photoBase64,
                    location = location,
                    description = description,
                    images = selectedImages.toList(),
                    timestamp = System.currentTimeMillis(),
                    likes = emptyMap(),
                    commentsCount = 0
                )

                // Save post to Firebase: /posts/{postId}
                db.child("posts").child(postId).setValue(post)
                    .addOnSuccessListener {
                        // Update user's posts count
                        db.child("users").child(uid).child("postsCount")
                            .setValue(userProfile.postsCount + 1)

                        // Add post reference to user's posts list
                        db.child("userPosts").child(uid).child(postId).setValue(true)

                        Toast.makeText(this, "Post created successfully!", Toast.LENGTH_SHORT).show()

                        // Navigate back to HomeFragment
                        startActivity(
                            Intent(this, HomeActivity::class.java).apply {
                                putExtra(HomeActivity.EXTRA_START_DEST, R.id.nav_home)
                                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
                            }
                        )
                        finish()
                    }
                    .addOnFailureListener { e ->
                        Toast.makeText(this, "Failed to create post: ${e.message}", Toast.LENGTH_SHORT).show()
                        btnPost.isEnabled = true
                    }
            }
            .addOnFailureListener { e ->
                Toast.makeText(this, "Failed to load user profile: ${e.message}", Toast.LENGTH_SHORT).show()
                btnPost.isEnabled = true
            }
    }
}