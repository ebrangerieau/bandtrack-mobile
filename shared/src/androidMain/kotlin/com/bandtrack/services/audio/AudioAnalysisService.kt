package com.bandtrack.services.audio

import android.content.Context
import android.media.MediaCodec
import android.media.MediaExtractor
import android.media.MediaFormat
import be.tarsos.dsp.AudioDispatcher

import be.tarsos.dsp.io.TarsosDSPAudioFormat
import be.tarsos.dsp.io.TarsosDSPAudioInputStream
import be.tarsos.dsp.pitch.PitchDetectionHandler
import be.tarsos.dsp.pitch.PitchProcessor
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import kotlin.math.ln

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

            // Décodage du fichier audio en PCM float array
            val audioData = decodeAudioFile(file.absolutePath, sampleRate)
                ?: return@withContext Result.failure(Exception("Failed to decode audio"))

            // Création manuelle du Dispatcher via custom InputStream
            // TarsosDSP Core n'expose pas directement fromFloatArray sans AudioAudioDispatcherFactory (JVM)
            val format = TarsosDSPAudioFormat(sampleRate.toFloat(), 16, 1, true, false)
            val audioStream = FloatArrayAudioInputStream(audioData, format)
            
            val dispatcher = AudioDispatcher(audioStream, bufferSize, overlap)
            
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
            dispatcher.run() 
            
            if (totalEnergy == 0f) {
                return@withContext Result.failure(Exception("No tonal content detected"))
            }
            
            val estimatedKey = estimateKeyFromPCP(pcp)
            
            Result.success(estimatedKey)
            
        } catch (e: Exception) {
            e.printStackTrace()
            Result.failure(e)
        }
    }

    /**
     * Décoder un fichier audio en tableau de floats (PCM Mono)
     * Utilise MediaCodec (Android)
     */
    private fun decodeAudioFile(path: String, targetSampleRate: Int): FloatArray? {
        val extractor = MediaExtractor()
        try {
            extractor.setDataSource(path)
            var trackIndex = -1
            var format: MediaFormat? = null
            
            for (i in 0 until extractor.trackCount) {
                val f = extractor.getTrackFormat(i)
                val mime = f.getString(MediaFormat.KEY_MIME)
                if (mime?.startsWith("audio/") == true) {
                    trackIndex = i
                    format = f
                    break
                }
            }
            
            if (trackIndex == -1 || format == null) return null
            
            extractor.selectTrack(trackIndex)
            
            val mime = format.getString(MediaFormat.KEY_MIME) ?: return null
            val codec = MediaCodec.createDecoderByType(mime)
            codec.configure(format, null, null, 0)
            codec.start()
            
            val info = MediaCodec.BufferInfo()
            var inputEOS = false
            var outputEOS = false
            
            val pcmData = java.io.ByteArrayOutputStream()
            
            while (!outputEOS) {
                if (!inputEOS) {
                    val inputIndex = codec.dequeueInputBuffer(10000)
                    if (inputIndex >= 0) {
                        val buffer = codec.getInputBuffer(inputIndex)
                        if (buffer != null) {
                            val sampleSize = extractor.readSampleData(buffer, 0)
                            if (sampleSize < 0) {
                                codec.queueInputBuffer(inputIndex, 0, 0, 0, MediaCodec.BUFFER_FLAG_END_OF_STREAM)
                                inputEOS = true
                            } else {
                                codec.queueInputBuffer(inputIndex, 0, sampleSize, extractor.sampleTime, 0)
                                extractor.advance()
                            }
                        }
                    }
                }
                
                val outputIndex = codec.dequeueOutputBuffer(info, 10000)
                if (outputIndex >= 0) {
                    val buffer = codec.getOutputBuffer(outputIndex)
                    if (buffer != null) {
                        val chunk = ByteArray(info.size)
                        buffer.get(chunk)
                        buffer.clear()
                        if (info.size > 0) {
                            pcmData.write(chunk)
                        }
                    }
                    codec.releaseOutputBuffer(outputIndex, false)
                    if ((info.flags and MediaCodec.BUFFER_FLAG_END_OF_STREAM) != 0) {
                        outputEOS = true
                    }
                } else if (outputIndex == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED) {
                    // Handle format change if needed (e.g. sample rate changed)
                }
            }
            
            codec.stop()
            codec.release()

            val bytes = pcmData.toByteArray()
            // Assume 16 bit little endian, mono or take first channel
            
            // Conversion manuelle Bytes (16-bit PCM LE) -> Floats (-1.0..1.0)
            val floatBuffer = FloatArray(bytes.size / 2)
            for (i in floatBuffer.indices) {
                // Little Endian: Low byte first
                val low = bytes[i * 2].toInt() and 0xFF
                val high = bytes[i * 2 + 1].toInt()
                // Combiner en Short (16-bit signed)
                val sample = (high shl 8) or low
                // Normaliser en float
                floatBuffer[i] = sample.toShort() / 32768.0f
            }
            
            return floatBuffer
            
        } catch (e: Exception) {
            e.printStackTrace()
            return null
        } finally {
            extractor.release()
        }
    }
    
    // Fréquence -> Index de note (0=C, 1=C#... 11=B)
    private fun frequencyToNoteIndex(frequency: Float): Int {
        val log2 = ln(frequency / 440.0) / ln(2.0)
        val noteNumber = (12 * log2).toInt() + 69
        return (noteNumber % 12 + 12) % 12 // Assurer positif 0-11 (A=9, C=0)
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
    
    /**
     * Helper pour transformer un FloatArray en TarsosDSPAudioInputStream
     */
    private class FloatArrayAudioInputStream(
        private val floatData: FloatArray,
        private val format: TarsosDSPAudioFormat
    ) : TarsosDSPAudioInputStream {
        private var position = 0

        override fun skip(bytesToSkip: Long): Long {
            // Un float = 4 bytes ? Non, ici on simule du 16bit PCM signed donc 2 bytes par sample
            // Mais Tarsos interne travaille souvent en bytes.
            // Simplification : On ne supporte pas le skip complexe ici pour l'analyse offline
            val samplesToSkip = bytesToSkip / format.frameSize
            position += samplesToSkip.toInt()
            return bytesToSkip
        }

        override fun read(b: ByteArray, off: Int, len: Int): Int {
            if (position >= floatData.size) return -1
            
            // On doit convertir les floats en bytes selon le format demandé (16 bit LE)
            // C'est ce que attend AudioDispatcher qui lit des bytes pour les reconvertir en float...
            // C'est un peu inefficace mais c'est le contrat de l'interface.
            
            var bytesRead = 0
            var i = off
            while (i < off + len && position < floatData.size) {
                val sample = floatData[position++]
                // Float (-1.0 à 1.0) vers Short (-32768 à 32767)
                val s = (sample * 32767).toInt().coerceIn(-32768, 32767).toShort()
                
                // Little Endian
                b[i++] = (s.toInt() and 0xFF).toByte()
                if (i < off + len) {
                    b[i++] = ((s.toInt() shr 8) and 0xFF).toByte()
                }
                bytesRead += 2
            }
            
            return bytesRead
        }

        override fun close() {
            // Rien à fermer
        }

        override fun getFormat(): TarsosDSPAudioFormat {
            return format
        }

        override fun getFrameLength(): Long {
            return floatData.size.toLong()
        }
    }
}
