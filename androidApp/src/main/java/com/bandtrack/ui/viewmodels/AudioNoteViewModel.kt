package com.bandtrack.ui.viewmodels

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.bandtrack.data.models.AudioNote
import com.bandtrack.data.repository.AudioNoteRepository
import com.bandtrack.data.repository.SongRepository
import com.bandtrack.services.AudioPlayerService
import com.bandtrack.services.AudioRecorderService
import com.bandtrack.services.audio.AudioAnalysisService
import com.bandtrack.services.audio.DetectedKey
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.UUID

/**
 * ViewModel pour la gestion des notes audio
 */
class AudioNoteViewModel(
    private val context: Context,
    private val repository: AudioNoteRepository,
    private val songRepository: SongRepository
) : ViewModel() {

    private val recorderService = AudioRecorderService(context)
    private val playerService = AudioPlayerService(context)
    private val analysisService = AudioAnalysisService(context)

    // État de l'enregistrement
    private val _isRecording = MutableStateFlow(false)
    val isRecording: StateFlow<Boolean> = _isRecording.asStateFlow()

    private val _recordingDuration = MutableStateFlow(0L)
    val recordingDuration: StateFlow<Long> = _recordingDuration.asStateFlow()

    // État de la lecture
    private val _isPlaying = MutableStateFlow(false)
    val isPlaying: StateFlow<Boolean> = _isPlaying.asStateFlow()

    private val _currentPlayingNoteId = MutableStateFlow<String?>(null)
    val currentPlayingNoteId: StateFlow<String?> = _currentPlayingNoteId.asStateFlow()

    private val _playbackPosition = MutableStateFlow(0)
    val playbackPosition: StateFlow<Int> = _playbackPosition.asStateFlow()

    private val _playbackDuration = MutableStateFlow(0)
    val playbackDuration: StateFlow<Int> = _playbackDuration.asStateFlow()

    // Liste des notes audio
    private val _audioNotes = MutableStateFlow<List<AudioNote>>(emptyList())
    val audioNotes: StateFlow<List<AudioNote>> = _audioNotes.asStateFlow()

    // État de chargement et erreurs
    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    // Résultats d'analyse : Map<NoteId, DetectedKey>
    private val _analysisResults = MutableStateFlow<Map<String, DetectedKey>>(emptyMap())
    val analysisResults: StateFlow<Map<String, DetectedKey>> = _analysisResults.asStateFlow()

    // Loading spécifique pour l'analyse
    private val _isAnalyzing = MutableStateFlow(false)
    val isAnalyzing: StateFlow<Boolean> = _isAnalyzing.asStateFlow()

    private var recordingStartTime = 0L
    private var currentRecordingFileName: String? = null

    init {
        playerService.setOnCompletionListener {
            _isPlaying.value = false
            _currentPlayingNoteId.value = null
            _playbackPosition.value = 0
        }
    }

    /**
     * Charge les notes audio d'un morceau
     */
    fun loadAudioNotes(songId: String) {
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null

            repository.getAudioNotes(songId).fold(
                onSuccess = { notes ->
                    _audioNotes.value = notes
                    _isLoading.value = false
                },
                onFailure = { exception ->
                    _error.value = exception.message ?: "Erreur lors du chargement des notes"
                    _isLoading.value = false
                }
            )
        }
    }

    /**
     * Démarre l'enregistrement d'une note audio
     */
    fun startRecording() {
        if (_isRecording.value) return

        val fileName = "audio_note_${UUID.randomUUID()}"
        val filePath = recorderService.startRecording(fileName)

        if (filePath != null) {
            currentRecordingFileName = fileName
            recordingStartTime = System.currentTimeMillis()
            _isRecording.value = true
            _recordingDuration.value = 0L
            
            // Mettre à jour la durée d'enregistrement
            viewModelScope.launch {
                while (_isRecording.value) {
                    _recordingDuration.value = System.currentTimeMillis() - recordingStartTime
                    kotlinx.coroutines.delay(100)
                }
            }
        } else {
            _error.value = "Impossible de démarrer l'enregistrement"
        }
    }

    /**
     * Arrête l'enregistrement et sauvegarde la note
     */
    fun stopRecording(songId: String, groupId: String, userId: String, title: String = "") {
        if (!_isRecording.value) return

        val filePath = recorderService.stopRecording()
        _isRecording.value = false

        if (filePath != null) {
            val duration = _recordingDuration.value
            val audioNote = AudioNote(
                id = UUID.randomUUID().toString(),
                songId = songId,
                groupId = groupId,
                userId = userId,
                title = title.ifEmpty { "Note audio ${_audioNotes.value.size + 1}" },
                localFilePath = filePath,
                duration = (duration / 1000).toInt(),
                createdAt = System.currentTimeMillis()
            )

            viewModelScope.launch {
                repository.saveAudioNote(audioNote).fold(
                    onSuccess = {
                        // Recharger les notes
                        loadAudioNotes(songId)
                    },
                    onFailure = { exception ->
                        _error.value = exception.message ?: "Erreur lors de la sauvegarde"
                    }
                )
            }
        } else {
            _error.value = "Erreur lors de l'enregistrement"
        }

        _recordingDuration.value = 0L
        currentRecordingFileName = null
    }

    /**
     * Annule l'enregistrement en cours
     */
    fun cancelRecording() {
        if (!_isRecording.value) return

        recorderService.cancelRecording()
        _isRecording.value = false
        _recordingDuration.value = 0L
        currentRecordingFileName = null
    }

    /**
     * Démarre la lecture d'une note audio
     */
    fun startPlaying(noteId: String) {
        val note = _audioNotes.value.find { it.id == noteId } ?: return

        // Arrêter la lecture en cours si nécessaire
        if (_isPlaying.value) {
            stopPlaying()
        }

        val success = playerService.startPlaying(note.localFilePath)
        if (success) {
            _isPlaying.value = true
            _currentPlayingNoteId.value = noteId
            _playbackDuration.value = playerService.getDuration()
            
            // Mettre à jour la position de lecture
            viewModelScope.launch {
                while (_isPlaying.value && _currentPlayingNoteId.value == noteId) {
                    _playbackPosition.value = playerService.getCurrentPosition()
                    kotlinx.coroutines.delay(100)
                }
            }
        } else {
            _error.value = "Impossible de lire le fichier audio"
        }
    }

    /**
     * Arrête la lecture
     */
    fun stopPlaying() {
        playerService.stopPlaying()
        _isPlaying.value = false
        _currentPlayingNoteId.value = null
        _playbackPosition.value = 0
        _playbackDuration.value = 0
    }

    /**
     * Met en pause la lecture
     */
    fun pausePlaying() {
        playerService.pausePlaying()
        _isPlaying.value = false
    }

    /**
     * Reprend la lecture
     */
    fun resumePlaying() {
        playerService.resumePlaying()
        _isPlaying.value = true
    }

    /**
     * Change la position de lecture
     */
    fun seekTo(position: Int) {
        playerService.seekTo(position)
        _playbackPosition.value = position
    }

    /**
     * Supprime une note audio
     */
    fun deleteAudioNote(noteId: String, songId: String) {
        viewModelScope.launch {
            // Arrêter la lecture si c'est cette note qui est en cours
            if (_currentPlayingNoteId.value == noteId) {
                stopPlaying()
            }

            repository.deleteAudioNote(noteId).fold(
                onSuccess = {
                    // Recharger les notes
                    loadAudioNotes(songId)
                },
                onFailure = { exception ->
                    _error.value = exception.message ?: "Erreur lors de la suppression"
                }
            )
        }
    }

    /**
     * Met à jour le titre d'une note audio
     */
    fun updateNoteTitle(noteId: String, newTitle: String, songId: String) {
        viewModelScope.launch {
            val note = _audioNotes.value.find { it.id == noteId } ?: return@launch
            val updatedNote = note.copy(title = newTitle)

            repository.updateAudioNote(updatedNote).fold(
                onSuccess = {
                    loadAudioNotes(songId)
                },
                onFailure = { exception ->
                    _error.value = exception.message ?: "Erreur lors de la mise à jour"
                }
            )
        }
    }

    /**
     * Efface le message d'erreur
     */
    fun clearError() {
        _error.value = null
    }

    /**
     * Analyse une note audio
     */
    fun analyzeAudio(note: AudioNote) {
        viewModelScope.launch {
            _isAnalyzing.value = true
            
            analysisService.analyzeKey(note.localFilePath).fold(
                onSuccess = { result ->
                    val currentMap = _analysisResults.value.toMutableMap()
                    currentMap[note.id] = result
                    _analysisResults.value = currentMap
                    _isAnalyzing.value = false
                    
                    // Calculer la transposition si possible
                    calculateTransposition(note.groupId, note.songId, note.id, result.rootNote)
                },
                onFailure = { e ->
                    _error.value = "Analyse échouée : ${e.message}"
                    _isAnalyzing.value = false
                }
            )
        }
    }

    // Message de transposition : Map<NoteId, String>
    private val _transpositionSuggestions = MutableStateFlow<Map<String, String>>(emptyMap())
    val transpositionSuggestions: StateFlow<Map<String, String>> = _transpositionSuggestions.asStateFlow()

    private fun calculateTransposition(groupId: String, songId: String, noteId: String, detectedRoot: String) {
        viewModelScope.launch {
            songRepository.getSong(groupId, songId).onSuccess { song ->
                 val originalKey = song.key
                 if (!originalKey.isNullOrEmpty()) {
                     val suggestion = com.bandtrack.utils.TranspositionHelper.getTranspositionSuggestion(originalKey, detectedRoot)
                     
                     val currentMap = _transpositionSuggestions.value.toMutableMap()
                     currentMap[noteId] = suggestion
                     _transpositionSuggestions.value = currentMap
                 }
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        recorderService.release()
        playerService.release()
    }
}
