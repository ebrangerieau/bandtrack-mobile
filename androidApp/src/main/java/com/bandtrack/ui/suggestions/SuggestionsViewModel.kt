package com.bandtrack.ui.suggestions

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.bandtrack.data.models.Suggestion
import com.bandtrack.data.repository.SuggestionRepository
import com.bandtrack.data.repository.SongRepository
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

/**
 * États possibles de l'écran des suggestions
 */
sealed class SuggestionsUiState {
    object Loading : SuggestionsUiState()
    data class Success(val suggestions: List<Suggestion>) : SuggestionsUiState()
    data class Error(val message: String) : SuggestionsUiState()
}

/**
 * ViewModel pour la gestion des suggestions
 */
class SuggestionsViewModel(
    private val suggestionRepository: SuggestionRepository = SuggestionRepository(),
    private val songRepository: SongRepository = SongRepository()
) : ViewModel() {

    private val _uiState = MutableStateFlow<SuggestionsUiState>(SuggestionsUiState.Loading)
    val uiState: StateFlow<SuggestionsUiState> = _uiState.asStateFlow()

    private var currentGroupId: String = ""
    private var currentUserId: String = ""
    private var currentUserName: String = ""

    /**
     * Initialiser avec un groupe
     */
    fun initialize(groupId: String, userId: String, userName: String) {
        currentGroupId = groupId
        currentUserId = userId
        currentUserName = userName
        observeSuggestions()
    }

    /**
     * Observer les suggestions en temps réel
     */
    private fun observeSuggestions() {
        viewModelScope.launch {
            suggestionRepository.observeGroupSuggestions(currentGroupId)
                .catch { e ->
                    _uiState.value = SuggestionsUiState.Error(
                        e.message ?: "Erreur lors du chargement des suggestions"
                    )
                }
                .collect { suggestions ->
                    _uiState.value = SuggestionsUiState.Success(suggestions)
                }
        }
    }

    /**
     * Créer une nouvelle suggestion
     */
    fun createSuggestion(title: String, artist: String, link: String?) {
        viewModelScope.launch {
            val result = suggestionRepository.createSuggestion(
                groupId = currentGroupId,
                title = title,
                artist = artist,
                link = link,
                userId = currentUserId,
                userName = currentUserName
            )

            if (result.isFailure) {
                _uiState.value = SuggestionsUiState.Error(
                    result.exceptionOrNull()?.message ?: "Erreur lors de la création"
                )
            }
            // Le listener temps réel mettra à jour automatiquement la liste
        }
    }

    /**
     * Voter pour une suggestion
     */
    fun toggleVote(suggestionId: String) {
        viewModelScope.launch {
            val result = suggestionRepository.toggleVote(
                groupId = currentGroupId,
                suggestionId = suggestionId,
                userId = currentUserId
            )

            if (result.isFailure) {
                _uiState.value = SuggestionsUiState.Error(
                    result.exceptionOrNull()?.message ?: "Erreur lors du vote"
                )
            }
        }
    }

    /**
     * Convertir une suggestion en morceau du répertoire
     */
    fun convertToSong(suggestion: Suggestion) {
        viewModelScope.launch {
            // 1. Créer le morceau
            val songResult = songRepository.createSongFromSuggestion(
                groupId = currentGroupId,
                suggestion = suggestion,
                userId = currentUserId
            )

            if (songResult.isFailure) {
                _uiState.value = SuggestionsUiState.Error(
                    songResult.exceptionOrNull()?.message ?: "Erreur lors de la conversion"
                )
                return@launch
            }

            val songId = songResult.getOrNull()!!

            // 2. Marquer la suggestion comme acceptée
            val acceptResult = suggestionRepository.acceptSuggestion(
                groupId = currentGroupId,
                suggestionId = suggestion.id,
                convertedToSongId = songId
            )

            if (acceptResult.isFailure) {
                _uiState.value = SuggestionsUiState.Error(
                    acceptResult.exceptionOrNull()?.message ?: "Erreur lors de l'acceptation"
                )
            }
        }
    }

    /**
     * Supprimer une suggestion
     */
    fun deleteSuggestion(suggestionId: String) {
        viewModelScope.launch {
            val result = suggestionRepository.deleteSuggestion(
                groupId = currentGroupId,
                suggestionId = suggestionId
            )

            if (result.isFailure) {
                _uiState.value = SuggestionsUiState.Error(
                    result.exceptionOrNull()?.message ?: "Erreur lors de la suppression"
                )
            }
        }
    }
}

class SuggestionsViewModelFactory(
    private val songRepository: SongRepository
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(SuggestionsViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return SuggestionsViewModel(
                songRepository = songRepository
            ) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
