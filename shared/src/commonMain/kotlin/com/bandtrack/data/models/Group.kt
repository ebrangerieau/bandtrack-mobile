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
    val createdAt: Long = System.currentTimeMillis(),
    val memberCount: Int = 0
) {
    companion object {
        fun empty() = Group()
    }
}
