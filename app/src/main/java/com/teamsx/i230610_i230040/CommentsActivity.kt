package com.teamsx.i230610_i230040

import android.graphics.BitmapFactory
import android.os.Bundle
import android.util.Base64
import android.view.View
import android.widget.EditText
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.imageview.ShapeableImageView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*

class CommentsActivity : AppCompatActivity() {

    private val auth by lazy { FirebaseAuth.getInstance() }
    private val db by lazy { FirebaseDatabase.getInstance().reference }

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
        val uid = auth.currentUser?.uid ?: return

        db.child("users").child(uid).child("photoBase64").get()
            .addOnSuccessListener { snapshot ->
                val photoBase64 = snapshot.getValue(String::class.java)
                if (!photoBase64.isNullOrEmpty()) {
                    try {
                        val bytes = Base64.decode(photoBase64, Base64.DEFAULT)
                        val bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
                        userProfileImage.setImageBitmap(bitmap)
                    } catch (e: Exception) {
                        userProfileImage.setImageResource(R.drawable.profile_login_splash_small)
                    }
                }
            }
    }

    private fun loadComments() {
        db.child("comments").child(postId)
            .addValueEventListener(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    comments.clear()

                    for (commentSnap in snapshot.children) {
                        val comment = commentSnap.getValue(Comment::class.java)
                        if (comment != null) {
                            comments.add(comment)
                        }
                    }

                    // Sort by timestamp (oldest first)
                    comments.sortBy { it.timestamp }

                    if (comments.isEmpty()) {
                        noCommentsText.visibility = View.VISIBLE
                        commentsRecyclerView.visibility = View.GONE
                    } else {
                        noCommentsText.visibility = View.GONE
                        commentsRecyclerView.visibility = View.VISIBLE
                    }

                    commentsAdapter.notifyDataSetChanged()

                    // Scroll to bottom to show latest comment
                    if (comments.isNotEmpty()) {
                        commentsRecyclerView.scrollToPosition(comments.size - 1)
                    }
                }

                override fun onCancelled(error: DatabaseError) {
                    Toast.makeText(
                        this@CommentsActivity,
                        "Failed to load comments",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            })
    }

    private fun postComment() {
        val message = etComment.text.toString().trim()
        if (message.isEmpty()) {
            Toast.makeText(this, "Please write a comment", Toast.LENGTH_SHORT).show()
            return
        }

        val currentUser = auth.currentUser
        if (currentUser == null) {
            Toast.makeText(this, "Please log in first", Toast.LENGTH_SHORT).show()
            return
        }

        val uid = currentUser.uid

        // Get user info
        db.child("users").child(uid).get()
            .addOnSuccessListener { snapshot ->
                val userProfile = snapshot.getValue(UserProfile::class.java)

                if (userProfile == null) {
                    Toast.makeText(this, "User profile not found", Toast.LENGTH_SHORT).show()
                    return@addOnSuccessListener
                }

                val commentId = PostUtils.generateCommentId()

                val comment = Comment(
                    commentId = commentId,
                    postId = postId,
                    userId = uid,
                    username = userProfile.username,
                    userPhotoBase64 = userProfile.photoBase64,
                    message = message,
                    timestamp = System.currentTimeMillis()
                )

                // Save comment
                db.child("comments").child(postId).child(commentId).setValue(comment)
                    .addOnSuccessListener {
                        // Update comments count on post
                        db.child("posts").child(postId).child("commentsCount")
                            .get()
                            .addOnSuccessListener { countSnapshot ->
                                val currentCount = countSnapshot.getValue(Int::class.java) ?: 0
                                db.child("posts").child(postId).child("commentsCount")
                                    .setValue(currentCount + 1)
                            }

                        // Clear input
                        etComment.text.clear()
                        Toast.makeText(this, "Comment added", Toast.LENGTH_SHORT).show()
                    }
                    .addOnFailureListener { e ->
                        Toast.makeText(
                            this,
                            "Failed to post comment: ${e.message}",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
            }
            .addOnFailureListener { e ->
                Toast.makeText(
                    this,
                    "Failed to load user profile: ${e.message}",
                    Toast.LENGTH_SHORT
                ).show()
            }
    }
}