package com.teamsx.i230610_i230040

import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.widget.EditText
import android.widget.ImageView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.teamsx.i230610_i230040.utils.UserPreferences
import com.teamsx.i230610_i230040.repository.UserRepository
import com.teamsx.i230610_i230040.utils.NetworkMonitor
import kotlinx.coroutines.launch

class socialhomescreen4 : AppCompatActivity() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var searchField: EditText
    private val userPreferences by lazy { UserPreferences(this) }
    private lateinit var userRepository: UserRepository
    private lateinit var networkMonitor: NetworkMonitor
    private val usersList = mutableListOf<Pair<String, UserProfile>>() // Pair<uid, UserProfile>
    private val allUsers = mutableListOf<Pair<String, UserProfile>>() // All users for search filtering

    // Class-level variable for logged-in user's UID
    private val currentUserId: String by lazy { userPreferences.getUser()?.uid ?: "" }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_socialhomescreen4)

        // Initialize offline support
        userRepository = UserRepository(this)
        networkMonitor = NetworkMonitor(this)

        recyclerView = findViewById(R.id.userRecyclerView)
        searchField = findViewById(R.id.searchField)
        recyclerView.layoutManager = LinearLayoutManager(this)

        val back = findViewById<ImageView>(R.id.leftarrowlogo)
        back.setOnClickListener {
            val intent = Intent(this, HomeActivity::class.java).apply {
                putExtra(HomeActivity.EXTRA_START_DEST, R.id.nav_home)
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            }
            startActivity(intent)
            finish()
        }

        setupSearch()
        loadUsers()
    }

    private fun setupSearch() {
        searchField.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                filterUsers(s.toString())
            }

            override fun afterTextChanged(s: Editable?) {}
        })
    }

    private fun filterUsers(query: String) {
        val filteredList = if (query.isEmpty()) {
            allUsers
        } else {
            allUsers.filter { (_, profile) ->
                profile.username.contains(query, ignoreCase = true)
            }
        }
        updateRecyclerView(filteredList)
    }

    private fun loadUsers() {
        if (currentUserId.isEmpty()) {
            Log.e("socialhomescreen4", "Current user ID is empty")
            Toast.makeText(this, "Please log in first", Toast.LENGTH_SHORT).show()
            return
        }

        lifecycleScope.launch {
            try {
                // First, load from cache immediately (offline-first)
                val cachedUsers = userRepository.getAllUsers()
                if (cachedUsers.isNotEmpty()) {
                    Log.d("socialhomescreen4", "Loaded ${cachedUsers.size} users from cache")
                    displayUsers(cachedUsers.map { user ->
                        Pair(user.uid, UserProfile(
                            username = user.username,
                            firstName = user.fullName,
                            photoBase64 = user.profileImageUrl
                        ))
                    })
                }

                // Then fetch fresh data from server and update cache
                val freshUsers = userRepository.fetchChatUsersFromServer(currentUserId)

                if (freshUsers.isNotEmpty()) {
                    Log.d("socialhomescreen4", "Loaded ${freshUsers.size} users from server")
                    displayUsers(freshUsers.map { user ->
                        Pair(user.uid, UserProfile(
                            username = user.username,
                            firstName = user.fullName,
                            photoBase64 = user.profileImageUrl
                        ))
                    })
                } else if (cachedUsers.isEmpty()) {
                    Toast.makeText(this@socialhomescreen4, "No mutual followers found. Follow users and have them follow you back to chat.", Toast.LENGTH_LONG).show()
                }

            } catch (e: Exception) {
                Log.e("socialhomescreen4", "Exception loading users", e)

                // Try to load from cache on error
                val cachedUsers = userRepository.getAllUsers()
                if (cachedUsers.isNotEmpty()) {
                    Log.d("socialhomescreen4", "Loaded ${cachedUsers.size} users from cache after error")
                    displayUsers(cachedUsers.map { user ->
                        Pair(user.uid, UserProfile(
                            username = user.username,
                            firstName = user.fullName,
                            photoBase64 = user.profileImageUrl
                        ))
                    })
                    Toast.makeText(this@socialhomescreen4, "Offline mode: Showing cached users", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(this@socialhomescreen4, "Network error: ${e.localizedMessage}", Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    private fun displayUsers(users: List<Pair<String, UserProfile>>) {
        usersList.clear()
        allUsers.clear()

        usersList.addAll(users)

        // Sort users alphabetically by username
        val sortedUsers = usersList.sortedBy { it.second.username.lowercase() }
        allUsers.addAll(sortedUsers)

        Log.d("socialhomescreen4", "Displaying ${allUsers.size} users")
        updateRecyclerView(allUsers)
    }

    private fun updateRecyclerView(users: List<Pair<String, UserProfile>>) {
        recyclerView.adapter = UserAdapter(users) { clickedUid, username ->
            // Create chat ID by sorting user IDs alphabetically for consistency
            val chatId = if (currentUserId < clickedUid) "$currentUserId$clickedUid" else "$clickedUid$currentUserId"
            val intent = Intent(this@socialhomescreen4, socialhomescreenchat::class.java)
            intent.putExtra("chatId", chatId)
            intent.putExtra("otherUserName", username)
            intent.putExtra("otherUserId", clickedUid)
            startActivity(intent)
        }
    }
}
