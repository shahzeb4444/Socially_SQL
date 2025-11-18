package com.teamsx.i230610_i230040

import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.widget.EditText
import android.widget.ImageView
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*

class socialhomescreen4 : AppCompatActivity() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var searchField: EditText
    private lateinit var database: DatabaseReference
    private val usersList = mutableListOf<Pair<String, UserProfile>>() // Pair<uid, UserProfile>
    private val allUsers = mutableListOf<Pair<String, UserProfile>>() // All users for search filtering

    // Class-level variable for logged-in user's UID
    private val currentUserId: String by lazy { FirebaseAuth.getInstance().currentUser?.uid ?: "" }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_socialhomescreen4)

        database = FirebaseDatabase.getInstance().reference.child("users")
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
        if (currentUserId.isEmpty()) return  // safety check

        database.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                usersList.clear()
                allUsers.clear()
                val addedUserIds = mutableSetOf<String>() // Track added users to prevent duplicates

                for (userSnap in snapshot.children) {
                    val uid = userSnap.key ?: continue
                    if (uid == currentUserId) continue // skip self
                    if (addedUserIds.contains(uid)) continue // skip duplicates

                    val profile = userSnap.getValue(UserProfile::class.java) ?: continue

                    // Skip users with empty or blank usernames
                    if (profile.username.isBlank()) continue

                    usersList.add(Pair(uid, profile))
                    addedUserIds.add(uid)
                }

                // Sort users alphabetically by username for better UX
                val sortedUsers = usersList.sortedBy { it.second.username.lowercase() }
                allUsers.addAll(sortedUsers)

                updateRecyclerView(allUsers)
            }

            override fun onCancelled(error: DatabaseError) {}
        })
    }

    private fun updateRecyclerView(users: List<Pair<String, UserProfile>>) {
        recyclerView.adapter = UserAdapter(users) { clickedUid, username ->
            // now we can safely use currentUserId and clickedUid
            val chatId = if (currentUserId < clickedUid) "$currentUserId$clickedUid" else "$clickedUid$currentUserId"
            val intent = Intent(this@socialhomescreen4, socialhomescreenchat::class.java)
            intent.putExtra("chatId", chatId)
            intent.putExtra("otherUserName", username)
            startActivity(intent)
        }
    }
}
