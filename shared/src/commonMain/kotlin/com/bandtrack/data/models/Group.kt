package com.bandtrack.data.models

import kotlinx.serialization.Serializable

/**
 * Modèle représentant un groupe de musiciens
 */
@Serializable
data class Group(
    val id: String = "",
    val name: String = "",
    val description: String = "",
    val imageUrl: String? = null,
    val createdBy: String = "", // User ID du créateur
    val createdAt: Long = 0L,
    val memberCount: Int = 0,
    val memberIds: List<String> = emptyList() // Pour les requêtes whereArrayContains
) {
    companion object {
        fun empty() = Group()
    }
}
