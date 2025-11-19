package com.teamsx.i230610_i230040

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.imageview.ShapeableImageView
import com.teamsx.i230610_i230040.network.Resource
import com.teamsx.i230610_i230040.utils.UserPreferences
import com.teamsx.i230610_i230040.viewmodels.FollowViewModel
import kotlinx.coroutines.launch

class OtherUserProfileFragment : Fragment() {

    companion object {
        const val ARG_USER_ID = "user_id"
        const val ARG_USERNAME = "username"
    }

    private val followViewModel: FollowViewModel by viewModels()
    private val userPreferences by lazy { UserPreferences(requireContext()) }

    private lateinit var profileImageView: ShapeableImageView
    private lateinit var usernameTextView: TextView
    private lateinit var fullNameTextView: TextView
    private lateinit var bioTextView: TextView
    private lateinit var postsCountTextView: TextView
    private lateinit var followersCountTextView: TextView
    private lateinit var followingCountTextView: TextView
    private lateinit var followButtonBg: ImageView
    private lateinit var followLabel: TextView
    private lateinit var backArrow: ImageView
    private lateinit var postsGridRecyclerView: RecyclerView

    private lateinit var profileGridAdapter: ProfileGridAdapter
    private val userPosts = mutableListOf<Post>()

    private var userId: String? = null
    private var username: String? = null

    private var currentRelationship: String = "none" // none, requested, incoming_request, following, followed_by

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            userId = it.getString(ARG_USER_ID)
            username = it.getString(ARG_USERNAME)
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? =
        inflater.inflate(R.layout.fragment_other_user_profile, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        initViews(view)
        setupClickListeners()
        setupPostsGrid()
        setupObservers()

        loadUserProfile()
        loadUserPosts()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        // No Firebase listeners to clean up
    }

    private fun initViews(view: View) {
        profileImageView = view.findViewById(R.id.profileicon)
        usernameTextView = view.findViewById(R.id.jacobtext)
        fullNameTextView = view.findViewById(R.id.profileusername)
        bioTextView = view.findViewById(R.id.d1)
        postsCountTextView = view.findViewById(R.id.noposts)
        followersCountTextView = view.findViewById(R.id.nofollowers)
        followingCountTextView = view.findViewById(R.id.nofollowing)
        followButtonBg = view.findViewById(R.id.feedbutton1)
        followLabel = view.findViewById(R.id.followLabel)
        backArrow = view.findViewById(R.id.leftarrow)
        postsGridRecyclerView = view.findViewById(R.id.postsGridRecyclerView)
    }

    private fun setupClickListeners() {
        backArrow.setOnClickListener { requireActivity().onBackPressedDispatcher.onBackPressed() }

        val clickFollow: (View) -> Unit = { onFollowClicked() }
        followButtonBg.setOnClickListener(clickFollow)
        followLabel.setOnClickListener(clickFollow)
    }

    private fun setupPostsGrid() {
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
        // Observe user profile
        followViewModel.userProfile.observe(viewLifecycleOwner) { resource ->
            when (resource) {
                is Resource.Success -> {
                    resource.data?.let { wrapper ->
                        displayProfile(wrapper.user)
                        currentRelationship = wrapper.relationship ?: "none"
                        renderFollowButton()
                    }
                }
                is Resource.Error -> {
                    toast(resource.message ?: "Failed to load profile")
                }
                is Resource.Loading -> {
                    // Show loading if needed
                }
                else -> {}
            }
        }

        // Observe follow action results
        followViewModel.followActionResult.observe(viewLifecycleOwner) { resource ->
            when (resource) {
                is Resource.Success -> {
                    toast(resource.data?.message ?: "Action completed")
                    // Reload profile to get updated relationship status
                    val uid = userId ?: return@observe
                    val currentUid = userPreferences.getUser()?.uid
                    followViewModel.getUserProfile(uid, currentUid)
                }
                is Resource.Error -> {
                    toast(resource.message ?: "Action failed")
                }
                is Resource.Loading -> {
                    // Show loading if needed
                }
                else -> {}
            }
        }
    }

    private fun loadUserProfile() {
        val uid = userId ?: return
        val currentUid = userPreferences.getUser()?.uid
        followViewModel.getUserProfile(uid, currentUid)
    }

    private fun renderFollowButton() {
        when (currentRelationship) {
            "none", "followed_by" -> followLabel.text = "Follow"
            "requested" -> followLabel.text = "Requested"
            "incoming_request" -> followLabel.text = "Accept"
            "following" -> followLabel.text = "Following"
            else -> followLabel.text = "Follow"
        }
    }

    private fun onFollowClicked() {
        val me = userPreferences.getUser()?.uid ?: run { toast("Login required"); return }
        val other = userId ?: return

        when (currentRelationship) {
            "none", "followed_by" -> followViewModel.sendFollowRequest(me, other)
            "requested" -> followViewModel.cancelFollowRequest(me, other)
            "incoming_request" -> followViewModel.acceptFollowRequest(other, me)
            "following" -> followViewModel.unfollowUser(me, other)
        }
    }

    private fun displayProfile(profile: com.teamsx.i230610_i230040.network.ApiUserProfile) {
        usernameTextView.text = profile.username
        fullNameTextView.text = profile.fullName ?: "User"
        bioTextView.text = profile.bio ?: "No bio yet"
        postsCountTextView.text = profile.postsCount.toString()
        followersCountTextView.text = formatCount(profile.followersCount)
        followingCountTextView.text = profile.followingCount.toString()

        if (!profile.profileImageUrl.isNullOrEmpty()) {
            val bmp = ImageUtils.loadBase64ImageOptimized(profile.profileImageUrl, 300)
            if (bmp != null) profileImageView.setImageBitmap(ImageUtils.getCircularBitmap(bmp, 300))
            else setDefaultProfileImage()
        } else setDefaultProfileImage()
    }

    private fun setDefaultProfileImage() {
        profileImageView.setImageResource(R.drawable.profile_login_splash)
    }

    private fun formatCount(count: Int): String =
        when {
            count >= 1_000_000 -> String.format("%.1fM", count / 1_000_000.0)
            count >= 1_000 -> String.format("%.1fK", count / 1_000.0)
            else -> count.toString()
        }

    private fun toast(msg: String) = Toast.makeText(requireContext(), msg, Toast.LENGTH_SHORT).show()

    private fun openPostView(post: Post) {
        val intent = Intent(requireContext(), PostViewActivity::class.java).apply {
            putExtra("post_id", post.postId)
        }
        startActivity(intent)
    }

    private fun loadUserPosts() {
        // TODO: Convert to PHP backend - for now keep Firebase implementation
        // This will be updated when we implement posts fetching via PHP
    }
}