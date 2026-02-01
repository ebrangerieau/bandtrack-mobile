package com.bandtrack.ui.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.bandtrack.data.repository.PerformanceRepository
import com.bandtrack.data.repository.SongRepository

class PerformanceViewModelFactory(
    private val performanceRepository: PerformanceRepository,
    private val songRepository: SongRepository
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(PerformanceViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return PerformanceViewModel(performanceRepository, songRepository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
