package com.bandtrack.data.models

import kotlinx.serialization.Serializable

/**
 * Modèle représentant un code d'invitation pour rejoindre un groupe
 */
@Serializable
data class InvitationCode(
    val id: String = "",
    val groupId: String = "",
    val code: String = "", // Code alphanumérique unique (ex: "ABCD1234")
    val createdBy: String = "", // User ID du créateur
    val createdAt: Long = System.currentTimeMillis(),
    val expiresAt: Long = 0L, // Timestamp d'expiration (0 = pas d'expiration)
    val maxUses: Int = 0, // Nombre max d'utilisations (0 = illimité)
    val usedCount: Int = 0,
    val isActive: Boolean = true
) {
    fun isExpired(): Boolean {
        return expiresAt > 0 && System.currentTimeMillis() > expiresAt
    }

    fun canBeUsed(): Boolean {
        return isActive && !isExpired() && (maxUses == 0 || usedCount < maxUses)
    }

    companion object {
        fun empty() = InvitationCode()
        
        /**
         * Génère un code aléatoire de 8 caractères
         */
        fun generateCode(): String {
            val chars = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789" // Évite les caractères ambigus
            return (1..8)
                .map { chars.random() }
                .joinToString("")
        }
    }
}
