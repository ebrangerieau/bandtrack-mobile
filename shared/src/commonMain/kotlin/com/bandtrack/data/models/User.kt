package com.bandtrack.data.models

import kotlinx.serialization.Serializable

/**
 * Modèle représentant un utilisateur BandTrack
 */
@Serializable
data class User(
    val id: String = "",
    val email: String = "",
    val displayName: String = "",
    val photoUrl: String? = null,
    val groupIds: List<String> = emptyList(), // IDs des groupes dont l'utilisateur est membre
    val createdAt: Long = 0L,
    val lastLoginAt: Long = 0L
) {
    companion object {
        fun empty() = User()
    }
}
