package com.bandtrack.ui.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.bandtrack.data.models.Performance
import com.bandtrack.data.models.PerformanceType
import com.bandtrack.data.models.Song
import com.bandtrack.data.repository.PerformanceRepository
import com.bandtrack.data.repository.SongRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.launch

sealed class PerformanceUiState {
    data object Loading : PerformanceUiState()
    data class Success(
        val performances: List<Performance>,
        val pastPerformances: List<Performance>,
        val upcomingPerformances: List<Performance>
    ) : PerformanceUiState()
    data class Error(val message: String) : PerformanceUiState()
}

class PerformanceViewModel(
    private val performanceRepository: PerformanceRepository = PerformanceRepository(),
    private val songRepository: SongRepository = SongRepository()
) : ViewModel() {

    private val _uiState = MutableStateFlow<PerformanceUiState>(PerformanceUiState.Loading)
    val uiState: StateFlow<PerformanceUiState> = _uiState.asStateFlow()

    // Cache des chansons pour l'affichage des setlists
    private val _songsCache = MutableStateFlow<Map<String, Song>>(emptyMap())
    val songsCache: StateFlow<Map<String, Song>> = _songsCache.asStateFlow()

    fun initialize(groupId: String) {
        viewModelScope.launch {
            // Charger les chansons pour le cache
            launch {
                songRepository.observeGroupSongs(groupId).collect { songs ->
                    _songsCache.value = songs.associateBy { it.id }
                }
            }

            // Observer les performances
            performanceRepository.observeGroupPerformances(groupId)
                .catch { e ->
                    _uiState.value = PerformanceUiState.Error(e.message ?: "Erreur inconnue")
                }
                .collect { performances ->
                    val now = System.currentTimeMillis()
                    val (upcoming, past) = performances.partition { 
                        it.date + (it.durationMinutes * 60 * 1000) > now 
                    }
                    
                    _uiState.value = PerformanceUiState.Success(
                        performances = performances,
                        pastPerformances = past.sortedByDescending { it.date }, // Les plus récents d'abord
                        upcomingPerformances = upcoming // Les plus proches d'abord (déjà trié par repo)
                    )
                }
        }
    }

    fun createPerformance(
        groupId: String,
        type: PerformanceType,
        date: Long,
        time: Long, // Heure en ms depuis le début de la journée, ou timestamp complet
        durationMinutes: Int,
        location: String,
        title: String,
        notes: String,
        userId: String
    ) {
        viewModelScope.launch {
            // Combiner date et heure si nécessaire. Supposons que 'date' soit le timestamp complet
            // Ou que date est le jour à 00:00 et time est l'offset.
            // Pour simplifier, supposons que 'date' passé ici est le timestamp final complet
            
            val performance = Performance(
                groupId = groupId,
                type = type,
                date = date,
                durationMinutes = durationMinutes,
                location = location,
                title = title,
                notes = notes,
                createdBy = userId
            )

            performanceRepository.createPerformance(performance)
                .onFailure { e ->
                    // Gérer l'erreur (via un event ou state temporaire)
                    // _uiState.value = PerformanceUiState.Error(e.message ?: "Erreur lors de la création")
                }
        }
    }

    fun deletePerformance(groupId: String, performanceId: String) {
        viewModelScope.launch {
            performanceRepository.deletePerformance(groupId, performanceId)
        }
    }

    fun updateSetlist(groupId: String, performanceId: String, songIds: List<String>) {
        viewModelScope.launch {
            performanceRepository.updateSetlist(groupId, performanceId, songIds)
        }
    }

    fun addToSetlist(groupId: String, performanceId: String, songId: String) {
        val currentState = _uiState.value
        if (currentState is PerformanceUiState.Success) {
            val perf = currentState.performances.find { it.id == performanceId } ?: return
            val newSetlist = perf.setlist + songId
            updateSetlist(groupId, performanceId, newSetlist)
        }
    }
    
    fun removeFromSetlist(groupId: String, performanceId: String, songId: String) {
        val currentState = _uiState.value
        if (currentState is PerformanceUiState.Success) {
            val perf = currentState.performances.find { it.id == performanceId } ?: return
            val newSetlist = perf.setlist - songId
            updateSetlist(groupId, performanceId, newSetlist)
        }
    }

    fun reorderSetlist(groupId: String, performanceId: String, fromIndex: Int, toIndex: Int) {
        val currentState = _uiState.value
        if (currentState is PerformanceUiState.Success) {
            val perf = currentState.performances.find { it.id == performanceId } ?: return
            val mutableSetlist = perf.setlist.toMutableList()
            val item = mutableSetlist.removeAt(fromIndex)
            mutableSetlist.add(toIndex, item)
            updateSetlist(groupId, performanceId, mutableSetlist)
        }
    }
}
