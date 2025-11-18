package com.teamsx.i230610_i230040

import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.BitmapShader
import android.graphics.Shader
import android.os.Bundle
import android.util.Base64
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.imageview.ShapeableImageView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener

class ProfileFragment : Fragment() {

    private val auth by lazy { FirebaseAuth.getInstance() }
    private val db by lazy { FirebaseDatabase.getInstance().reference }

    private lateinit var profileImageView: ShapeableImageView
    private lateinit var usernameTextView: TextView
    private lateinit var fullNameTextView: TextView
    private lateinit var bioTextView: TextView
    private lateinit var postsCountTextView: TextView
    private lateinit var followersCountTextView: TextView
    private lateinit var followingCountTextView: TextView
    private lateinit var postsGridRecyclerView: RecyclerView

    private lateinit var profileGridAdapter: ProfileGridAdapter
    private val userPosts = mutableListOf<Post>()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View = inflater.inflate(R.layout.fragment_profile, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        initViews(view)
        setupClickListeners(view)
        setupPostsGrid(view)
        loadUserProfile()
        loadUserPosts()
    }

    override fun onResume() {
        super.onResume()
        loadUserProfile()
        loadUserPosts()
    }

    private fun initViews(view: View) {
        profileImageView = view.findViewById(R.id.profileicon)
        usernameTextView = view.findViewById(R.id.jacobtext)
        fullNameTextView = view.findViewById(R.id.profileusername)
        bioTextView = view.findViewById(R.id.d1)
        postsCountTextView = view.findViewById(R.id.noposts)
        followersCountTextView = view.findViewById(R.id.nofollowers)
        followingCountTextView = view.findViewById(R.id.nofollowing)
        postsGridRecyclerView = view.findViewById(R.id.postsGridRecyclerView)
    }

    private fun setupClickListeners(view: View) {
        val openFollowers = View.OnClickListener {
            startActivity(Intent(requireContext(), FollowListActivity::class.java)
                .putExtra(FollowListActivity.EXTRA_MODE, FollowListActivity.MODE_FOLLOWERS))
        }
        val openFollowing = View.OnClickListener {
            startActivity(Intent(requireContext(), FollowListActivity::class.java)
                .putExtra(FollowListActivity.EXTRA_MODE, FollowListActivity.MODE_FOLLOWING))
        }

        followersCountTextView.setOnClickListener(openFollowers)
        view.findViewById<TextView>(R.id.followers).setOnClickListener(openFollowers)
        followingCountTextView.setOnClickListener(openFollowing)
        view.findViewById<TextView>(R.id.following).setOnClickListener(openFollowing)

        val editBtn = view.findViewById<Button>(R.id.editprofilebtn)
        editBtn.setOnClickListener {
            startActivity(Intent(requireContext(), socialhomescreen11::class.java))
        }

        // Story highlights listeners
        listOf(
            view.findViewById<ImageView>(R.id.circle2),
            view.findViewById<ImageView>(R.id.circle3),
            view.findViewById<ImageView>(R.id.circle4)
        ).forEach {
            it.setOnClickListener { _ ->
                startActivity(Intent(requireContext(), socialhomescreen13::class.java))
            }
        }
    }

    private fun setupPostsGrid(view: View) {
        postsGridRecyclerView.layoutManager = GridLayoutManager(requireContext(), 3)

        profileGridAdapter = ProfileGridAdapter(
            posts = userPosts,
            onPostClick = { post ->
                openPostView(post)
            }
        )

        postsGridRecyclerView.adapter = profileGridAdapter
    }

    private fun loadUserProfile() {
        val uid = auth.currentUser?.uid
        if (uid == null) {
            Toast.makeText(requireContext(), "User not logged in", Toast.LENGTH_SHORT).show()
            return
        }

        db.child("users").child(uid).addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val profile = snapshot.getValue(UserProfile::class.java)
                if (profile != null) {
                    displayProfile(profile)
                } else {
                    Toast.makeText(requireContext(), "Profile not found", Toast.LENGTH_SHORT).show()
                }
            }

