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
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.isVisible
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.imageview.ShapeableImageView
import com.teamsx.i230610_i230040.network.Resource
import com.teamsx.i230610_i230040.utils.UserPreferences
import com.teamsx.i230610_i230040.viewmodels.FollowViewModel

class FollowListActivity : AppCompatActivity() {

    companion object {
        const val EXTRA_MODE = "mode"
        const val MODE_FOLLOWERS = "followers"
        const val MODE_FOLLOWING = "following"
    }

    private val followViewModel: FollowViewModel by viewModels()
    private val userPreferences by lazy { UserPreferences(this) }

    private lateinit var title: TextView
    private lateinit var searchField: AutoCompleteTextView
    private lateinit var recycler: RecyclerView
    private lateinit var emptyView: TextView
    private lateinit var adapter: FollowUsersAdapter

    private val all = mutableListOf<RowUser>()      // full list loaded
    private val filtered = mutableListOf<RowUser>() // filtered for display
    private var mode: String = MODE_FOLLOWERS

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_follow_list)

        mode = intent.getStringExtra(EXTRA_MODE) ?: MODE_FOLLOWERS

        title = findViewById(R.id.title)
        title.text = if (mode == MODE_FOLLOWERS) "Followers" else "Following"

        searchField = findViewById(R.id.searchfield)
        recycler = findViewById(R.id.rvUsers)
        emptyView = findViewById(R.id.emptyText)
        findViewById<ImageView>(R.id.btnBack).setOnClickListener { onBackPressedDispatcher.onBackPressed() }

        adapter = FollowUsersAdapter(filtered)
        recycler.layoutManager = LinearLayoutManager(this)
        recycler.adapter = adapter

        // typing filter
        searchField.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable?) {}
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                applyFilter(s?.toString().orEmpty())
            }
        })
        searchField.setOnEditorActionListener { _, actionId, _ ->
            actionId == EditorInfo.IME_ACTION_SEARCH || actionId == EditorInfo.IME_ACTION_DONE
        }

        setupObservers()
        loadList()
    }

    private fun setupObservers() {
        // Observe followers
        followViewModel.followers.observe(this) { resource ->
            when (resource) {
                is Resource.Success -> {
                    if (mode == MODE_FOLLOWERS) {
                        all.clear()
                        resource.data?.forEach { apiUser ->
                            all.add(RowUser(apiUser.uid, apiUser.username, apiUser.profileImageUrl))
                        }
                        applyFilter(searchField.text?.toString().orEmpty())
                    }
                }
                is Resource.Error -> {
                    emptyView.isVisible = true
                    emptyView.text = resource.message ?: "Failed to load followers"
                }
                is Resource.Loading -> {
                    // Show loading if needed
                }
                else -> {}
            }
        }

        // Observe following
        followViewModel.following.observe(this) { resource ->
            when (resource) {
                is Resource.Success -> {
                    if (mode == MODE_FOLLOWING) {
                        all.clear()
                        resource.data?.forEach { apiUser ->
                            all.add(RowUser(apiUser.uid, apiUser.username, apiUser.profileImageUrl))
                        }
                        applyFilter(searchField.text?.toString().orEmpty())
                    }
                }
                is Resource.Error -> {
                    emptyView.isVisible = true
                    emptyView.text = resource.message ?: "Failed to load following"
                }
                is Resource.Loading -> {
                    // Show loading if needed
                }
                else -> {}
            }
        }
    }

    private fun loadList() {
        val uid = userPreferences.getUser()?.uid ?: return

        if (mode == MODE_FOLLOWERS) {
            followViewModel.getFollowers(uid)
        } else {
            followViewModel.getFollowing(uid)
        }
    }

    private fun applyFilter(query: String) {
        filtered.clear()
        if (query.isBlank()) {
            filtered.addAll(all)
        } else {
            val q = query.trim().lowercase()
            filtered.addAll(all.filter { it.username.lowercase().contains(q) })
        }
        emptyView.isVisible = filtered.isEmpty()
        if (filtered.isEmpty()) {
            emptyView.text = if (all.isEmpty()) {
                "No ${if (mode == MODE_FOLLOWERS) "followers" else "following"} yet"
            } else {
                "No results"
            }
        }
        adapter.notifyDataSetChanged()
    }
}

/* --- row model --- */
data class RowUser(
    val uid: String,
    val username: String,
    val photoBase64: String?
)

/* --- adapter --- */
private class FollowUsersAdapter(
    private val data: List<RowUser>
) : RecyclerView.Adapter<FUserVH>() {

    override fun onCreateViewHolder(parent: android.view.ViewGroup, viewType: Int): FUserVH {
        val v = android.view.LayoutInflater.from(parent.context)
            .inflate(R.layout.item_follow_user, parent, false)
        return FUserVH(v)
    }

    override fun onBindViewHolder(holder: FUserVH, position: Int) {
        holder.bind(data[position])
    }

    override fun getItemCount(): Int = data.size
}

/* --- view holder --- */
private class FUserVH(itemView: android.view.View) : RecyclerView.ViewHolder(itemView) {
    private val avatar = itemView.findViewById<ShapeableImageView>(R.id.imgAvatar)
    private val name = itemView.findViewById<TextView>(R.id.txtUsername)

    fun bind(row: RowUser) {
        name.text = row.username

        // --- avatar load (circular via ShapeableImageView + CircleImage style) ---
        val b64 = row.photoBase64
        if (!b64.isNullOrEmpty()) {
            runCatching {
                val bytes = Base64.decode(b64, Base64.DEFAULT)
                val bmp = BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
                avatar.setImageBitmap(bmp) // centerCrop + CircleImage makes it circular
            }.onFailure {
                avatar.setImageResource(R.drawable.profile_login_splash_small)
            }
        } else {
            avatar.setImageResource(R.drawable.profile_login_splash_small)
        }

        // open OtherUserProfileFragment via HomeActivity (same pattern you use elsewhere)
        itemView.setOnClickListener {
            val ctx = itemView.context
            val intent = Intent(ctx, HomeActivity::class.java).apply {
                putExtra("open_user_profile", true)
                putExtra("user_id", row.uid)
                putExtra("username", row.username)
            }
            ctx.startActivity(intent)
        }
    }
}
