package com.bandtrack.ui.repertoire

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.bandtrack.data.models.Song
import com.bandtrack.data.repository.SongRepository
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

/**
 * États possibles de l'écran du répertoire
 */
sealed class RepertoireUiState {
    object Loading : RepertoireUiState()
    data class Success(val songs: List<Song>) : RepertoireUiState()
    data class Error(val message: String) : RepertoireUiState()
}

/**
 * ViewModel pour la gestion du répertoire
 */
class RepertoireViewModel(
    private val songRepository: SongRepository = SongRepository()
) : ViewModel() {

    private val _uiState = MutableStateFlow<RepertoireUiState>(RepertoireUiState.Loading)
    val uiState: StateFlow<RepertoireUiState> = _uiState.asStateFlow()

    private var currentGroupId: String = ""
    private var currentUserId: String = ""

    /**
     * Initialiser avec un groupe
     */
    fun initialize(groupId: String, userId: String) {
        currentGroupId = groupId
        currentUserId = userId
        observeSongs()
    }

    /**
     * Observer les morceaux en temps réel
     */
    private fun observeSongs() {
        viewModelScope.launch {
            songRepository.observeGroupSongs(currentGroupId)
                .catch { e ->
                    _uiState.value = RepertoireUiState.Error(
                        e.message ?: "Erreur lors du chargement du répertoire"
                    )
                }
                .collect { songs ->
                    _uiState.value = RepertoireUiState.Success(songs)
                }
        }
    }

    /**
     * Créer un nouveau morceau
     */
    fun createSong(
        title: String,
        artist: String,
        duration: Int = 0,
        structure: String = "",
        key: String? = null,
        tempo: Int? = null,
        notes: String = "",
        link: String? = null
    ) {
        viewModelScope.launch {
            val song = Song(
                title = title,
                artist = artist,
                duration = duration,
                structure = structure,
                key = key,
                tempo = tempo,
                notes = notes,
                link = link
            )

            val result = songRepository.createSong(
                groupId = currentGroupId,
                song = song,
                userId = currentUserId
            )

            if (result.isFailure) {
                _uiState.value = RepertoireUiState.Error(
                    result.exceptionOrNull()?.message ?: "Erreur lors de la création"
                )
            }
        }
    }

    /**
     * Mettre à jour le niveau de maîtrise de l'utilisateur actuel
     */
    fun updateMyMasteryLevel(songId: String, level: Int) {
        viewModelScope.launch {
            val result = songRepository.updateMasteryLevel(
                groupId = currentGroupId,
                songId = songId,
                userId = currentUserId,
                level = level
            )

            if (result.isFailure) {
                _uiState.value = RepertoireUiState.Error(
                    result.exceptionOrNull()?.message ?: "Erreur lors de la mise à jour"
                )
            }
        }
    }

    /**
     * Mettre à jour les informations d'un morceau
     */
    fun updateSong(songId: String, updates: Map<String, Any>) {
        viewModelScope.launch {
            val result = songRepository.updateSong(
                groupId = currentGroupId,
                songId = songId,
                updates = updates
            )

            if (result.isFailure) {
                _uiState.value = RepertoireUiState.Error(
                    result.exceptionOrNull()?.message ?: "Erreur lors de la mise à jour"
                )
            }
        }
    }

    /**
     * Supprimer un morceau
     */
    fun deleteSong(songId: String) {
        viewModelScope.launch {
            val result = songRepository.deleteSong(
                groupId = currentGroupId,
                songId = songId
            )

            if (result.isFailure) {
                _uiState.value = RepertoireUiState.Error(
                    result.exceptionOrNull()?.message ?: "Erreur lors de la suppression"
                )
            }
        }
    }

    /**
     * Filtrer les morceaux bien maîtrisés (moyenne >= 7)
     */
    fun getWellMasteredSongs(): List<Song> {
        val currentState = _uiState.value
        return if (currentState is RepertoireUiState.Success) {
            currentState.songs.filter { it.isWellMastered() }
        } else {
            emptyList()
        }
    }
}
