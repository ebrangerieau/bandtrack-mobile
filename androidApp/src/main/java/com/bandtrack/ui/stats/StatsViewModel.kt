package com.bandtrack.ui.stats

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.bandtrack.data.models.Song
import com.bandtrack.data.repository.SongRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class GroupStats(
    val totalSongs: Int = 0,
    val globalMastery: Float = 0f,
    val bestSongs: List<Song> = emptyList(),
    val worstSongs: List<Song> = emptyList()
)

class StatsViewModel(
    private val songRepository: SongRepository = SongRepository()
) : ViewModel() {
    private val _stats = MutableStateFlow(GroupStats())
    val stats: StateFlow<GroupStats> = _stats.asStateFlow()

    fun loadStats(groupId: String) {
        viewModelScope.launch {
            songRepository.getGroupSongs(groupId).onSuccess { songs ->
                val total = songs.size
                // Calcul de la moyenne des moyennes de chaque chanson
                val globalAvg = if (total > 0) {
                    songs.map { it.getAverageMasteryLevel() }.average().toFloat()
                } else {
                    0f
                }
                
                // Trier par maîtrise
                val sortedByMastery = songs.sortedByDescending { it.getAverageMasteryLevel() }
                
                _stats.value = GroupStats(
                    totalSongs = total,
                    globalMastery = globalAvg,
                    bestSongs = sortedByMastery.take(3),
                    // On prend les pires seulement s'ils ont une maîtrise définie (moyenne > 0) 
                    // ou alors on considère 0 comme pire. Disons qu'on affiche tout.
                    worstSongs = sortedByMastery.takeLast(3).reversed() 
                )
            }
        }
    }
}
