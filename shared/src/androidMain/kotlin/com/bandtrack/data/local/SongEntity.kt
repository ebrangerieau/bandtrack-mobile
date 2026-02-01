package com.bandtrack.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.bandtrack.data.models.Song

/**
 * Entité Room représentant un morceau en base locale.
 * Miroir du modèle [Song]
 */
@Entity(tableName = "songs")
data class SongEntity(
    @PrimaryKey
    val id: String,
    val groupId: String,
    val title: String,
    val artist: String,
    val duration: Int,
    val structure: String,
    val key: String?,
    val tempo: Int?,
    val notes: String,
    val masteryLevels: Map<String, Int>, // Converti via Converters
    val addedBy: String,
    val addedAt: Long,
    val convertedFromSuggestionId: String?,
    val link: String?,
    val hasAudioNotes: Boolean,
    val memberInstrumentConfigs: Map<String, String>, // Converti via Converters
    val memberPersonalNotes: Map<String, String> // Converti via Converters
)

/**
 * Extension pour convertir Song -> SongEntity
 */
fun Song.toEntity(): SongEntity {
    return SongEntity(
        id = id,
        groupId = groupId,
        title = title,
        artist = artist,
        duration = duration,
        structure = structure,
        key = key,
        tempo = tempo,
        notes = notes,
        masteryLevels = masteryLevels,
        addedBy = addedBy,
        addedAt = addedAt,
        convertedFromSuggestionId = convertedFromSuggestionId,
        link = link,
        hasAudioNotes = hasAudioNotes,
        memberInstrumentConfigs = memberInstrumentConfigs,
        memberPersonalNotes = memberPersonalNotes
    )
}

/**
 * Extension pour convertir SongEntity -> Song
 */
fun SongEntity.toModel(): Song {
    return Song(
        id = id,
        groupId = groupId,
        title = title,
        artist = artist,
        duration = duration,
        structure = structure,
        key = key,
        tempo = tempo,
        notes = notes,
        masteryLevels = masteryLevels,
        addedBy = addedBy,
        addedAt = addedAt,
        convertedFromSuggestionId = convertedFromSuggestionId,
        link = link,
        hasAudioNotes = hasAudioNotes,
        memberInstrumentConfigs = memberInstrumentConfigs,
        memberPersonalNotes = memberPersonalNotes
    )
}
