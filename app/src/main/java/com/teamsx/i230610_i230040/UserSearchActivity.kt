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
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.teamsx.i230610_i230040.network.Resource
import com.teamsx.i230610_i230040.network.UserRepository
import com.teamsx.i230610_i230040.utils.UserPreferences
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class UserSearchActivity : AppCompatActivity() {

    companion object {
        const val EXTRA_QUERY = "extra_query"
    }

    private lateinit var searchField: AutoCompleteTextView
    private lateinit var recycler: RecyclerView
    private lateinit var emptyView: TextView
    private lateinit var adapter: UsersAdapter

    private val userRepository = UserRepository()
    private val userPreferences by lazy { UserPreferences(this) }
    private val filtered = mutableListOf<UserRow>()
    private var searchJob: Job? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_user_search)

        searchField = findViewById(R.id.searchfield)
        recycler = findViewById(R.id.rvUsers)
        emptyView = findViewById(R.id.emptyText)

        adapter = UsersAdapter(filtered)
        recycler.layoutManager = LinearLayoutManager(this)
        recycler.adapter = adapter

        findViewById<ImageView>(R.id.btnBack)?.setOnClickListener { onBackPressedDispatcher.onBackPressed() }

        // Get initial query if passed
        val initialQuery = intent?.getStringExtra(EXTRA_QUERY).orEmpty()
        if (initialQuery.isNotEmpty()) {
            searchField.setText(initialQuery)
            performSearch(initialQuery)
        } else {
            emptyView.isVisible = true
            emptyView.text = "Search for people"
        }

        // Live search with debounce
        searchField.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable?) {}
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                val query = s?.toString().orEmpty()

                // Cancel previous search job
                searchJob?.cancel()

                if (query.isBlank()) {
                    filtered.clear()
                    adapter.notifyDataSetChanged()
                    emptyView.isVisible = true
                    emptyView.text = "Search for people"
                } else {
                    // Debounce search by 500ms
                    searchJob = lifecycleScope.launch {
                        delay(500)
                        performSearch(query)
                    }
                }
            }
        })

        searchField.setOnEditorActionListener { _, actionId, _ ->
            actionId == EditorInfo.IME_ACTION_SEARCH || actionId == EditorInfo.IME_ACTION_DONE
        }
    }

    private fun performSearch(query: String) {
        lifecycleScope.launch {
            val currentUid = userPreferences.getUser()?.uid ?: ""
            when (val result = userRepository.searchUsers(query, currentUid)) {
                is Resource.Success -> {
                    filtered.clear()
                    val users = result.data?.map { apiUser ->
                        UserRow(
                            uid = apiUser.uid,
                            username = apiUser.username,
                            photoBase64 = apiUser.profileImageUrl
                        )
                    } ?: emptyList()
                    filtered.addAll(users)

                    emptyView.isVisible = filtered.isEmpty()
                    emptyView.text = if (filtered.isEmpty()) "No accounts found" else ""
                    adapter.notifyDataSetChanged()
                }
                is Resource.Error -> {
                    filtered.clear()
                    adapter.notifyDataSetChanged()
                    emptyView.isVisible = true
                    emptyView.text = result.message ?: "Failed to search users"
                }
                is Resource.Loading -> {
                    // Show loading state if needed
                }
            }
        }
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
