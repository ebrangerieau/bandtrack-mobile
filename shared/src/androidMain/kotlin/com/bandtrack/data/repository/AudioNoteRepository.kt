package com.bandtrack.data.repository

import android.content.Context
import com.bandtrack.data.models.AudioNote
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.File

/**
 * Repository pour la gestion des notes audio
 * Les fichiers audio sont stockés localement sur l'appareil
 */
class AudioNoteRepository(private val context: Context) {
    private val json = Json { prettyPrint = true }
    private val metadataFile = File(context.filesDir, "audio_notes_metadata.json")
    
    /**
     * Récupère toutes les notes audio d'un morceau
     */
    suspend fun getAudioNotes(songId: String): Result<List<AudioNote>> = withContext(Dispatchers.IO) {
        try {
            val allNotes = loadMetadata()
            val songNotes = allNotes.filter { it.songId == songId }
                .sortedByDescending { it.createdAt }
            Result.success(songNotes)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Récupère une note audio par ID
     */
    suspend fun getAudioNote(noteId: String): Result<AudioNote> = withContext(Dispatchers.IO) {
        try {
            val allNotes = loadMetadata()
            val note = allNotes.find { it.id == noteId }
                ?: return@withContext Result.failure(Exception("Audio note not found"))
            Result.success(note)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Sauvegarde une nouvelle note audio
     */
    suspend fun saveAudioNote(audioNote: AudioNote): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            // Vérifier que le fichier audio existe
            val audioFile = File(audioNote.localFilePath)
            if (!audioFile.exists()) {
                return@withContext Result.failure(Exception("Audio file not found"))
            }

            val allNotes = loadMetadata().toMutableList()
            allNotes.add(audioNote)
            saveMetadata(allNotes)
            
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Met à jour une note audio
     */
    suspend fun updateAudioNote(audioNote: AudioNote): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val allNotes = loadMetadata().toMutableList()
            val index = allNotes.indexOfFirst { it.id == audioNote.id }
            
            if (index == -1) {
                return@withContext Result.failure(Exception("Audio note not found"))
            }
            
            allNotes[index] = audioNote
            saveMetadata(allNotes)
            
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Supprime une note audio
     */
    suspend fun deleteAudioNote(noteId: String): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val allNotes = loadMetadata().toMutableList()
            val note = allNotes.find { it.id == noteId }
                ?: return@withContext Result.failure(Exception("Audio note not found"))
            
            // Supprimer le fichier audio
            val audioFile = File(note.localFilePath)
            if (audioFile.exists()) {
                audioFile.delete()
            }
            
            // Supprimer des métadonnées
            allNotes.removeIf { it.id == noteId }
            saveMetadata(allNotes)
            
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Supprime toutes les notes audio d'un morceau
     */
    suspend fun deleteAllAudioNotes(songId: String): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val allNotes = loadMetadata().toMutableList()
            val songNotes = allNotes.filter { it.songId == songId }
            
            // Supprimer tous les fichiers audio
            songNotes.forEach { note ->
                val audioFile = File(note.localFilePath)
                if (audioFile.exists()) {
                    audioFile.delete()
                }
            }
            
            // Supprimer des métadonnées
            allNotes.removeIf { it.songId == songId }
            saveMetadata(allNotes)
            
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Obtient le nombre de notes audio pour un morceau
     */
    suspend fun getAudioNotesCount(songId: String): Int = withContext(Dispatchers.IO) {
        try {
            val allNotes = loadMetadata()
            allNotes.count { it.songId == songId }
        } catch (e: Exception) {
            0
        }
    }

    /**
     * Charge les métadonnées depuis le fichier JSON
     */
    private fun loadMetadata(): List<AudioNote> {
        return try {
            if (!metadataFile.exists()) {
                emptyList()
            } else {
                val jsonString = metadataFile.readText()
                json.decodeFromString<List<AudioNote>>(jsonString)
            }
        } catch (e: Exception) {
            emptyList()
        }
    }

    /**
     * Sauvegarde les métadonnées dans le fichier JSON
     */
    private fun saveMetadata(notes: List<AudioNote>) {
        val jsonString = json.encodeToString(notes)
        metadataFile.writeText(jsonString)
    }

    /**
     * Nettoie les fichiers orphelins (fichiers sans métadonnées)
     */
    suspend fun cleanupOrphanedFiles(): Result<Int> = withContext(Dispatchers.IO) {
        try {
            val audioDir = File(context.filesDir, "audio_notes")
            if (!audioDir.exists()) {
                return@withContext Result.success(0)
            }

            val allNotes = loadMetadata()
            val validPaths = allNotes.map { it.localFilePath }.toSet()
            
            var deletedCount = 0
            audioDir.listFiles()?.forEach { file ->
                if (file.absolutePath !in validPaths) {
                    if (file.delete()) {
                        deletedCount++
                    }
                }
            }
            
            Result.success(deletedCount)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
