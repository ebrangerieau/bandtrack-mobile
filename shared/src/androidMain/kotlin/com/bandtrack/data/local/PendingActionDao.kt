package com.bandtrack.data.local

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query

@Dao
interface PendingActionDao {
    @Query("SELECT * FROM pending_actions ORDER BY createdAt ASC")
    suspend fun getAll(): List<PendingActionEntity>

    @Insert
    suspend fun insert(action: PendingActionEntity): Long

    @Delete
    suspend fun delete(action: PendingActionEntity)
    
    @Query("DELETE FROM pending_actions WHERE id = :id")
    suspend fun deleteById(id: Long)
}
