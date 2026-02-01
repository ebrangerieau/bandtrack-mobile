package com.bandtrack.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import kotlinx.coroutines.flow.Flow

@Dao
interface SuggestionDao {
    @Query("SELECT * FROM suggestions WHERE groupId = :groupId AND status = 'PENDING' ORDER BY voteCount DESC")
    fun getSuggestionsByGroup(groupId: String): Flow<List<SuggestionEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSuggestion(suggestion: SuggestionEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSuggestions(suggestions: List<SuggestionEntity>)

    @Query("DELETE FROM suggestions WHERE groupId = :groupId")
    suspend fun clearGroupSuggestions(groupId: String)

    @Transaction
    suspend fun replaceGroupSuggestions(groupId: String, suggestions: List<SuggestionEntity>) {
        clearGroupSuggestions(groupId)
        insertSuggestions(suggestions)
    }

    @Query("DELETE FROM suggestions WHERE id = :id")
    suspend fun deleteSuggestion(id: String)

    @Query("SELECT * FROM suggestions WHERE id = :id")
    suspend fun getSuggestionById(id: String): SuggestionEntity?
}
