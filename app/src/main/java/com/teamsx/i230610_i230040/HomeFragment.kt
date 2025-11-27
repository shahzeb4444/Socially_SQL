package com.teamsx.i230610_i230040

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.teamsx.i230610_i230040.network.Resource
import com.teamsx.i230610_i230040.utils.UserPreferences
import com.teamsx.i230610_i230040.viewmodels.PostViewModel
import com.teamsx.i230610_i230040.viewmodels.StoryViewModel
import com.teamsx.i230610_i230040.repository.PostRepository
import com.teamsx.i230610_i230040.repository.StoryRepository
import com.teamsx.i230610_i230040.utils.NetworkMonitor
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch

class HomeFragment : Fragment() {

    private val userPreferences by lazy { UserPreferences(requireContext()) }
    private lateinit var postViewModel: PostViewModel
    private lateinit var storyViewModel: StoryViewModel
    private lateinit var postRepository: PostRepository
    private lateinit var storyRepository: StoryRepository
    private lateinit var networkMonitor: NetworkMonitor

    private lateinit var storiesRecyclerView: RecyclerView
    private lateinit var postsRecyclerView: RecyclerView
    private lateinit var storiesAdapter: StoriesAdapter
    private lateinit var postsAdapter: PostsAdapter

    private val storyGroups = mutableListOf<StoryGroup>()
    private val posts = mutableListOf<Post>()


    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View = inflater.inflate(R.layout.fragment_home, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Initialize offline support
        postRepository = PostRepository(requireContext())
        storyRepository = StoryRepository(requireContext())
        networkMonitor = NetworkMonitor(requireContext())

        postViewModel = ViewModelProvider(this)[PostViewModel::class.java]
        storyViewModel = ViewModelProvider(this)[StoryViewModel::class.java]

        setupTopBarListeners(view)
        setupStoriesRecyclerView(view)
        setupPostsRecyclerView(view)
        observeViewModels()
        observeOfflineData()

        loadStories()
        loadPosts()
    }

    private fun observeViewModels() {
        // Observe stories
        storyViewModel.feedStoriesState.observe(viewLifecycleOwner) { resource ->
            when (resource) {
                is Resource.Success<*> -> {
                    @Suppress("UNCHECKED_CAST")
                    val fetchedStoryGroups = resource.data as? List<StoryGroup> ?: emptyList()
                    Log.d("HomeFragment", "Stories loaded successfully: ${fetchedStoryGroups.size} groups")
                    storyGroups.clear()
                    storyGroups.addAll(fetchedStoryGroups)
                    storiesAdapter.notifyDataSetChanged()
                    Log.d("HomeFragment", "Stories adapter updated")
                }
                is Resource.Error<*> -> {
                    Log.e("HomeFragment", "Failed to load stories: ${resource.message}")
                    Toast.makeText(requireContext(), "Failed to load stories: ${resource.message}", Toast.LENGTH_SHORT).show()
                }
                is Resource.Loading<*> -> {
                    Log.d("HomeFragment", "Loading stories...")
                }
                null -> {}
            }
        }

        // Observe posts
        postViewModel.feedPostsState.observe(viewLifecycleOwner) { resource ->
            when (resource) {
                is Resource.Success<*> -> {
                    @Suppress("UNCHECKED_CAST")
                    val fetchedPosts = resource.data as? List<Post> ?: emptyList()
                    Log.d("HomeFragment", "Posts loaded successfully: ${fetchedPosts.size} posts")
                    posts.clear()
                    posts.addAll(fetchedPosts)
                    postsAdapter.notifyDataSetChanged()
                    Log.d("HomeFragment", "Posts adapter updated, total posts: ${posts.size}")
                }
                is Resource.Error<*> -> {
                    Log.e("HomeFragment", "Failed to load posts: ${resource.message}")
                    Toast.makeText(requireContext(), "Failed to load posts: ${resource.message}", Toast.LENGTH_SHORT).show()
                }
                is Resource.Loading<*> -> {
                    Log.d("HomeFragment", "Loading posts...")
                }
                null -> {}
            }
        }

        // Observe toggle like
        postViewModel.toggleLikeState.observe(viewLifecycleOwner) { resource ->
            when (resource) {
                is Resource.Success<*> -> {
                    @Suppress("UNCHECKED_CAST")
                    val updatedPost = resource.data as? Post
                    if (updatedPost != null) {
                        val index = posts.indexOfFirst { it.postId == updatedPost.postId }
                        if (index != -1) {
                            posts[index] = updatedPost
                            postsAdapter.notifyItemChanged(index)
                        }
                    }
                    postViewModel.clearToggleLikeState()
                }
                is Resource.Error<*> -> {
                    Toast.makeText(requireContext(), "Failed to update like: ${resource.message}", Toast.LENGTH_SHORT).show()
                    postViewModel.clearToggleLikeState()
                }
                is Resource.Loading<*> -> {}
                null -> {}
            }
        }
    }

