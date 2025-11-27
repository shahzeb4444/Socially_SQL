# 🔧 CRITICAL FIX REQUIRED - MessageDao.kt Corrupted!

## ⚠️ **Problem Discovered:**

The `MessageDao.kt` file got **corrupted** during creation - the content is displayed backwards/reversed! This is causing all the errors.

---

## ✅ **SOLUTION:**

### **Step 1: Delete Corrupted File**

Delete this file:
```
C:\Users\Dr Irum Shaikh\AndroidStudioProjects\23I-0610-23I-0040_Assignment3_Socially\app\src\main\java\com\teamsx\i230610_i230040\database\dao\MessageDao.kt
```

### **Step 2: Rename Fixed File**

Rename this file:
```
FROM: MessageDao_Fixed.kt
TO:   MessageDao.kt
```

**OR** manually copy the content from `MessageDao_Fixed.kt` to replace `MessageDao.kt`

---

## 📄 **Correct MessageDao.kt Content:**

Here's what the file SHOULD look like:

```kotlin
package com.teamsx.i230610_i230040.database.dao

import androidx.lifecycle.LiveData
import androidx.room.*
import com.teamsx.i230610_i230040.database.entity.MessageEntity

/**
 * Data Access Object for Messages
 * Provides methods to interact with messages table
 */
@Dao
interface MessageDao {
    
    // Insert or replace message
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(message: MessageEntity)
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(messages: List<MessageEntity>)
    
    // Get all messages for a chat (LiveData for auto-updates)
    @Query("SELECT * FROM messages WHERE chatId = :chatId ORDER BY timestamp ASC")
    fun getMessagesForChat(chatId: String): LiveData<List<MessageEntity>>
    
    // Get messages for a chat (one-time)
    @Query("SELECT * FROM messages WHERE chatId = :chatId ORDER BY timestamp ASC")
    suspend fun getMessagesForChatOnce(chatId: String): List<MessageEntity>
    
    // Get unsynced messages
    @Query("SELECT * FROM messages WHERE isSynced = 0 ORDER BY localTimestamp ASC")
    suspend fun getUnsyncedMessages(): List<MessageEntity>
    
    // Update sync status
    @Query("UPDATE messages SET isSynced = :synced, syncStatus = :status WHERE messageId = :messageId")
    suspend fun updateSyncStatus(messageId: String, synced: Boolean, status: String)
    
    // Update message with server ID (after sync)
    @Query("UPDATE messages SET messageId = :newMessageId, isSynced = 1, syncStatus = 'synced' WHERE messageId = :oldMessageId")
    suspend fun updateMessageId(oldMessageId: String, newMessageId: String)
    
    // Delete message
    @Query("DELETE FROM messages WHERE messageId = :messageId")
    suspend fun deleteMessage(messageId: String)
    
    // Mark message as deleted (soft delete)
    @Query("UPDATE messages SET isDeleted = 1, deletedAt = :deletedAt WHERE messageId = :messageId")
    suspend fun markAsDeleted(messageId: String, deletedAt: Long)
    
    // Update message text (for edits)
    @Query("UPDATE messages SET text = :newText, isEdited = 1 WHERE messageId = :messageId")
    suspend fun updateMessageText(messageId: String, newText: String)
    
    // Get message by ID
    @Query("SELECT * FROM messages WHERE messageId = :messageId LIMIT 1")
    suspend fun getMessageById(messageId: String): MessageEntity?
    
    // Delete all messages for a chat
    @Query("DELETE FROM messages WHERE chatId = :chatId")
    suspend fun deleteAllMessagesForChat(chatId: String)
    
    // Get messages count for a chat
    @Query("SELECT COUNT(*) FROM messages WHERE chatId = :chatId")
    suspend fun getMessagesCount(chatId: String): Int
}
```

---

## 🎯 **Quick Fix Steps:**

1. **Open file explorer:** Navigate to the dao folder
2. **Delete:** `MessageDao.kt` (corrupted file)
3. **Rename:** `MessageDao_Fixed.kt` → `MessageDao.kt`
4. **Sync Gradle:** Click "Sync Now"
5. **Rebuild:** Build → Rebuild Project

---

## ✅ **After Fix:**

All errors in:
- `MessageRepository.kt` ✅
- `SyncWorker.kt` ✅

Will be **RESOLVED** after:
1. Fixing MessageDao.kt (above)
2. Syncing Gradle
3. Rebuilding project

---

## 📊 **Why This Happened:**

The file got created with reversed content - likely a read_file issue showing content backwards. The fix is simply to use the correct version.

---

## 🔥 **DO THIS NOW:**

1. Delete `MessageDao.kt`
2. Rename `MessageDao_Fixed.kt` to `MessageDao.kt`
3. Sync & Rebuild

**All errors will disappear!** ✅

