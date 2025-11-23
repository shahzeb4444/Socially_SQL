package com.teamsx.i230610_i230040

import android.graphics.BitmapFactory
import android.os.Bundle
import android.util.Base64
import android.util.Log
import android.view.View
import android.widget.EditText
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.imageview.ShapeableImageView
import com.teamsx.i230610_i230040.utils.UserPreferences
import com.teamsx.i230610_i230040.network.RetrofitInstance
import com.teamsx.i230610_i230040.network.*
import kotlinx.coroutines.launch

class CommentsActivity : AppCompatActivity() {

    private val userPreferences by lazy { UserPreferences(this) }

    private lateinit var btnBack: ImageView
    private lateinit var commentsRecyclerView: RecyclerView
    private lateinit var noCommentsText: TextView
    private lateinit var userProfileImage: ShapeableImageView
    private lateinit var etComment: EditText
    private lateinit var btnSendComment: ImageView

    private lateinit var commentsAdapter: CommentsAdapter
    private val comments = mutableListOf<Comment>()

    private var postId: String = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_comments)

        postId = intent.getStringExtra("post_id") ?: ""
        if (postId.isEmpty()) {
            Toast.makeText(this, "Invalid post", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        initViews()
        setupRecyclerView()
        setupClickListeners()
        loadUserProfileImage()
        loadComments()

        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                finish()
            }
        })
    }

    private fun initViews() {
        btnBack = findViewById(R.id.btnBack)
        commentsRecyclerView = findViewById(R.id.commentsRecyclerView)
        noCommentsText = findViewById(R.id.noCommentsText)
        userProfileImage = findViewById(R.id.userProfileImage)
        etComment = findViewById(R.id.etComment)
        btnSendComment = findViewById(R.id.btnSendComment)
    }

    private fun setupRecyclerView() {
        commentsRecyclerView.layoutManager = LinearLayoutManager(this)
        commentsAdapter = CommentsAdapter(comments)
        commentsRecyclerView.adapter = commentsAdapter
    }

    private fun setupClickListeners() {
        btnBack.setOnClickListener {
            finish()
        }

        btnSendComment.setOnClickListener {
            postComment()
        }
    }

    private fun loadUserProfileImage() {
        val currentUser = userPreferences.getUser() ?: return
        val photoBase64 = currentUser.profileImageUrl

        if (photoBase64 != null && photoBase64.isNotEmpty()) {
            try {
                val cleanBase64 = if (photoBase64.startsWith("data:")) {
                    photoBase64.substringAfter("base64,")
                } else {
                    photoBase64
                }
                val bytes = Base64.decode(cleanBase64, Base64.DEFAULT)
                val bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
                userProfileImage.setImageBitmap(bitmap)
            } catch (e: Exception) {
                Log.e("CommentsActivity", "Error loading profile image", e)
                userProfileImage.setImageResource(R.drawable.profile_login_splash_small)
            }
        } else {
            userProfileImage.setImageResource(R.drawable.profile_login_splash_small)
        }
    }

    private fun loadComments() {
        lifecycleScope.launch {
            try {
                val request = GetCommentsRequest(postId)
                Log.d("CommentsActivity", "Loading comments for post: $postId")

                val response = RetrofitInstance.apiService.getComments(request)

                Log.d("CommentsActivity", "Response code: ${response.code()}")
                Log.d("CommentsActivity", "Response successful: ${response.isSuccessful}")
                Log.d("CommentsActivity", "Response body: ${response.body()}")

                if (response.isSuccessful && response.body()?.success == true) {
                    val apiComments = response.body()?.data?.comments ?: emptyList()

                    Log.d("CommentsActivity", "Loaded ${apiComments.size} comments")

                    comments.clear()
                    comments.addAll(apiComments.map { apiComment ->
                        Comment(
                            commentId = apiComment.commentId,
                            postId = apiComment.postId,
                            userId = apiComment.userId,
                            username = apiComment.username,
                            userPhotoBase64 = apiComment.userPhotoBase64,
                            message = apiComment.message,
                            timestamp = apiComment.timestamp
                        )
                    })

                    // Always update UI - don't show error for empty comments
                    if (comments.isEmpty()) {
                        noCommentsText.visibility = View.VISIBLE
                        commentsRecyclerView.visibility = View.GONE
                    } else {
                        noCommentsText.visibility = View.GONE
                        commentsRecyclerView.visibility = View.VISIBLE
                    }

                    commentsAdapter.notifyDataSetChanged()

                    if (comments.isNotEmpty()) {
                        commentsRecyclerView.scrollToPosition(comments.size - 1)
                    }
                } else {
                    val errorMsg = response.body()?.error ?: "Unknown error"
                    Log.e("CommentsActivity", "Failed to load comments: $errorMsg")
                    Log.e("CommentsActivity", "Response code: ${response.code()}")

                    // Don't show toast if there are just no comments yet
                    if (response.isSuccessful && response.body()?.success == false) {
                        Toast.makeText(
                            this@CommentsActivity,
                            "Error: $errorMsg",
                            Toast.LENGTH_SHORT
                        ).show()
                    } else if (!response.isSuccessful) {
                        Toast.makeText(
                            this@CommentsActivity,
                            "Failed to load comments (HTTP ${response.code()})",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                }
            } catch (e: Exception) {
                Log.e("CommentsActivity", "Error loading comments", e)
                e.printStackTrace()
                Toast.makeText(
                    this@CommentsActivity,
                    "Error: ${e.message}",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    }

    private fun postComment() {
        val message = etComment.text.toString().trim()
        if (message.isEmpty()) {
            Toast.makeText(this, "Please write a comment", Toast.LENGTH_SHORT).show()
            return
        }

        val currentUser = userPreferences.getUser()
        if (currentUser == null) {
            Toast.makeText(this, "Please log in first", Toast.LENGTH_SHORT).show()
            return
        }

        lifecycleScope.launch {
            try {
                val request = AddCommentRequest(
                    postId = postId,
                    userId = currentUser.uid,
                    message = message
                )

                val response = RetrofitInstance.apiService.addComment(request)

                if (response.isSuccessful && response.body()?.success == true) {
                    etComment.text.clear()
                    Toast.makeText(this@CommentsActivity, "Comment added", Toast.LENGTH_SHORT).show()

                    // Reload comments to show the new one
                    loadComments()
                } else {
                    val errorMsg = response.body()?.error ?: "Failed to post comment"
                    Toast.makeText(this@CommentsActivity, errorMsg, Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                Log.e("CommentsActivity", "Error posting comment", e)
                Toast.makeText(
                    this@CommentsActivity,
                    "Error: ${e.message}",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    }
}