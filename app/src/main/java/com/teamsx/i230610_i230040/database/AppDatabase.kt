package com.teamsx.i230610_i230040.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.teamsx.i230610_i230040.database.dao.*
import com.teamsx.i230610_i230040.database.entity.*

/**
 * App Database - Room Database for offline support
 * Version 1: Initial database with messages, posts, stories, users, and sync queue
 */
@Database(
    entities = [
        MessageEntity::class,
        PostEntity::class,
        StoryEntity::class,
        UserEntity::class,
        SyncQueueEntity::class
    ],
    version = 1,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {

    abstract fun messageDao(): MessageDao
    abstract fun postDao(): PostDao
    abstract fun storyDao(): StoryDao
    abstract fun userDao(): UserDao
    abstract fun syncQueueDao(): SyncQueueDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "socially_database"
                )
                    .fallbackToDestructiveMigration() // For development - removes this in production
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}

