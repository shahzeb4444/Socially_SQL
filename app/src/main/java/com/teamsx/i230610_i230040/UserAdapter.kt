package com.teamsx.i230610_i230040

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.BitmapShader
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Shader
import android.util.Base64
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.RelativeLayout
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

/**
 * Adapter for displaying a list of users in socialhomescreen4.
 * Uses UserProfile to fetch username, and UID is stored separately for chat.
 */
class UserAdapter(
    private val users: List<Pair<String, UserProfile>>, // Pair<uid, UserProfile>
    private val onClick: (uid: String, username: String) -> Unit
) : RecyclerView.Adapter<UserAdapter.UserViewHolder>() {

    class UserViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val container: RelativeLayout = itemView.findViewById(R.id.r1) // your RelativeLayout in item layout
        val usernameText: TextView = itemView.findViewById(R.id.boldtext1) // username TextView
        val profileImage: ImageView = itemView.findViewById(R.id.profileImage) // profile ImageView
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): UserViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_user_dynamic, parent, false)
        return UserViewHolder(view)
    }

    override fun onBindViewHolder(holder: UserViewHolder, position: Int) {
        val (uid, profile) = users[position]
        holder.usernameText.text = profile.username

        // Load profile image
        if (!profile.photoBase64.isNullOrEmpty()) {
            try {
                val decodedBytes = Base64.decode(profile.photoBase64, Base64.DEFAULT)
                val bitmap = BitmapFactory.decodeByteArray(decodedBytes, 0, decodedBytes.size)
                val circularBitmap = getCircularBitmap(bitmap)
                holder.profileImage.setImageBitmap(circularBitmap)
            } catch (_: Exception) {
                // If decoding fails, use default image
                holder.profileImage.setImageResource(R.drawable.small_icon)
            }
        } else {
            // No photo, use default
            holder.profileImage.setImageResource(R.drawable.small_icon)
        }

        holder.container.setOnClickListener {
            onClick(uid, profile.username)
        }
    }

    override fun getItemCount(): Int = users.size

    /**
     * Converts a bitmap to a circular bitmap
     */
    private fun getCircularBitmap(bitmap: Bitmap): Bitmap {
        val size = Math.min(bitmap.width, bitmap.height)
        val output = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)

        val canvas = Canvas(output)
        val paint = Paint()
        paint.isAntiAlias = true
        paint.shader = BitmapShader(bitmap, Shader.TileMode.CLAMP, Shader.TileMode.CLAMP)

        val radius = size / 2f
        canvas.drawCircle(radius, radius, radius, paint)

        return output
    }
}
