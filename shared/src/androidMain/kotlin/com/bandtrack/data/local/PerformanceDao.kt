package com.bandtrack.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface PerformanceDao {
    @Query("SELECT * FROM performances WHERE groupId = :groupId ORDER BY date ASC")
    fun getPerformancesByGroup(groupId: String): Flow<List<PerformanceEntity>>

    @Query("SELECT * FROM performances WHERE id = :performanceId")
    suspend fun getPerformanceById(performanceId: String): PerformanceEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPerformance(performance: PerformanceEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun replaceGroupPerformances(performances: List<PerformanceEntity>)
    
    @Query("DELETE FROM performances WHERE groupId = :groupId")
    suspend fun deletePerformancesByGroup(groupId: String)

    @Query("DELETE FROM performances WHERE id = :performanceId")
    suspend fun deletePerformance(performanceId: String)
}
