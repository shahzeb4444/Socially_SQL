package com.teamsx.i230610_i230040

import android.graphics.BitmapFactory
import android.util.Base64
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import androidx.viewpager2.widget.ViewPager2
import com.google.android.material.imageview.ShapeableImageView

class PostsAdapter(
    private val posts: List<Post>,
    private val currentUserId: String,
    private val onLikeClick: (Post) -> Unit,
    private val onCommentClick: (Post) -> Unit,
    private val onPostClick: (Post) -> Unit,
    private val onProfileClick: (String, String) -> Unit
) : RecyclerView.Adapter<PostsAdapter.PostViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PostViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_post, parent, false)
        return PostViewHolder(view)
    }

    override fun onBindViewHolder(holder: PostViewHolder, position: Int) {
        holder.bind(posts[position], currentUserId, onLikeClick, onCommentClick, onPostClick, onProfileClick)
    }

    override fun getItemCount(): Int = posts.size

    class PostViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val userProfileImage: ShapeableImageView = itemView.findViewById(R.id.userProfileImage)
        private val username: TextView = itemView.findViewById(R.id.username)
        private val location: TextView = itemView.findViewById(R.id.location)
        private val verifiedBadge: ImageView = itemView.findViewById(R.id.verifiedBadge)
        private val imagesViewPager: ViewPager2 = itemView.findViewById(R.id.imagesViewPager)
        private val imageIndicator: TextView = itemView.findViewById(R.id.imageIndicator)
        private val btnLike: ImageView = itemView.findViewById(R.id.btnLike)
        private val btnComment: ImageView = itemView.findViewById(R.id.btnComment)
        private val btnSend: ImageView = itemView.findViewById(R.id.btnSend)
        private val btnSave: ImageView = itemView.findViewById(R.id.btnSave)
        private val likesCount: TextView = itemView.findViewById(R.id.likesCount)
        private val description: TextView = itemView.findViewById(R.id.description)
        private val timeAgo: TextView = itemView.findViewById(R.id.timeAgo)

        fun bind(
            post: Post,
            currentUserId: String,
            onLikeClick: (Post) -> Unit,
            onCommentClick: (Post) -> Unit,
            onPostClick: (Post) -> Unit,
            onProfileClick: (String, String) -> Unit
        ) {
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
            updateLikeButton(post.isLikedBy(currentUserId))

            // Set likes count
            updateLikesCount(post)

            // Set description
            val descriptionText = "${post.username} ${post.description}"
            description.text = descriptionText

            // Set time ago
            timeAgo.text = PostUtils.getTimeAgo(post.timestamp)

            // Click listeners
            btnLike.setOnClickListener {
                onLikeClick(post)
            }

            btnComment.setOnClickListener {
                onCommentClick(post)
            }

            imagesViewPager.setOnClickListener {
                onPostClick(post)
            }

            userProfileImage.setOnClickListener {
                onProfileClick(post.userId, post.username)
            }

            username.setOnClickListener {
                onProfileClick(post.userId, post.username)
            }
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

        private fun updateLikesCount(post: Post) {
            val count = post.getTotalLikes()
            likesCount.text = when {
                count == 0 -> "Be the first to like this"
                count == 1 -> "1 like"
                else -> "$count likes"
            }
        }
    }
}