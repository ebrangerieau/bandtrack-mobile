package com.bandtrack.data.models

import kotlinx.serialization.Serializable

enum class PerformanceType {
    REHEARSAL, // Répétition
    GIG,       // Concert
    OTHER      // Autre
}

@Serializable
data class Performance(
    val id: String = "",
    val groupId: String = "",
    val type: PerformanceType = PerformanceType.REHEARSAL,
    val date: Long = 0L, // Timestamp du début de l'événement
    val durationMinutes: Int = 120, // Durée estimée en minutes
    val location: String = "",
    val title: String = "", // Titre optionnel (ex: "Fête de la musique")
    val notes: String = "",
    val setlist: List<String> = emptyList(), // Liste d'IDs de morceaux
    val createdBy: String = ""
) {
    // Constructeur sans argument nécessaire pour Firestore
    constructor() : this("", "", PerformanceType.REHEARSAL)

    fun getDisplayName(): String {
        return if (title.isNotBlank()) title else when(type) {
            PerformanceType.REHEARSAL -> "Répétition"
            PerformanceType.GIG -> "Concert"
            PerformanceType.OTHER -> "Événement"
        }
    }
}
