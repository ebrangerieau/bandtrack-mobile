package com.bandtrack.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.bandtrack.data.models.Suggestion
import com.bandtrack.data.models.SuggestionStatus

@Entity(tableName = "suggestions")
data class SuggestionEntity(
    @PrimaryKey val id: String,
    val groupId: String,
    val title: String,
    val artist: String,
    val link: String?,
    val createdBy: String,
    val createdByName: String,
    val createdAt: Long,
    val votes: Map<String, Boolean>,
    val voteCount: Int,
    val status: String, // Enum as String
    val convertedToSongId: String?
)

fun SuggestionEntity.toModel(): Suggestion {
    return Suggestion(
        id = id,
        groupId = groupId,
        title = title,
        artist = artist,
        link = link,
        createdBy = createdBy,
        createdByName = createdByName,
        createdAt = createdAt,
        votes = votes,
        voteCount = voteCount,
        status = try { SuggestionStatus.valueOf(status) } catch (e: Exception) { SuggestionStatus.PENDING },
        convertedToSongId = convertedToSongId
    )
}

fun Suggestion.toEntity(): SuggestionEntity {
    return SuggestionEntity(
        id = id,
        groupId = groupId,
        title = title,
        artist = artist,
        link = link,
        createdBy = createdBy,
        createdByName = createdByName,
        createdAt = createdAt,
        votes = votes,
        voteCount = voteCount,
        status = status.name,
        convertedToSongId = convertedToSongId
    )
}