            override fun onCancelled(error: DatabaseError) {
                Toast.makeText(
                    requireContext(),
                    "Failed to load profile: ${error.message}",
                    Toast.LENGTH_SHORT
                ).show()
            }
        })
    }

    private fun loadUserPosts() {
        val uid = auth.currentUser?.uid ?: return

        db.child("userPosts").child(uid).addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                userPosts.clear()

                val postIds = snapshot.children.mapNotNull { it.key }

                if (postIds.isEmpty()) {
                    profileGridAdapter.notifyDataSetChanged()
                    return
                }

                val tempPosts = mutableListOf<Post>()
                var processedCount = 0

                for (postId in postIds) {
                    db.child("posts").child(postId).get()
                        .addOnSuccessListener { postSnapshot ->
                            val post = postSnapshot.getValue(Post::class.java)
                            if (post != null) {
                                synchronized(tempPosts) {
                                    tempPosts.add(post)
                                }
                            }

                            processedCount++
                            if (processedCount == postIds.size) {
                                // Sort by timestamp (newest first)
                                tempPosts.sortByDescending { it.timestamp }
                                userPosts.clear()
                                userPosts.addAll(tempPosts)
                                profileGridAdapter.notifyDataSetChanged()
                            }
                        }
                        .addOnFailureListener {
                            processedCount++
                            if (processedCount == postIds.size) {
                                tempPosts.sortByDescending { it.timestamp }
                                userPosts.clear()
                                userPosts.addAll(tempPosts)
                                profileGridAdapter.notifyDataSetChanged()
                            }
                        }
                }
            }

            override fun onCancelled(error: DatabaseError) {
                Toast.makeText(
                    requireContext(),
                    "Failed to load posts",
                    Toast.LENGTH_SHORT
                ).show()
            }
        })
    }

    private fun displayProfile(profile: UserProfile) {
        usernameTextView.text = profile.username

        val fullName = "${profile.firstName} ${profile.lastName}".trim()
        fullNameTextView.text = if (fullName.isNotEmpty()) fullName else "User"

        bioTextView.text = if (profile.bio.isNotEmpty()) profile.bio else "No bio yet"

        postsCountTextView.text = profile.postsCount.toString()
        followersCountTextView.text = formatCount(profile.followersCount)
        followingCountTextView.text = profile.followingCount.toString()

        loadProfilePhoto(profile.photoBase64)
    }

    private fun loadProfilePhoto(photoBase64: String?) {
        if (!photoBase64.isNullOrEmpty()) {
            try {
                val bytes = Base64.decode(photoBase64, Base64.DEFAULT)
                val bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
                if (bitmap != null) {
                    val circularBitmap = getCircularBitmap(bitmap)
                    profileImageView.setImageBitmap(circularBitmap)
                } else {
                    setDefaultProfileImage()
                }
            } catch (e: Exception) {
                e.printStackTrace()
                setDefaultProfileImage()
            }
        } else {
            setDefaultProfileImage()
        }
    }

    private fun setDefaultProfileImage() {
        profileImageView.setImageResource(R.drawable.profile_login_splash)
    }

    private fun getCircularBitmap(bitmap: Bitmap): Bitmap {
        val size = 300
        val scaled = Bitmap.createScaledBitmap(bitmap, size, size, true)

        val output = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(output)

        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            shader = BitmapShader(scaled, Shader.TileMode.CLAMP, Shader.TileMode.CLAMP)
        }

        val radius = size / 2f
        canvas.drawCircle(radius, radius, radius, paint)

        return output
    }

    private fun formatCount(count: Int): String {
        return when {
            count >= 1000000 -> String.format("%.1fM", count / 1000000.0)
            count >= 1000 -> String.format("%.1fK", count / 1000.0)
            else -> count.toString()
        }
    }

    private fun openPostView(post: Post) {
        val intent = Intent(requireContext(), PostViewActivity::class.java).apply {
            putExtra("post_id", post.postId)
        }
        startActivity(intent)
    }
}