package com.teamsx.i230610_i230040

import android.content.Intent
import android.graphics.BitmapFactory
import android.os.Bundle
import android.util.Base64
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.viewpager2.widget.ViewPager2
import com.google.android.material.imageview.ShapeableImageView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*

class PostViewActivity : AppCompatActivity() {

    private val auth by lazy { FirebaseAuth.getInstance() }
    private val db by lazy { FirebaseDatabase.getInstance().reference }

    private lateinit var btnBack: ImageView
    private lateinit var userProfileImage: ShapeableImageView
    private lateinit var username: TextView
    private lateinit var location: TextView
    private lateinit var imagesViewPager: ViewPager2
    private lateinit var imageIndicator: TextView
    private lateinit var btnLike: ImageView
    private lateinit var btnComment: ImageView
    private lateinit var btnSend: ImageView
    private lateinit var btnSave: ImageView
    private lateinit var likesCount: TextView
    private lateinit var description: TextView
    private lateinit var viewAllComments: TextView
    private lateinit var timeAgo: TextView

    private var postId: String = ""
    private var currentPost: Post? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_post_view)

        postId = intent.getStringExtra("post_id") ?: ""
        if (postId.isEmpty()) {
            Toast.makeText(this, "Invalid post", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        initViews()
        setupClickListeners()
        loadPost()

        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                finish()
            }
        })
    }

    private fun initViews() {
        btnBack = findViewById(R.id.btnBack)
        userProfileImage = findViewById(R.id.userProfileImage)
        username = findViewById(R.id.username)
        location = findViewById(R.id.location)
        imagesViewPager = findViewById(R.id.imagesViewPager)
        imageIndicator = findViewById(R.id.imageIndicator)
        btnLike = findViewById(R.id.btnLike)
        btnComment = findViewById(R.id.btnComment)
        btnSend = findViewById(R.id.btnSend)
        btnSave = findViewById(R.id.btnSave)
        likesCount = findViewById(R.id.likesCount)
        description = findViewById(R.id.description)
        viewAllComments = findViewById(R.id.viewAllComments)
        timeAgo = findViewById(R.id.timeAgo)
    }

    private fun setupClickListeners() {
        btnBack.setOnClickListener {
            finish()
        }

        btnLike.setOnClickListener {
            currentPost?.let { post ->
                toggleLike(post)
            }
        }

        btnComment.setOnClickListener {
            openComments()
        }

        viewAllComments.setOnClickListener {
            openComments()
        }

        userProfileImage.setOnClickListener {
            currentPost?.let { post ->
                navigateToUserProfile(post.userId, post.username)
            }
        }

        username.setOnClickListener {
            currentPost?.let { post ->
                navigateToUserProfile(post.userId, post.username)
            }
        }
    }

    private fun loadPost() {
        db.child("posts").child(postId).addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val post = snapshot.getValue(Post::class.java)
                if (post != null) {
                    currentPost = post
                    displayPost(post)
                } else {
                    Toast.makeText(this@PostViewActivity, "Post not found", Toast.LENGTH_SHORT).show()
                    finish()
                }
            }

            override fun onCancelled(error: DatabaseError) {
                Toast.makeText(
                    this@PostViewActivity,
                    "Failed to load post",
                    Toast.LENGTH_SHORT
                ).show()
                finish()
            }
        })
    }

    private fun displayPost(post: Post) {
        // Set username
        username.text = post.username

        // Set location
        if (post.location.isNotEmpty()) {
            location.text = post.location
            location.visibility = View.VISIBLE
        } else {
            location.visibility = View.GONE
        }

        // Load profile image
        loadProfileImage(post.userPhotoBase64)

        // Setup images ViewPager
        val imagesAdapter = PostImagesAdapter(post.images)
        imagesViewPager.adapter = imagesAdapter

        // Show indicator only if multiple images
        if (post.images.size > 1) {
            imageIndicator.visibility = View.VISIBLE
            imageIndicator.text = "1/${post.images.size}"

            imagesViewPager.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
                override fun onPageSelected(position: Int) {
                    imageIndicator.text = "${position + 1}/${post.images.size}"
                }
            })
        } else {
            imageIndicator.visibility = View.GONE
        }

        // Set like button state
        val currentUserId = auth.currentUser?.uid ?: ""
        updateLikeButton(post.isLikedBy(currentUserId))

        // Set likes count
        val count = post.getLikesCount()
        likesCount.text = when {
            count == 0 -> "Be the first to like this"
            count == 1 -> "1 like"
            else -> "$count likes"
        }

        // Set description
        val descriptionText = "${post.username} ${post.description}"
        description.text = descriptionText

        // Set comments count
        viewAllComments.text = if (post.commentsCount == 0) {
            "No comments yet"
        } else {
            "View all ${post.commentsCount} comments"
        }

        // Set time ago
        timeAgo.text = PostUtils.getTimeAgo(post.timestamp)
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

    private fun updateLikeButton(isLiked: Boolean) {
        if (isLiked) {
            btnLike.setImageResource(R.drawable.red_heart)
        } else {
            btnLike.setImageResource(R.drawable.heart)
        }
    }

    private fun toggleLike(post: Post) {
        val currentUserId = auth.currentUser?.uid ?: return

        val likesRef = db.child("posts").child(post.postId).child("likes")

        if (post.isLikedBy(currentUserId)) {
            // Unlike
            likesRef.child(currentUserId).removeValue()
        } else {
            // Like
            likesRef.child(currentUserId).setValue(true)
        }
    }

    private fun openComments() {
        val intent = Intent(this, CommentsActivity::class.java).apply {
            putExtra("post_id", postId)
        }
        startActivity(intent)
    }

    private fun navigateToUserProfile(userId: String, username: String) {
        // Navigate to user profile (you can implement this based on your navigation)
        Toast.makeText(this, "Navigate to $username's profile", Toast.LENGTH_SHORT).show()
    }
}