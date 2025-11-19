package com.teamsx.i230610_i230040

import android.content.Intent
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Bundle
import android.widget.*
import androidx.activity.OnBackPressedCallback
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProvider
import com.teamsx.i230610_i230040.network.Resource
import com.teamsx.i230610_i230040.utils.UserPreferences
import com.teamsx.i230610_i230040.viewmodels.PostViewModel

class CreatePostScreen : AppCompatActivity() {

    private val userPreferences by lazy { UserPreferences(this) }
    private lateinit var postViewModel: PostViewModel

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
            } catch (_: Exception) {
                // Ignore image loading errors
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

        postViewModel = ViewModelProvider(this)[PostViewModel::class.java]

        initViews()
        setupClickListeners()
        observeViewModel()

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

    private fun observeViewModel() {
        postViewModel.createPostState.observe(this) { resource ->
            when (resource) {
                is Resource.Loading<*> -> {
                    btnPost.isEnabled = false
                }
                is Resource.Success<*> -> {
                    btnPost.isEnabled = true
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
                is Resource.Error<*> -> {
                    btnPost.isEnabled = true
                    Toast.makeText(this, "Failed to create post: ${resource.message}", Toast.LENGTH_SHORT).show()
                }
                null -> {
                    // Initial state, do nothing
                }
            }
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
                } catch (_: Exception) {
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

        val currentUser = userPreferences.getUser()
        if (currentUser == null) {
            Toast.makeText(this, "Please log in first", Toast.LENGTH_SHORT).show()
            return
        }

        val postId = PostUtils.generatePostId()
        val location = etLocation.text.toString().trim().ifEmpty { "" }
        val timestamp = System.currentTimeMillis()

        // Create post via API
        postViewModel.createPost(
            postId = postId,
            userId = currentUser.uid,
            description = description,
            location = location,
            images = selectedImages.toList(),
            timestamp = timestamp
        )
    }
}