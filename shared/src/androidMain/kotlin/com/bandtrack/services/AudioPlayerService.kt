package com.bandtrack.services

import android.content.Context
import android.media.MediaPlayer
import java.io.File
import java.io.IOException

/**
 * Service pour la lecture audio
 */
class AudioPlayerService(private val context: Context) {
    private var player: MediaPlayer? = null
    private var isPlaying = false
    private var currentFilePath: String? = null
    private var onCompletionListener: (() -> Unit)? = null

    /**
     * Démarre la lecture d'un fichier audio
     * @param filePath Chemin du fichier audio
     * @return true si la lecture a démarré, false sinon
     */
    fun startPlaying(filePath: String): Boolean {
        if (isPlaying) {
            stopPlaying()
        }

        val file = File(filePath)
        if (!file.exists()) {
            return false
        }

        return try {
            player = MediaPlayer().apply {
                setDataSource(filePath)
                prepare()
                start()
                setOnCompletionListener {
                    this@AudioPlayerService.isPlaying = false
                    onCompletionListener?.invoke()
                }
            }
            currentFilePath = filePath
            isPlaying = true
            true
        } catch (e: IOException) {
            e.printStackTrace()
            player?.release()
            player = null
            false
        }
    }

    /**
     * Arrête la lecture
     */
    fun stopPlaying() {
        if (!isPlaying) return

        try {
            player?.apply {
                if (isPlaying) {
                    stop()
                }
                release()
            }
        } catch (e: Exception) {
            e.printStackTrace()
        } finally {
            player = null
            isPlaying = false
            currentFilePath = null
        }
    }

    /**
     * Met en pause la lecture
     */
    fun pausePlaying() {
        if (isPlaying) {
            player?.pause()
            isPlaying = false
        }
    }

    /**
     * Reprend la lecture
     */
    fun resumePlaying() {
        if (!isPlaying && player != null) {
            player?.start()
            isPlaying = true
        }
    }

    /**
     * Obtient la durée totale du fichier audio en millisecondes
     */
    fun getDuration(): Int {
        return player?.duration ?: 0
    }

    /**
     * Obtient la position actuelle de lecture en millisecondes
     */
    fun getCurrentPosition(): Int {
        return player?.currentPosition ?: 0
    }

    /**
     * Définit la position de lecture
     */
    fun seekTo(position: Int) {
        player?.seekTo(position)
    }

    /**
     * Vérifie si la lecture est en cours
     */
    fun isPlaying(): Boolean = isPlaying

    /**
     * Définit un listener pour la fin de lecture
     */
    fun setOnCompletionListener(listener: () -> Unit) {
        onCompletionListener = listener
    }

    /**
     * Libère les ressources
     */
    fun release() {
        player?.release()
        player = null
        isPlaying = false
        currentFilePath = null
        onCompletionListener = null
    }
}
