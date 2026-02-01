package com.bandtrack.services

import android.content.Context
import android.media.MediaRecorder
import android.os.Build
import java.io.File
import java.io.IOException

/**
 * Service pour l'enregistrement audio
 */
class AudioRecorderService(private val context: Context) {
    private var recorder: MediaRecorder? = null
    private var currentFilePath: String? = null
    private var isRecording = false

    /**
     * Démarre l'enregistrement audio
     * @return Le chemin du fichier audio ou null en cas d'erreur
     */
    fun startRecording(fileName: String): String? {
        if (isRecording) {
            stopRecording()
        }

        return try {
            val audioDir = File(context.filesDir, "audio_notes")
            if (!audioDir.exists()) {
                audioDir.mkdirs()
            }

            val audioFile = File(audioDir, "$fileName.m4a")
            currentFilePath = audioFile.absolutePath

            recorder = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                MediaRecorder(context)
            } else {
                @Suppress("DEPRECATION")
                MediaRecorder()
            }.apply {
                setAudioSource(MediaRecorder.AudioSource.MIC)
                setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
                setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
                setAudioEncodingBitRate(128000)
                setAudioSamplingRate(44100)
                setOutputFile(currentFilePath)

                try {
                    prepare()
                    start()
                    isRecording = true
                } catch (e: IOException) {
                    e.printStackTrace()
                    release()
                    return null
                }
            }

            currentFilePath
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    /**
     * Arrête l'enregistrement
     * @return Le chemin du fichier enregistré ou null
     */
    fun stopRecording(): String? {
        if (!isRecording) return null

        return try {
            recorder?.apply {
                stop()
                release()
            }
            recorder = null
            isRecording = false
            currentFilePath
        } catch (e: Exception) {
            e.printStackTrace()
            recorder?.release()
            recorder = null
            isRecording = false
            null
        } finally {
            currentFilePath = null
        }
    }

    /**
     * Annule l'enregistrement en cours
     */
    fun cancelRecording() {
        if (isRecording) {
            val filePath = currentFilePath
            stopRecording()
            filePath?.let { File(it).delete() }
        }
    }

    /**
     * Vérifie si un enregistrement est en cours
     */
    fun isRecording(): Boolean = isRecording

    /**
     * Libère les ressources
     */
    fun release() {
        recorder?.release()
        recorder = null
        isRecording = false
        currentFilePath = null
    }
}
