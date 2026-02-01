package com.bandtrack.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters

@Database(entities = [SongEntity::class, PendingActionEntity::class, SuggestionEntity::class, PerformanceEntity::class], version = 6, exportSchema = false)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun songDao(): SongDao
    abstract fun pendingActionDao(): PendingActionDao
    abstract fun suggestionDao(): SuggestionDao
    abstract fun performanceDao(): PerformanceDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "bandtrack_database"
                )
                .fallbackToDestructiveMigration() // Dev mode: reset DB on version change
                .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
