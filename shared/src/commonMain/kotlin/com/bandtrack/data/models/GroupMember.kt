package com.bandtrack.data.models

import kotlinx.serialization.Serializable

/**
 * Rôles possibles dans un groupe
 */
enum class GroupRole {
    ADMIN,  // Peut gérer le groupe, inviter/supprimer des membres
    MEMBER  // Membre standard
}

/**
 * Modèle représentant l'appartenance d'un utilisateur à un groupe
 */
@Serializable
data class GroupMember(
    val userId: String = "",
    val groupId: String = "",
    val displayName: String = "",
    val role: String = GroupRole.MEMBER.name, // Stocké comme String pour Firestore
    val instrument: String? = null, // Instrument principal du musicien
    val joinedAt: Long = System.currentTimeMillis()
) {
    fun getRole(): GroupRole = try {
        GroupRole.valueOf(role)
    } catch (e: Exception) {
        GroupRole.MEMBER
    }

    fun isAdmin(): Boolean = getRole() == GroupRole.ADMIN

    companion object {
        fun empty() = GroupMember()
    }
}
