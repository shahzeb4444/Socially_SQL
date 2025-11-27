package com.teamsx.i230610_i230040.database.dao

import androidx.lifecycle.LiveData
import androidx.room.*
import com.teamsx.i230610_i230040.database.entity.PostEntity

/**
 * Data Access Object for Posts
 */
@Dao
interface PostDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(post: PostEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(posts: List<PostEntity>)

    // Get all posts (LiveData)
    @Query("SELECT * FROM posts ORDER BY timestamp DESC")
    fun getAllPosts(): LiveData<List<PostEntity>>

    // Get posts one-time
    @Query("SELECT * FROM posts ORDER BY timestamp DESC")
    suspend fun getAllPostsOnce(): List<PostEntity>

    // Get posts by user
    @Query("SELECT * FROM posts WHERE userId = :userId ORDER BY timestamp DESC")
    fun getPostsByUser(userId: String): LiveData<List<PostEntity>>

    // Get unsynced posts
    @Query("SELECT * FROM posts WHERE isSynced = 0 ORDER BY localTimestamp ASC")
    suspend fun getUnsyncedPosts(): List<PostEntity>

    // Update sync status
    @Query("UPDATE posts SET isSynced = :synced, syncStatus = :status WHERE postId = :postId")
    suspend fun updateSyncStatus(postId: String, synced: Boolean, status: String)

    // Update post ID after sync
    @Query("UPDATE posts SET postId = :newPostId, isSynced = 1, syncStatus = 'synced' WHERE postId = :oldPostId")
    suspend fun updatePostId(oldPostId: String, newPostId: String)

    // Update likes
    @Query("UPDATE posts SET likesCount = :count, likedBy = :likedBy WHERE postId = :postId")
    suspend fun updateLikes(postId: String, count: Int, likedBy: String)

    // Delete post
    @Query("DELETE FROM posts WHERE postId = :postId")
    suspend fun deletePost(postId: String)

    // Get post by ID
    @Query("SELECT * FROM posts WHERE postId = :postId LIMIT 1")
    suspend fun getPostById(postId: String): PostEntity?

    // Clear all posts
    @Query("DELETE FROM posts")
    suspend fun deleteAllPosts()
}

