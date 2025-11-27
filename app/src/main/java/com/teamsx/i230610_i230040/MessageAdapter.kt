package com.teamsx.i230610_i230040

import android.graphics.BitmapFactory
import android.util.Base64
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.PopupMenu
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.textview.MaterialTextView
import java.text.SimpleDateFormat
import java.util.*

class MessageAdapter(
    private val messages: List<Message>,
    private val currentUserId: String,
    private val onMessageAction: (Message, String) -> Unit
) : RecyclerView.Adapter<MessageAdapter.VH>() {

    private companion object {
        const val SENT_TEXT = 1
        const val SENT_MEDIA = 2
        const val RECV_TEXT = 3
        const val RECV_MEDIA = 4
    }

    override fun getItemViewType(position: Int): Int {
        val m = messages[position]
        val mine = m.senderId == currentUserId
        val hasMedia = m.mediaType.isNotEmpty() && m.mediaUrl.isNotEmpty()
        return when {
            mine && hasMedia -> SENT_MEDIA
            mine && !hasMedia -> SENT_TEXT
            !mine && hasMedia -> RECV_MEDIA
            else -> RECV_TEXT
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val layoutId = when (viewType) {
            SENT_MEDIA -> R.layout.item_message_sent_media
            SENT_TEXT  -> R.layout.item_message_sent
            RECV_MEDIA -> R.layout.item_message_received_media
            else       -> R.layout.item_message_received
        }
        val v = LayoutInflater.from(parent.context).inflate(layoutId, parent, false)
        return VH(v, onMessageAction)
    }

    override fun onBindViewHolder(holder: VH, position: Int) {
        holder.bind(messages[position])
    }

    override fun getItemCount() = messages.size

    class VH(
        itemView: View,
        private val onMessageAction: (Message, String) -> Unit
    ) : RecyclerView.ViewHolder(itemView) {

        private val messageText: MaterialTextView? = itemView.findViewById(R.id.messageText)
        private val timestampText: MaterialTextView? = itemView.findViewById(R.id.timestampText)
        private val messageContainer: FrameLayout? = itemView.findViewById(R.id.messageContainer)

        // present only in *_media layouts (will be null for text-only rows)
        private val mediaContainer: FrameLayout? = itemView.findViewById(R.id.mediaContainer)
        private val mediaImageView: ImageView? = itemView.findViewById(R.id.mediaImageView)
        private val mediaCaptionText: MaterialTextView? = itemView.findViewById(R.id.mediaCaptionText)

        fun bind(m: Message) {
            val isMedia = m.mediaType.isNotEmpty() && m.mediaUrl.isNotEmpty()

            if (isMedia) {
                // hide text, show media
                messageText?.visibility = View.GONE
                mediaContainer?.visibility = View.VISIBLE

                // decode Base64 into Bitmap
                val bmp = decodeBase64(m.mediaUrl)
                if (bmp != null) {
                    mediaImageView?.setImageBitmap(bmp)
                } else {
                    // optional: show a placeholder
                    mediaImageView?.setImageResource(android.R.drawable.ic_menu_report_image)
                }

                if (m.mediaCaption.isNotEmpty()) {
                    mediaCaptionText?.text = m.mediaCaption
                    mediaCaptionText?.visibility = View.VISIBLE
                } else {
                    mediaCaptionText?.visibility = View.GONE
                }
            } else {
                // text message
                messageText?.text = m.text
                messageText?.visibility = View.VISIBLE
                mediaContainer?.visibility = View.GONE
            }

            val edited = if (m.isEdited) " (edited)" else ""
            val vanishIndicator = if (m.isVanishMode) " 👻" else ""
            timestampText?.text = formatTime(m.timestamp) + edited + vanishIndicator

            val longClickTarget: View = mediaContainer ?: messageContainer ?: itemView
            longClickTarget.setOnLongClickListener {
                // 5-min edit/delete window and not deleted
                val minutes = (System.currentTimeMillis() - m.timestamp) / 1000 / 60
                if (minutes <= 5 && !m.isDeleted) {
                    showMenu(it, m, canEdit = !isMedia) // text-only messages can be edited
                    true
                } else false
            }
        }

        private fun showMenu(anchor: View, m: Message, canEdit: Boolean) {
            val popup = PopupMenu(anchor.context, anchor)
            popup.menuInflater.inflate(R.menu.message_menu, popup.menu)
            // Hide "Edit" for media messages
            if (!canEdit) popup.menu.findItem(R.id.edit_message)?.isVisible = false

            popup.setOnMenuItemClickListener { item ->
                when (item.itemId) {
                    R.id.edit_message -> { onMessageAction(m, "EDIT"); true }
                    R.id.delete_message -> { onMessageAction(m, "DELETE"); true }
                    else -> false
                }
            }
            popup.show()
        }

        private fun decodeBase64(b64: String): android.graphics.Bitmap? {
            return try {
                val bytes = Base64.decode(b64, Base64.DEFAULT)
                BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
            } catch (_: Exception) {
                null
            }
        }

        private fun formatTime(ts: Long): String {
            val sdf = SimpleDateFormat("HH:mm", Locale.getDefault())
            return sdf.format(Date(ts))
        }
    }
}
