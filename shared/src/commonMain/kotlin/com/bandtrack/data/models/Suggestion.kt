package com.bandtrack.data.models

import kotlinx.serialization.Serializable

/**
 * Modèle représentant une suggestion de morceau
 * Les membres du groupe peuvent proposer des morceaux et voter
 */
@Serializable
data class Suggestion(
    val id: String = "",
    val groupId: String = "",
    val title: String = "",
    val artist: String = "",
    val link: String? = null, // Lien YouTube, Spotify, etc.
    val createdBy: String = "", // User ID
    val createdByName: String = "", // Nom d'affichage
    val createdAt: Long = 0L,
    val votes: Map<String, Boolean> = emptyMap(), // userId -> hasVoted
    val voteCount: Int = 0,
    val status: SuggestionStatus = SuggestionStatus.PENDING,
    val convertedToSongId: String? = null // ID du morceau si converti
) {
    companion object {
        fun empty() = Suggestion()
    }
    
    /**
     * Vérifie si un utilisateur a voté pour cette suggestion
     */
    fun hasUserVoted(userId: String): Boolean = votes[userId] == true
    
    /**
     * Ajoute ou retire le vote d'un utilisateur
     */
    fun toggleVote(userId: String): Suggestion {
        val newVotes = votes.toMutableMap()
        val currentVote = newVotes[userId] ?: false
        
        return if (currentVote) {
            // Retirer le vote
            newVotes.remove(userId)
            copy(votes = newVotes, voteCount = voteCount - 1)
        } else {
            // Ajouter le vote
            newVotes[userId] = true
            copy(votes = newVotes, voteCount = voteCount + 1)
        }
    }
}

/**
 * Statut d'une suggestion
 */
@Serializable
enum class SuggestionStatus {
    PENDING,    // En attente de votes
    ACCEPTED,   // Acceptée et convertie en morceau
    REJECTED    // Rejetée
}
