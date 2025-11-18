package com.teamsx.i230610_i230040

import android.graphics.BitmapFactory
import android.util.Base64
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.imageview.ShapeableImageView

class StoriesAdapter(
    private val stories: List<StoryGroup>,
    private val currentUserId: String,
    private val onStoryClick: (StoryGroup) -> Unit,
    private val onAddStoryClick: () -> Unit
) : RecyclerView.Adapter<StoriesAdapter.StoryViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): StoryViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_story, parent, false)
        return StoryViewHolder(view)
    }

    override fun onBindViewHolder(holder: StoryViewHolder, position: Int) {
        val storyGroup = stories[position]
        holder.bind(storyGroup, currentUserId, onStoryClick, onAddStoryClick)
    }

    override fun getItemCount(): Int = stories.size

    class StoryViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val userProfileImage: ShapeableImageView = itemView.findViewById(R.id.userProfileImage)
        private val storyRing: ImageView = itemView.findViewById(R.id.storyRing)
        private val username: TextView = itemView.findViewById(R.id.username)
        private val addStoryIcon: ImageView = itemView.findViewById(R.id.addStoryIcon)

        fun bind(
            storyGroup: StoryGroup,
            currentUserId: String,
            onStoryClick: (StoryGroup) -> Unit,
            onAddStoryClick: () -> Unit
        ) {
            // Set username
            username.text = if (storyGroup.userId == currentUserId) "Your Story" else storyGroup.username

            // Load profile image
            val photoBase64 = storyGroup.userPhotoBase64
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

            // Show story ring based on viewed status
            if (storyGroup.hasUnviewedStories) {
                storyRing.setImageResource(R.drawable.colorfulstoryoutline)
            } else {
                storyRing.setImageResource(R.drawable.storiesoutlineoval)
            }

            // Show add icon only for current user with no stories
            if (storyGroup.userId == currentUserId && storyGroup.stories.isEmpty()) {
                addStoryIcon.visibility = View.VISIBLE
                storyRing.visibility = View.GONE
                itemView.setOnClickListener { onAddStoryClick() }
            } else {
                addStoryIcon.visibility = View.GONE
                storyRing.visibility = View.VISIBLE
                itemView.setOnClickListener { onStoryClick(storyGroup) }
            }
        }
    }
}