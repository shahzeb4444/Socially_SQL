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
import com.teamsx.i230610_i230040.network.RetrofitInstance
import com.teamsx.i230610_i230040.network.GetChatUsersRequest
import kotlinx.coroutines.launch

class socialhomescreen4 : AppCompatActivity() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var searchField: EditText
    private val userPreferences by lazy { UserPreferences(this) }
    private val usersList = mutableListOf<Pair<String, UserProfile>>() // Pair<uid, UserProfile>
    private val allUsers = mutableListOf<Pair<String, UserProfile>>() // All users for search filtering

    // Class-level variable for logged-in user's UID
    private val currentUserId: String by lazy { userPreferences.getUser()?.uid ?: "" }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_socialhomescreen4)


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
                val request = GetChatUsersRequest(currentUserId)
                Log.d("socialhomescreen4", "Loading chat users for: $currentUserId")
                Log.d("socialhomescreen4", "Request: $request")

                val response = RetrofitInstance.apiService.getChatUsers(request)

                Log.d("socialhomescreen4", "Response code: ${response.code()}")
                Log.d("socialhomescreen4", "Response successful: ${response.isSuccessful}")
                Log.d("socialhomescreen4", "Response body: ${response.body()}")
                Log.d("socialhomescreen4", "Error body: ${response.errorBody()?.string()}")

                if (response.isSuccessful && response.body()?.success == true) {
                    val apiUsers = response.body()?.data?.users ?: emptyList()

                    Log.d("socialhomescreen4", "Loaded ${apiUsers.size} chat users")

                    usersList.clear()
                    allUsers.clear()

                    // Convert API users to UserProfile pairs
                    apiUsers.forEach { apiUser ->
                        Log.d("socialhomescreen4", "Processing user: ${apiUser.username}")
                        val profile = UserProfile(
                            username = apiUser.username,
                            firstName = apiUser.fullName ?: "",
                            photoBase64 = apiUser.profileImageUrl
                        )
                        usersList.add(Pair(apiUser.uid, profile))
                    }

                    // Sort users alphabetically by username
                    val sortedUsers = usersList.sortedBy { it.second.username.lowercase() }
                    allUsers.addAll(sortedUsers)

                    Log.d("socialhomescreen4", "Updating RecyclerView with ${allUsers.size} users")
                    updateRecyclerView(allUsers)

                    if (allUsers.isEmpty()) {
                        Toast.makeText(this@socialhomescreen4, "No mutual followers found. Follow users and have them follow you back to chat.", Toast.LENGTH_LONG).show()
                    }
                } else {
                    val errorMsg = response.body()?.error ?: response.errorBody()?.string() ?: "Failed to load users (HTTP ${response.code()})"
                    Log.e("socialhomescreen4", "Error: $errorMsg")
                    Toast.makeText(this@socialhomescreen4, "Error: $errorMsg", Toast.LENGTH_LONG).show()
                }
            } catch (e: Exception) {
                Log.e("socialhomescreen4", "Exception loading users", e)
                e.printStackTrace()
                Toast.makeText(this@socialhomescreen4, "Network error: ${e.localizedMessage}", Toast.LENGTH_LONG).show()
            }
        }
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
