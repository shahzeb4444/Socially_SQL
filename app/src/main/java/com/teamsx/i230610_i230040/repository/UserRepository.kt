package com.teamsx.i230610_i230040.repository

import android.content.Context
import androidx.lifecycle.LiveData
import com.teamsx.i230610_i230040.database.AppDatabase
import com.teamsx.i230610_i230040.database.entity.UserEntity
import com.teamsx.i230610_i230040.network.*
import com.teamsx.i230610_i230040.utils.NetworkMonitor
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * User Repository - Handles user caching for offline access
 */
class UserRepository(private val context: Context) {

    private val database = AppDatabase.getDatabase(context)
    private val userDao = database.userDao()
    private val apiService = RetrofitInstance.apiService

    /**
     * Get all cached users (for chat list)
     */
    suspend fun getAllUsers(): List<UserEntity> = withContext(Dispatchers.IO) {
        userDao.getAllUsers()
    }

    /**
     * Get user by ID
     */
    suspend fun getUserById(uid: String): UserEntity? = withContext(Dispatchers.IO) {
        userDao.getUserById(uid)
    }

    /**
     * Fetch chat users from server and cache locally
     */
    suspend fun fetchChatUsersFromServer(userId: String): List<UserEntity> = withContext(Dispatchers.IO) {
        try {
            if (!NetworkMonitor.isOnline(context)) {
                // Return cached users if offline
                return@withContext userDao.getAllUsers()
            }

            val request = GetChatUsersRequest(userId)
            val response = apiService.getChatUsers(request)

            if (response.isSuccessful && response.body()?.success == true) {
                val serverUsers = response.body()?.data?.users ?: emptyList()

                val entities = serverUsers.map { user ->
                    UserEntity(
                        uid = user.uid,
                        username = user.username,
                        email = "", // Not needed for chat list
                        fullName = user.username,
                        profileImageUrl = user.profileImageUrl ?: "",
                        bio = "",
                        isOnline = false,
                        lastSeen = 0L
                    )
                }

                // Insert/update all users
                userDao.insertAll(entities)

                return@withContext entities
            } else {
                // Return cached users on error
                return@withContext userDao.getAllUsers()
            }
        } catch (e: Exception) {
            // Return cached users on exception
            return@withContext userDao.getAllUsers()
        }
    }

    /**
     * Cache a single user
     */
    suspend fun cacheUser(
        uid: String,
        username: String,
        fullName: String,
        profileImageUrl: String,
        email: String = "",
        bio: String = ""
    ) = withContext(Dispatchers.IO) {
        val userEntity = UserEntity(
            uid = uid,
            username = username,
            email = email,
            fullName = fullName,
            profileImageUrl = profileImageUrl,
            bio = bio,
            isOnline = false,
            lastSeen = System.currentTimeMillis()
        )
        userDao.insert(userEntity)
    }

    /**
     * Update user online status
     */
    suspend fun updateOnlineStatus(uid: String, isOnline: Boolean) = withContext(Dispatchers.IO) {
        userDao.updateOnlineStatus(uid, isOnline, System.currentTimeMillis())
    }

    /**
     * Search cached users
     */
    suspend fun searchUsers(query: String): List<UserEntity> = withContext(Dispatchers.IO) {
        userDao.searchUsers("%$query%")
    }
}

