package com.bandtrack.utils

import kotlin.math.abs

/**
 * Utilitaire pour la gestion des transpositions et tonalités
 */
object TranspositionHelper {

    private val notes = listOf("C", "C#", "D", "D#", "E", "F", "F#", "G", "G#", "A", "A#", "B")
    
    /**
     * Calcule l'écart en demi-tons entre deux tonalités
     * Retourne une suggestion "Capo X" ou "Transposition +X"
     */
    fun getTranspositionSuggestion(originalKey: String, detectedKey: String): String {
        val start = noteToIndex(originalKey)
        val end = noteToIndex(detectedKey)
        
        if (start == -1 || end == -1) return "Tonalité inconnue"
        
        if (start == end) return "Même tonalité"
        
        // Calculer la différence (modulo 12)
        var diff = (end - start)
        // Normaliser entre -6 et +6 pour le chemin le plus court
        if (diff > 6) diff -= 12
        if (diff < -6) diff += 12
        
        return when {
            diff > 0 -> "Capo $diff (ou +$diff demi-tons)"
            diff < 0 -> "Descendre de ${abs(diff)} demi-tons"
            else -> "Même tonalité"
        }
    }
    
    private fun noteToIndex(note: String): Int {
        // Normaliser (Gb -> F#, Db -> C#, etc.)
        val normalized = when(note) {
            "Db" -> "C#"
            "Eb" -> "D#"
            "Gb" -> "F#"
            "Ab" -> "G#"
            "Bb" -> "A#"
            else -> note
        }
        return notes.indexOf(normalized)
    }
}