    override fun onResume() {
        super.onResume()
        loadStories()
        loadPosts()
    }

    private fun setupTopBarListeners(view: View) {
        val messages = view.findViewById<ImageView>(R.id.messangerlogo)
        val cameraicon = view.findViewById<ImageView>(R.id.cameralogo)

        cameraicon.setOnClickListener {
            val intent = Intent(Intent.ACTION_VIEW).apply { type = "image/*" }
            startActivity(intent)
        }

        messages.setOnClickListener {
            startActivity(Intent(requireContext(), socialhomescreen4::class.java))
        }
    }

    private fun setupStoriesRecyclerView(view: View) {
        storiesRecyclerView = view.findViewById(R.id.storiesRecyclerView)
        storiesRecyclerView.layoutManager = LinearLayoutManager(
            requireContext(),
            LinearLayoutManager.HORIZONTAL,
            false
        )

        val currentUserId = getCurrentUserId() ?: ""

        storiesAdapter = StoriesAdapter(
            stories = storyGroups,
            currentUserId = currentUserId,
            onStoryClick = { storyGroup ->
                openStoryViewer(storyGroup)
            },
            onAddStoryClick = {
                openCreateStory()
            }
        )

        storiesRecyclerView.adapter = storiesAdapter
    }

    private fun setupPostsRecyclerView(view: View) {
        postsRecyclerView = view.findViewById(R.id.postsRecyclerView)
        postsRecyclerView.layoutManager = LinearLayoutManager(requireContext())

        val currentUserId = getCurrentUserId() ?: ""

        postsAdapter = PostsAdapter(
            posts = posts,
            currentUserId = currentUserId,
            onLikeClick = { post ->
                toggleLike(post)
            },
            onCommentClick = { post ->
                openComments(post)
            },
            onPostClick = { post ->
                openPostView(post)
            },
            onProfileClick = { userId, username ->
                navigateToUserProfile(userId, username)
            }
        )

        postsRecyclerView.adapter = postsAdapter
    }

    private fun loadStories() {
        val currentUserId = getCurrentUserId() ?: return
        Log.d("HomeFragment", "Loading stories for user: $currentUserId")

        // Load from server and cache using repository
        lifecycleScope.launch {
            try {
                storyRepository.fetchStoriesFromServer(currentUserId)
            } catch (e: Exception) {
                Log.e("HomeFragment", "Error fetching stories", e)
                // Stories will still load from cache via LiveData observer
            }
        }

        // Also load via ViewModel (for compatibility)
        storyViewModel.getFeedStories(currentUserId)
    }

    private fun loadPosts() {
        val currentUserId = getCurrentUserId() ?: return
        Log.d("HomeFragment", "Loading posts for user: $currentUserId")

        // Load from server and cache using repository
        lifecycleScope.launch {
            try {
                postRepository.fetchFeedFromServer(currentUserId)
            } catch (e: Exception) {
                Log.e("HomeFragment", "Error fetching posts", e)
                // Posts will still load from cache via LiveData observer
            }
        }

        // Also load via ViewModel (for compatibility)
        postViewModel.getFeedPosts(currentUserId)
    }


    private fun toggleLike(post: Post) {
        val currentUserId = getCurrentUserId() ?: return

        // Toggle like using repository (offline-first)
        lifecycleScope.launch {
            try {
                postRepository.toggleLike(post.postId, currentUserId)
            } catch (e: Exception) {
                Log.e("HomeFragment", "Error toggling like", e)
            }
        }

        // Also update via ViewModel (for compatibility)
        postViewModel.toggleLike(post.postId, currentUserId)
    }

