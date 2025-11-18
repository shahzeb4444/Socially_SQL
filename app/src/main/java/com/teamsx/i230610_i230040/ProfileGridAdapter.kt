package com.teamsx.i230610_i230040

import android.graphics.BitmapFactory
import android.util.Base64
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.recyclerview.widget.RecyclerView

class ProfileGridAdapter(
    private val posts: List<Post>,
    private val onPostClick: (Post) -> Unit
) : RecyclerView.Adapter<ProfileGridAdapter.GridViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): GridViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_profile_grid_post, parent, false)
        return GridViewHolder(view)
    }

    override fun onBindViewHolder(holder: GridViewHolder, position: Int) {
        holder.bind(posts[position], onPostClick)
    }

    override fun getItemCount(): Int = posts.size

    class GridViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val postImage: ImageView = itemView.findViewById(R.id.postImage)
        private val multiImageIndicator: ImageView = itemView.findViewById(R.id.multiImageIndicator)

        fun bind(post: Post, onPostClick: (Post) -> Unit) {
            // Load first image
            val firstImage = post.getFirstImage()
            if (firstImage.isNotEmpty()) {
                try {
                    val bytes = Base64.decode(firstImage, Base64.DEFAULT)
                    val bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
                    postImage.setImageBitmap(bitmap)
                } catch (e: Exception) {
                    postImage.setImageResource(R.drawable.profile_login_splash_small)
                }
            }

            // Show multi-image indicator if post has more than 1 image
            if (post.images.size > 1) {
                multiImageIndicator.visibility = View.VISIBLE
            } else {
                multiImageIndicator.visibility = View.GONE
            }

            itemView.setOnClickListener {
                onPostClick(post)
            }
        }
    }
}