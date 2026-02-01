package com.bandtrack.data.models

import kotlinx.serialization.Serializable

/**
 * Modèle représentant un morceau du répertoire
 * Chaque membre peut indiquer son niveau de maîtrise (0-10)
 */
@Serializable
data class Song(
    val id: String = "",
    val groupId: String = "",
    val title: String = "",
    val artist: String = "",
    val duration: Int = 0, // Durée en secondes
    val structure: String = "", // Ex: "Intro - Couplet - Refrain - Solo - Refrain - Outro"
    val key: String? = null, // Tonalité (ex: "Am", "G", "C#m")
    val tempo: Int? = null, // BPM
    val notes: String = "", // Notes générales sur le morceau
    val masteryLevels: Map<String, Int> = emptyMap(), // userId -> niveau (0-10)
    val addedBy: String = "", // User ID
    val addedAt: Long = 0L,
    val convertedFromSuggestionId: String? = null, // ID de la suggestion d'origine
    val link: String? = null, // Lien vers ressource externe
    val hasAudioNotes: Boolean = false // Indique si des notes audio existent localement
) {
    companion object {
        fun empty() = Song()
        
        /**
         * Créer un morceau à partir d'une suggestion
         */
        fun fromSuggestion(suggestion: Suggestion, addedBy: String): Song {
            return Song(
                groupId = suggestion.groupId,
                title = suggestion.title,
                artist = suggestion.artist,
                link = suggestion.link,
                addedBy = addedBy,
                convertedFromSuggestionId = suggestion.id
            )
        }
    }
    
    /**
     * Récupère le niveau de maîtrise d'un utilisateur
     */
    fun getMasteryLevel(userId: String): Int = masteryLevels[userId] ?: 0
    
    /**
     * Met à jour le niveau de maîtrise d'un utilisateur
     */
    fun updateMasteryLevel(userId: String, level: Int): Song {
        require(level in 0..10) { "Le niveau de maîtrise doit être entre 0 et 10" }
        
        val newLevels = masteryLevels.toMutableMap()
        newLevels[userId] = level
        
        return copy(masteryLevels = newLevels)
    }
    
    /**
     * Calcule le niveau de maîtrise moyen du groupe
     */
    fun getAverageMasteryLevel(): Float {
        if (masteryLevels.isEmpty()) return 0f
        return masteryLevels.values.average().toFloat()
    }
    
    /**
     * Vérifie si le morceau est bien maîtrisé par le groupe (moyenne >= 7)
     */
    fun isWellMastered(): Boolean = getAverageMasteryLevel() >= 7f
}
