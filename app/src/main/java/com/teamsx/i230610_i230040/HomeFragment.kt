package com.teamsx.i230610_i230040

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*

class HomeFragment : Fragment() {

    private val auth by lazy { FirebaseAuth.getInstance() }
    private val db by lazy { FirebaseDatabase.getInstance().reference }

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

        setupTopBarListeners(view)
        setupStoriesRecyclerView(view)
        setupPostsRecyclerView(view)

        loadStories()
        loadPosts()
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

        val currentUserId = auth.currentUser?.uid ?: ""

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

        val currentUserId = auth.currentUser?.uid ?: ""

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
        val currentUserId = auth.currentUser?.uid ?: return

        db.child("follows").child(currentUserId).child("following")
            .addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(followingSnapshot: DataSnapshot) {
                    val followingIds = followingSnapshot.children.mapNotNull { it.key }.toMutableList()
                    followingIds.add(0, currentUserId)
                    loadStoriesForUsers(followingIds, currentUserId)
                }

                override fun onCancelled(error: DatabaseError) {}
            })
    }

    private fun loadStoriesForUsers(userIds: List<String>, currentUserId: String) {
        storyGroups.clear()
        storiesAdapter.notifyDataSetChanged()

        val tempStoryGroups = mutableListOf<StoryGroup>()
        val storiesRef = db.child("stories")
        var processedCount = 0

        if (userIds.isEmpty()) return

        for (userId in userIds) {
            storiesRef.child(userId).addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    val userStories = mutableListOf<Story>()

                    for (storySnap in snapshot.children) {
                        val story = storySnap.getValue(Story::class.java)
                        if (story != null && story.isValid()) {
                            userStories.add(story)
                        }
                    }

                    userStories.sortBy { it.timestamp }
                    val hasUnviewed = userStories.any { !it.isViewedBy(currentUserId) }

                    db.child("users").child(userId).get()
                        .addOnSuccessListener { userSnapshot ->
                            val userProfile = userSnapshot.getValue(UserProfile::class.java)

                            if (userId == currentUserId) {
                                val storyGroup = StoryGroup(
                                    userId = userId,
                                    username = userProfile?.username ?: "You",
                                    userPhotoBase64 = userProfile?.photoBase64,
                                    stories = userStories,
                                    hasUnviewedStories = false
                                )
                                synchronized(tempStoryGroups) {
                                    tempStoryGroups.removeAll { it.userId == userId }
                                    tempStoryGroups.add(storyGroup)
                                }
                            } else if (userStories.isNotEmpty()) {
                                val storyGroup = StoryGroup(
                                    userId = userId,
                                    username = userProfile?.username ?: "User",
                                    userPhotoBase64 = userProfile?.photoBase64,
                                    stories = userStories,
                                    hasUnviewedStories = hasUnviewed
                                )
                                synchronized(tempStoryGroups) {
                                    tempStoryGroups.removeAll { it.userId == userId }
                                    tempStoryGroups.add(storyGroup)
                                }
                            }

                            processedCount++
                            if (processedCount == userIds.size) {
                                updateStoriesUI(tempStoryGroups, currentUserId)
                            }
                        }
                        .addOnFailureListener {
                            processedCount++
                            if (processedCount == userIds.size) {
                                updateStoriesUI(tempStoryGroups, currentUserId)
                            }
                        }
                }

                override fun onCancelled(error: DatabaseError) {
                    processedCount++
                    if (processedCount == userIds.size) {
                        updateStoriesUI(tempStoryGroups, currentUserId)
                    }
                }
            })
        }
    }

    private fun updateStoriesUI(tempStoryGroups: List<StoryGroup>, currentUserId: String) {
        val sorted = tempStoryGroups.sortedWith(compareBy(
            { it.userId != currentUserId },
            { !it.hasUnviewedStories },
            { it.username }
        ))

        storyGroups.clear()
        storyGroups.addAll(sorted)
        storiesAdapter.notifyDataSetChanged()
    }

    private fun loadPosts() {
        val currentUserId = auth.currentUser?.uid ?: return

        db.child("follows").child(currentUserId).child("following")
            .addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(followingSnapshot: DataSnapshot) {
                    val followingIds = followingSnapshot.children.mapNotNull { it.key }.toMutableList()
                    followingIds.add(currentUserId)
                    loadPostsForUsers(followingIds)
                }

                override fun onCancelled(error: DatabaseError) {}
            })
    }

    private fun loadPostsForUsers(userIds: List<String>) {
        posts.clear()
        postsAdapter.notifyDataSetChanged()

        val tempPosts = mutableListOf<Post>()
        var processedCount = 0

        if (userIds.isEmpty()) return

        for (userId in userIds) {
            db.child("userPosts").child(userId).addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    val postIds = snapshot.children.mapNotNull { it.key }

                    var userPostsProcessed = 0
                    if (postIds.isEmpty()) {
                        processedCount++
                        if (processedCount == userIds.size) {
                            updatePostsUI(tempPosts)
                        }
                        return
                    }

                    for (postId in postIds) {
                        db.child("posts").child(postId).get()
                            .addOnSuccessListener { postSnapshot ->
                                val post = postSnapshot.getValue(Post::class.java)
                                if (post != null) {
                                    synchronized(tempPosts) {
                                        // Only add if post doesn't already exist
                                        if (tempPosts.none { it.postId == post.postId }) {
                                            tempPosts.add(post)
                                        }
                                    }
                                }

                                userPostsProcessed++
                                if (userPostsProcessed == postIds.size) {
                                    processedCount++
                                    if (processedCount == userIds.size) {
                                        updatePostsUI(tempPosts)
                                    }
                                }
                            }
                            .addOnFailureListener {
                                userPostsProcessed++
                                if (userPostsProcessed == postIds.size) {
                                    processedCount++
                                    if (processedCount == userIds.size) {
                                        updatePostsUI(tempPosts)
                                    }
                                }
                            }
                    }
                }

                override fun onCancelled(error: DatabaseError) {
                    processedCount++
                    if (processedCount == userIds.size) {
                        updatePostsUI(tempPosts)
                    }
                }
            })
        }
    }

    private fun updatePostsUI(tempPosts: List<Post>) {
        // Remove duplicates by postId and sort by timestamp
        val uniquePosts = tempPosts.distinctBy { it.postId }
        val sorted = uniquePosts.sortedByDescending { it.timestamp }
        posts.clear()
        posts.addAll(sorted)
        postsAdapter.notifyDataSetChanged()
    }

    private fun toggleLike(post: Post) {
        val currentUserId = auth.currentUser?.uid ?: return

        val likesRef = db.child("posts").child(post.postId).child("likes")

        if (post.isLikedBy(currentUserId)) {
            // Unlike
            likesRef.child(currentUserId).removeValue()
                .addOnSuccessListener {
                    // Update only the specific post in the list
                    updateSinglePost(post.postId)
                }
        } else {
            // Like
            likesRef.child(currentUserId).setValue(true)
                .addOnSuccessListener {
                    // Update only the specific post in the list
                    updateSinglePost(post.postId)
                }
        }
    }

    private fun updateSinglePost(postId: String) {
        db.child("posts").child(postId).get()
            .addOnSuccessListener { snapshot ->
                val updatedPost = snapshot.getValue(Post::class.java)
                if (updatedPost != null) {
                    // Find the index of the post in the list
                    val index = posts.indexOfFirst { it.postId == postId }
                    if (index != -1) {
                        // Update only that specific post
                        posts[index] = updatedPost
                        postsAdapter.notifyItemChanged(index)
                    }
                }
            }
            .addOnFailureListener {
                Toast.makeText(requireContext(), "Failed to update post", Toast.LENGTH_SHORT).show()
            }
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
        val currentUserId = auth.currentUser?.uid ?: return

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
}