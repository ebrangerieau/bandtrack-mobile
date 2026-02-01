package com.bandtrack.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import kotlinx.coroutines.flow.Flow

@Dao
interface SongDao {
    @Query("SELECT * FROM songs WHERE groupId = :groupId ORDER BY title ASC")
    fun getSongsByGroup(groupId: String): Flow<List<SongEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSongs(songs: List<SongEntity>)

    @Query("DELETE FROM songs WHERE groupId = :groupId")
    suspend fun clearGroupSongs(groupId: String)

    @Transaction
    suspend fun replaceGroupSongs(groupId: String, songs: List<SongEntity>) {
        // Simple strategy: clear all for group and re-insert 
        // Note: For finer updates, we would merge, but this is good for a cache refresh
        // Optimisation possible: Only delete IDs not in the new list
        clearGroupSongs(groupId)
        insertSongs(songs)
    }

    @Query("DELETE FROM songs WHERE id = :songId")
    suspend fun deleteSong(songId: String)
}
