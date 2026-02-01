package com.bandtrack.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.bandtrack.data.models.Performance
import com.bandtrack.data.models.PerformanceType

@Entity(tableName = "performances")
data class PerformanceEntity(
    @PrimaryKey val id: String,
    val groupId: String,
    val type: String, // Stored as String (name of enum)
    val date: Long,
    val durationMinutes: Int,
    val location: String,
    val title: String,
    val notes: String,
    val setlist: String, // JSON String (via Converters)
    val createdBy: String
)

fun PerformanceEntity.toModel(converters: Converters = Converters()): Performance {
    return Performance(
        id = id,
        groupId = groupId,
        type = try { PerformanceType.valueOf(type) } catch (e: Exception) { PerformanceType.REHEARSAL },
        date = date,
        durationMinutes = durationMinutes,
        location = location,
        title = title,
        notes = notes,
        setlist = converters.fromStringList(setlist),
        createdBy = createdBy
    )
}

fun Performance.toEntity(converters: Converters = Converters()): PerformanceEntity {
    return PerformanceEntity(
        id = id,
        groupId = groupId,
        type = type.name,
        date = date,
        durationMinutes = durationMinutes,
        location = location,
        title = title,
        notes = notes,
        setlist = converters.toStringList(setlist),
        createdBy = createdBy
    )
}