    private fun openComments(post: Post) {
        val intent = Intent(requireContext(), CommentsActivity::class.java).apply {
            putExtra("post_id", post.postId)
        }
        startActivity(intent)
    }

    private fun openPostView(post: Post) {
        val intent = Intent(requireContext(), PostViewActivity::class.java).apply {
            putExtra("post_id", post.postId)
        }
        startActivity(intent)
    }

    private fun openStoryViewer(storyGroup: StoryGroup) {
        val currentUserId = getCurrentUserId() ?: return

        if (storyGroup.userId == currentUserId) {
            val intent = Intent(requireContext(), socialhomescreen14::class.java).apply {
                putExtra("user_id", storyGroup.userId)
                putExtra("username", storyGroup.username)
            }
            startActivity(intent)
        } else {
            val intent = Intent(requireContext(), socialhomescreen12::class.java).apply {
                putExtra("user_id", storyGroup.userId)
                putExtra("username", storyGroup.username)
                putExtra("start_index", 0)
            }
            startActivity(intent)
        }
    }

    /**
     * Observe offline data from local database (auto-updates UI)
     */
    private fun observeOfflineData() {
        // Observe cached posts
        postRepository.getAllPosts().observe(viewLifecycleOwner) { cachedPosts ->
            if (cachedPosts.isNotEmpty()) {
                Log.d("HomeFragment", "Loaded ${cachedPosts.size} posts from cache")
                posts.clear()
                posts.addAll(cachedPosts.map { entity ->
                    Post(
                        postId = entity.postId,
                        userId = entity.userId,
                        username = entity.username,
                        userPhotoBase64 = entity.userProfileImage,
                        location = entity.location,
                        description = entity.description,
                        images = entity.images.split(",").filter { it.isNotEmpty() },
                        timestamp = entity.timestamp,
                        likesCount = entity.likesCount,
                        commentsCount = entity.commentsCount
                    )
                })
                postsAdapter.notifyDataSetChanged()
            }
        }

        // Observe cached stories
        storyRepository.getActiveStories().observe(viewLifecycleOwner) { cachedStories ->
            if (cachedStories.isNotEmpty()) {
                Log.d("HomeFragment", "Loaded ${cachedStories.size} stories from cache")

                // Group stories by user
                val groupedStories = cachedStories.groupBy { it.userId }
                storyGroups.clear()

                val currentUserIdValue = getCurrentUserId() ?: ""

                groupedStories.forEach { (userId, storiesForUser) ->
                    val firstStory = storiesForUser.first()

                    // Check if user has unviewed stories
                    val hasUnviewed = storiesForUser.any { story ->
                        val viewedByList = story.viewedBy.split(",").filter { it.isNotEmpty() }
                        !viewedByList.contains(currentUserIdValue)
                    }

                    val storyGroup = StoryGroup(
                        userId = userId,
                        username = firstStory.username,
                        userPhotoBase64 = firstStory.userPhotoBase64,
                        stories = storiesForUser.map { entity ->
                            Story(
                                storyId = entity.storyId,
                                userId = entity.userId,
                                username = entity.username,
                                userPhotoBase64 = entity.userPhotoBase64,
                                imageBase64 = entity.imageBase64,
                                timestamp = entity.timestamp,
                                expiresAt = entity.expiresAt,
                                viewedBy = entity.viewedBy.split(",")
                                    .filter { it.isNotEmpty() }
                                    .associateWith { true },
                                isCloseFreindsOnly = entity.isCloseFriendsOnly
                            )
                        },
                        hasUnviewedStories = hasUnviewed
                    )
                    storyGroups.add(storyGroup)
                }
                storiesAdapter.notifyDataSetChanged()
            }
        }
    }

    private fun openCreateStory() {
        startActivity(Intent(requireContext(), socialhomescreen15::class.java))
    }

    private fun navigateToUserProfile(userId: String, username: String) {
        val bundle = Bundle().apply {
            putString("user_id", userId)
            putString("username", username)
        }
        findNavController().navigate(R.id.other_user_profile, bundle)
    }

    // Helper methods
    private fun getCurrentUserId(): String? {
        return userPreferences.getUser()?.uid
    }
}