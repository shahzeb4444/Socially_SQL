package com.teamsx.i230610_i230040.database.dao

import androidx.room.*
import com.teamsx.i230610_i230040.database.entity.UserEntity

/**
 * Data Access Object for Users
 */
@Dao
interface UserDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(user: UserEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(users: List<UserEntity>)

    // Get user by ID
    @Query("SELECT * FROM users WHERE uid = :uid LIMIT 1")
    suspend fun getUserById(uid: String): UserEntity?

    // Get all cached users
    @Query("SELECT * FROM users ORDER BY username ASC")
    suspend fun getAllUsers(): List<UserEntity>

    // Update online status
    @Query("UPDATE users SET isOnline = :isOnline, lastSeen = :lastSeen WHERE uid = :uid")
    suspend fun updateOnlineStatus(uid: String, isOnline: Boolean, lastSeen: Long)

    // Update user data
    @Update
    suspend fun update(user: UserEntity)

    // Delete user
    @Query("DELETE FROM users WHERE uid = :uid")
    suspend fun deleteUser(uid: String)

    // Search users by username
    @Query("SELECT * FROM users WHERE username LIKE '%' || :query || '%' ORDER BY username ASC")
    suspend fun searchUsers(query: String): List<UserEntity>
}

