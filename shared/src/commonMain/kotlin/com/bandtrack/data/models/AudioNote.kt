package com.bandtrack.data.models

import kotlinx.serialization.Serializable

/**
 * Modèle représentant une note audio locale
 * Les fichiers audio sont stockés localement sur l'appareil
 */
@Serializable
data class AudioNote(
    val id: String = "",
    val songId: String = "",
    val groupId: String = "",
    val userId: String = "", // Créateur de la note
    val userName: String = "",
    val title: String = "",
    val description: String = "",
    val localFilePath: String = "", // Chemin local du fichier audio
    val duration: Int = 0, // Durée en secondes
    val createdAt: Long = 0L,
    val fileSize: Long = 0, // Taille du fichier en octets
    val mimeType: String = "audio/mp4" // Type MIME (audio/mp4, audio/mpeg, etc.)
) {
    companion object {
        fun empty() = AudioNote()
    }
    
    /**
     * Vérifie si le fichier audio existe localement
     */
    fun fileExists(): Boolean {
        // Cette méthode sera implémentée côté Android
        return localFilePath.isNotEmpty()
    }
    
    /**
     * Formate la durée en format lisible (mm:ss)
     */
    fun getFormattedDuration(): String {
        val minutes = duration / 60
        val seconds = duration % 60
        return String.format("%d:%02d", minutes, seconds)
    }
    
    /**
     * Formate la taille du fichier en format lisible
     */
    fun getFormattedFileSize(): String {
        return when {
            fileSize < 1024 -> "$fileSize B"
            fileSize < 1024 * 1024 -> "${fileSize / 1024} KB"
            else -> String.format("%.1f MB", fileSize / (1024.0 * 1024.0))
        }
    }
}
