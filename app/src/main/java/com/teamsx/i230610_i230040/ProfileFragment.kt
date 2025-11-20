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
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.imageview.ShapeableImageView
import com.teamsx.i230610_i230040.utils.UserPreferences
import com.teamsx.i230610_i230040.viewmodels.UserProfileViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class ProfileFragment : Fragment() {

    private val userPreferences by lazy { UserPreferences(requireContext()) }
    private lateinit var viewModel: UserProfileViewModel

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

        viewModel = ViewModelProvider(this)[UserProfileViewModel::class.java]

        initViews(view)
        setupClickListeners(view)
        setupPostsGrid(view)
        setupObservers()
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

    private fun setupObservers() {
        viewModel.profile.observe(viewLifecycleOwner) { profile ->
            profile?.let { displayProfile(it) }
        }

        viewModel.error.observe(viewLifecycleOwner) { error ->
            error?.let {
                Toast.makeText(requireContext(), it, Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun loadUserProfile() {
        // Get UID from UserPreferences
        val currentUser = userPreferences.getUser()
        val uid = currentUser?.uid

        if (uid == null) {
            Log.e("ProfileFragment", "User not logged in - UID is null")
            Toast.makeText(requireContext(), "User not logged in", Toast.LENGTH_SHORT).show()
            // Navigate back to login
            startActivity(Intent(requireContext(), mainlogin::class.java))
            requireActivity().finish()
            return
        }

        Log.d("ProfileFragment", "Loading profile for UID: $uid")

        // Call ViewModel to load profile from PHP API
        viewModel.loadUserProfile(uid, uid)
    }

    private fun displayProfile(profile: com.teamsx.i230610_i230040.network.ApiUserProfile) {
        usernameTextView.text = profile.username
        fullNameTextView.text = profile.fullName ?: profile.username
        bioTextView.text = profile.bio ?: ""

        postsCountTextView.text = profile.postsCount.toString()
        followersCountTextView.text = profile.followersCount.toString()
        followingCountTextView.text = profile.followingCount.toString()

        // Load profile image from Base64
        if (!profile.profileImageUrl.isNullOrEmpty()) {
            try {
                val base64String = if (profile.profileImageUrl.startsWith("data:")) {
                    profile.profileImageUrl.substringAfter("base64,")
                } else {
                    profile.profileImageUrl
                }
                val decodedBytes = Base64.decode(base64String, Base64.DEFAULT)
                val bitmap = BitmapFactory.decodeByteArray(decodedBytes, 0, decodedBytes.size)
                profileImageView.setImageBitmap(getCircularBitmap(bitmap))
            } catch (e: Exception) {
                e.printStackTrace()
                profileImageView.setImageResource(R.drawable.whitecircle)
            }
        } else {
            profileImageView.setImageResource(R.drawable.whitecircle)
        }
    }

    private fun loadUserPosts() {
        // TODO: Implement API call to get user posts from PHP backend
        // For now, keeping posts empty until we create the endpoint
        userPosts.clear()
        profileGridAdapter.notifyDataSetChanged()
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


    private fun openPostView(post: Post) {
        val intent = Intent(requireContext(), PostViewActivity::class.java).apply {
            putExtra("post_id", post.postId)
        }
        startActivity(intent)
    }
}