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

class HomeFragment : Fragment() {

    private val userPreferences by lazy { UserPreferences(requireContext()) }
    private lateinit var postViewModel: PostViewModel
    private lateinit var storyViewModel: StoryViewModel

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

        postViewModel = ViewModelProvider(this)[PostViewModel::class.java]
        storyViewModel = ViewModelProvider(this)[StoryViewModel::class.java]

        setupTopBarListeners(view)
        setupStoriesRecyclerView(view)
        setupPostsRecyclerView(view)
        observeViewModels()

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
        storyViewModel.getFeedStories(currentUserId)
    }

    private fun loadPosts() {
        val currentUserId = getCurrentUserId() ?: return
        Log.d("HomeFragment", "Loading posts for user: $currentUserId")
        postViewModel.getFeedPosts(currentUserId)
    }


    private fun toggleLike(post: Post) {
        val currentUserId = getCurrentUserId() ?: return
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