package com.teamsx.i230610_i230040

import android.content.Intent
import android.graphics.BitmapFactory
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.util.Base64
import android.view.inputmethod.EditorInfo
import android.widget.AutoCompleteTextView
import android.widget.ImageView
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.isVisible
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.database.*

class UserSearchActivity : AppCompatActivity() {

    companion object {
        const val EXTRA_QUERY = "extra_query"
    }

    private lateinit var searchField: AutoCompleteTextView
    private lateinit var recycler: RecyclerView
    private lateinit var emptyView: TextView
    private lateinit var adapter: UsersAdapter

    private lateinit var dbRef: DatabaseReference
    private val allUsers = mutableListOf<UserRow>()   // full list loaded once
    private val filtered = mutableListOf<UserRow>()   // filtered list shown

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_user_search)

        searchField = findViewById(R.id.searchfield)    // keep same id name you used
        recycler = findViewById(R.id.rvUsers)
        emptyView = findViewById(R.id.emptyText)

        adapter = UsersAdapter(filtered)
        recycler.layoutManager = LinearLayoutManager(this)
        recycler.adapter = adapter

        // Optional back action
        findViewById<ImageView>(R.id.btnBack)?.setOnClickListener { onBackPressedDispatcher.onBackPressed() }

        dbRef = FirebaseDatabase.getInstance().getReference("users")

        // 1) Load once
        dbRef.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                allUsers.clear()
                for (child in snapshot.children) {
                    val uid = child.key ?: continue
                    val username = child.child("username").getValue(String::class.java) ?: continue
                    val b64 = child.child("photoBase64").getValue(String::class.java)
                    allUsers.add(UserRow(uid, username, b64))
                }
                // initial filter using incoming query (if any)
                val initial = intent?.getStringExtra(EXTRA_QUERY).orEmpty()
                searchField.setText(initial)
                applyFilter(initial)
            }
            override fun onCancelled(error: DatabaseError) {
                emptyView.isVisible = true
                emptyView.text = "Failed to load users."
            }
        })

        // 2) Filter live as user types
        searchField.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable?) {}
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                applyFilter(s?.toString().orEmpty())
            }
        })

        // Optional: hide keyboard action ‘search’
        searchField.setOnEditorActionListener { _, actionId, _ ->
            actionId == EditorInfo.IME_ACTION_SEARCH || actionId == EditorInfo.IME_ACTION_DONE
        }
    }

    private fun applyFilter(query: String) {
        filtered.clear()
        if (query.isBlank()) {
            // Show nothing until user types (Instagram-like)
            emptyView.isVisible = true
            emptyView.text = "Search for people"
        } else {
            val q = query.trim().lowercase()
            filtered.addAll(allUsers.filter { it.username.lowercase().contains(q) })
            emptyView.isVisible = filtered.isEmpty()
            emptyView.text = if (filtered.isEmpty()) "No accounts found" else ""
        }
        adapter.notifyDataSetChanged()
    }
}

// --- simple row model used by adapter ---
data class UserRow(
    val uid: String,
    val username: String,
    val photoBase64: String?
)

// --- RecyclerView adapter ---
private class UsersAdapter(
    private val data: List<UserRow>
) : RecyclerView.Adapter<UserVH>() {

    override fun onCreateViewHolder(parent: android.view.ViewGroup, viewType: Int): UserVH {
        val v = android.view.LayoutInflater.from(parent.context)
            .inflate(R.layout.item_user_row, parent, false)
        return UserVH(v)
    }

    override fun onBindViewHolder(holder: UserVH, position: Int) {
        holder.bind(data[position])
    }

    override fun getItemCount(): Int = data.size
}

private class UserVH(itemView: android.view.View) : RecyclerView.ViewHolder(itemView) {
    private val avatar = itemView.findViewById<com.google.android.material.imageview.ShapeableImageView>(R.id.imgAvatar)
    private val name = itemView.findViewById<TextView>(R.id.txtUsername)

    fun bind(row: UserRow) {
        name.text = row.username

        // Base64 → Bitmap (safe-guarded)
        val b64 = row.photoBase64
        if (!b64.isNullOrEmpty()) {
            try {
                val bytes = Base64.decode(b64, Base64.DEFAULT)
                val bmp = BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
                avatar.setImageBitmap(bmp)
            } catch (_: Exception) {
                avatar.setImageResource(R.drawable.profile_login_splash_small) // placeholder
            }
        } else {
            avatar.setImageResource(R.drawable.profile_login_splash_small) // placeholder
        }

        // Click to open user profile in HomeActivity
        itemView.setOnClickListener {
            val context = itemView.context
            val intent = Intent(context, HomeActivity::class.java).apply {
                putExtra("open_user_profile", true)
                putExtra("user_id", row.uid)
                putExtra("username", row.username)
                flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
            }
            context.startActivity(intent)
        }
    }
}
