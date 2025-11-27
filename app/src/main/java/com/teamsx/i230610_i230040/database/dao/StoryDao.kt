package com.teamsx.i230610_i230040.database.dao

import androidx.lifecycle.LiveData
import androidx.room.*
import com.teamsx.i230610_i230040.database.entity.StoryEntity

/**
 * Data Access Object for Stories
 */
@Dao
interface StoryDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(story: StoryEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(stories: List<StoryEntity>)

    // Get all active stories (not expired)
    @Query("SELECT * FROM stories WHERE expiresAt > :currentTime ORDER BY timestamp DESC")
    fun getActiveStories(currentTime: Long = System.currentTimeMillis()): LiveData<List<StoryEntity>>

    // Get active stories one-time
    @Query("SELECT * FROM stories WHERE expiresAt > :currentTime ORDER BY timestamp DESC")
    suspend fun getActiveStoriesOnce(currentTime: Long = System.currentTimeMillis()): List<StoryEntity>

    // Get stories by user
    @Query("SELECT * FROM stories WHERE userId = :userId AND expiresAt > :currentTime ORDER BY timestamp DESC")
    fun getStoriesByUser(userId: String, currentTime: Long = System.currentTimeMillis()): LiveData<List<StoryEntity>>

    // Get unsynced stories
    @Query("SELECT * FROM stories WHERE isSynced = 0 ORDER BY localTimestamp ASC")
    suspend fun getUnsyncedStories(): List<StoryEntity>

    // Update sync status
    @Query("UPDATE stories SET isSynced = :synced, syncStatus = :status WHERE storyId = :storyId")
    suspend fun updateSyncStatus(storyId: String, synced: Boolean, status: String)

    // Update story ID after sync
    @Query("UPDATE stories SET storyId = :newStoryId, isSynced = 1, syncStatus = 'synced' WHERE storyId = :oldStoryId")
    suspend fun updateStoryId(oldStoryId: String, newStoryId: String)

    // Delete story
    @Query("DELETE FROM stories WHERE storyId = :storyId")
    suspend fun deleteStory(storyId: String)

    // Delete expired stories
    @Query("DELETE FROM stories WHERE expiresAt < :currentTime")
    suspend fun deleteExpiredStories(currentTime: Long = System.currentTimeMillis())

    // Get story by ID
    @Query("SELECT * FROM stories WHERE storyId = :storyId LIMIT 1")
    suspend fun getStoryById(storyId: String): StoryEntity?

    // Update viewed by list
    @Query("UPDATE stories SET viewedBy = :viewedBy WHERE storyId = :storyId")
    suspend fun updateViewedBy(storyId: String, viewedBy: String)
}

