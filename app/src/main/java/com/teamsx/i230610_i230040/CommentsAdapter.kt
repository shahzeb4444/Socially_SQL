package com.teamsx.i230610_i230040

import android.graphics.BitmapFactory
import android.util.Base64
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.imageview.ShapeableImageView

class CommentsAdapter(
    private val comments: List<Comment>
) : RecyclerView.Adapter<CommentsAdapter.CommentViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CommentViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_comment, parent, false)
        return CommentViewHolder(view)
    }

    override fun onBindViewHolder(holder: CommentViewHolder, position: Int) {
        holder.bind(comments[position])
    }

    override fun getItemCount(): Int = comments.size

    class CommentViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val userProfileImage: ShapeableImageView = itemView.findViewById(R.id.userProfileImage)
        private val username: TextView = itemView.findViewById(R.id.username)
        private val timeAgo: TextView = itemView.findViewById(R.id.timeAgo)
        private val message: TextView = itemView.findViewById(R.id.message)

        fun bind(comment: Comment) {
            username.text = comment.username
            message.text = comment.message
            timeAgo.text = PostUtils.getTimeAgo(comment.timestamp)

            // Load profile image
            if (!comment.userPhotoBase64.isNullOrEmpty()) {
                try {
                    val bytes = Base64.decode(comment.userPhotoBase64, Base64.DEFAULT)
                    val bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
                    userProfileImage.setImageBitmap(bitmap)
                } catch (e: Exception) {
                    userProfileImage.setImageResource(R.drawable.profile_login_splash_small)
                }
            } else {
                userProfileImage.setImageResource(R.drawable.profile_login_splash_small)
            }
        }
    }
}