package com.teamsx.i230610_i230040.database.dao

import androidx.room.*
import com.teamsx.i230610_i230040.database.entity.SyncQueueEntity

/**
 * Data Access Object for Sync Queue
 * Manages pending actions that need to be synced
 */
@Dao
interface SyncQueueDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(item: SyncQueueEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(items: List<SyncQueueEntity>)

    // Get all pending items
    @Query("SELECT * FROM sync_queue WHERE status = 'pending' ORDER BY timestamp ASC")
    suspend fun getPendingItems(): List<SyncQueueEntity>

    // Get failed items (for retry)
    @Query("SELECT * FROM sync_queue WHERE status = 'failed' AND retryCount < 3 ORDER BY timestamp ASC")
    suspend fun getFailedItems(): List<SyncQueueEntity>

    // Update status
    @Query("UPDATE sync_queue SET status = :status, lastAttempt = :lastAttempt WHERE id = :id")
    suspend fun updateStatus(id: Long, status: String, lastAttempt: Long = System.currentTimeMillis())

    // Increment retry count
    @Query("UPDATE sync_queue SET retryCount = retryCount + 1, lastAttempt = :lastAttempt, errorMessage = :error WHERE id = :id")
    suspend fun incrementRetryCount(id: Long, lastAttempt: Long = System.currentTimeMillis(), error: String = "")

    // Delete item
    @Query("DELETE FROM sync_queue WHERE id = :id")
    suspend fun deleteItem(id: Long)

    // Delete completed items
    @Query("DELETE FROM sync_queue WHERE status = 'completed'")
    suspend fun deleteCompletedItems()

    // Get item by reference ID
    @Query("SELECT * FROM sync_queue WHERE localReferenceId = :referenceId AND status != 'completed' LIMIT 1")
    suspend fun getItemByReferenceId(referenceId: String): SyncQueueEntity?

    // Get all items
    @Query("SELECT * FROM sync_queue ORDER BY timestamp ASC")
    suspend fun getAllItems(): List<SyncQueueEntity>

    // Delete all
    @Query("DELETE FROM sync_queue")
    suspend fun deleteAll()
}

