package com.bandtrack.services.audio

import android.content.Context
import be.tarsos.dsp.AudioDispatcher
import be.tarsos.dsp.AudioEvent
import be.tarsos.dsp.io.android.AudioDispatcherFactory
import be.tarsos.dsp.pitch.PitchDetectionHandler
import be.tarsos.dsp.pitch.PitchDetectionResult
import be.tarsos.dsp.pitch.PitchProcessor
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import kotlin.math.ln
import kotlin.math.pow

data class DetectedKey(
    val rootNote: String, // C, C#, D...
    val scale: String, // Major, Minor
    val confidence: Float // 0.0 - 1.0
)

class AudioAnalysisService(private val context: Context) {

    /**
     * Analyse un fichier audio pour détecter sa tonalité dominante
     */
    suspend fun analyzeKey(filePath: String): Result<DetectedKey> = withContext(Dispatchers.IO) {
        try {
            val file = File(filePath)
            if (!file.exists()) return@withContext Result.failure(Exception("File not found"))

            // Configuration DSP
            val sampleRate = 44100
            val bufferSize = 2048
            val overlap = 0
            
            // Dispatcher
            val dispatcher = AudioDispatcherFactory.fromPipe(file.absolutePath, sampleRate, bufferSize, overlap)
            
            // Accumulateur de notes (PCP - Pitch Class Profile)
            val pcp = FloatArray(12) { 0f }
            var totalEnergy = 0f
            
            // Handler de Pitch (YIN)
            val pitchHandler = PitchDetectionHandler { result, _ ->
                val pitch = result.pitch
                val probability = result.probability
                
                if (pitch != -1f && probability > 0.85f) { // Filtrer les notes incertaines
                    val noteIndex = frequencyToNoteIndex(pitch)
                    if (noteIndex in 0..11) {
                         // Pondération par probabilité
                        pcp[noteIndex] += probability
                        totalEnergy += probability
                    }
                }
            }
            
            val pitchProcessor = PitchProcessor(
                PitchProcessor.PitchEstimationAlgorithm.YIN, 
                sampleRate.toFloat(), 
                bufferSize, 
                pitchHandler
            )
            
            dispatcher.addAudioProcessor(pitchProcessor)
            dispatcher.run() // Bloquant, mais on est dans IO dispatcher
            
            if (totalEnergy == 0f) {
                return@withContext Result.failure(Exception("No tonal content detected"))
            }
            
            // Estimation de la tonalité (Krumhansl-Schmuckler simplifié)
            val estimatedKey = estimateKeyFromPCP(pcp)
            
            Result.success(estimatedKey)
            
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    // Fréquence -> Index de note (0=C, 1=C#... 11=B)
    private fun frequencyToNoteIndex(frequency: Float): Int {
        val log2 = ln(frequency / 440.0) / ln(2.0)
        val noteNumber = (12 * log2).toInt() + 69
        return (noteNumber % 12 + 12) % 12 // Assurer positif 0-11 (A=9, C=0)
        
        // Formule standard MIDI: 69 = A4 (440Hz)
        // C4 = 60. 60 % 12 = 0. Donc 0 = C. Correct.
    }
    
    // Estimation basique de tonalité par corrélation avec templates Majeur/Mineur
    private fun estimateKeyFromPCP(pcp: FloatArray): DetectedKey {
        // Normaliser PCP
        val maxVal = pcp.maxOrNull() ?: 1f
        val normalizedPCP = pcp.map { it / maxVal }.toFloatArray()
        
        // Templates (Krumhansl-Schmuckler ou similaires)
        // C Major profile: C, D, E, F, G, A, B (forts)
        val majorProfile = floatArrayOf(6.35f, 2.23f, 3.48f, 2.33f, 4.38f, 4.09f, 2.52f, 5.19f, 2.39f, 3.66f, 2.29f, 2.88f)
        // C Minor profile: C, D, Eb, F, G, Ab, Bb
        val minorProfile = floatArrayOf(6.33f, 2.68f, 3.52f, 5.38f, 2.60f, 3.53f, 2.54f, 4.75f, 3.98f, 2.69f, 3.34f, 3.17f)
        
        var bestCorrelation = -1f
        var bestKeyIndex = -1
        var isMajor = true
        
        // Tester les 12 rotations pour Majeur
        for (i in 0 until 12) {
            val correlation = calculateCorrelation(normalizedPCP, rotate(majorProfile, i))
            if (correlation > bestCorrelation) {
                bestCorrelation = correlation
                bestKeyIndex = i
                isMajor = true
            }
        }
        
        // Tester les 12 rotations pour Mineur
        for (i in 0 until 12) {
            val correlation = calculateCorrelation(normalizedPCP, rotate(minorProfile, i))
            if (correlation > bestCorrelation) {
                bestCorrelation = correlation
                bestKeyIndex = i
                isMajor = false
            }
        }
        
        val noteNames = arrayOf("C", "C#", "D", "D#", "E", "F", "F#", "G", "G#", "A", "A#", "B")
        val root = noteNames[bestKeyIndex]
        
        return DetectedKey(root, if (isMajor) "Major" else "Minor", bestCorrelation) // Correlation as confidence proxy
    }
    
    private fun rotate(array: FloatArray, distance: Int): FloatArray {
        val result = FloatArray(array.size)
        for (i in array.indices) {
            result[(i + distance) % array.size] = array[i]
        }
        return result
    }
    
    private fun calculateCorrelation(pcp: FloatArray, template: FloatArray): Float {
        var sum = 0f
        for (i in pcp.indices) {
            sum += pcp[i] * template[i]
        }
        return sum
    }
}
