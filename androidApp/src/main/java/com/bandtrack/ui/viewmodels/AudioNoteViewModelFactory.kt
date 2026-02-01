package com.bandtrack.ui.viewmodels

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.bandtrack.data.repository.AudioNoteRepository
import com.bandtrack.data.repository.SongRepository

class AudioNoteViewModelFactory(
    private val context: Context,
    private val repository: AudioNoteRepository,
    private val songRepository: SongRepository
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(AudioNoteViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return AudioNoteViewModel(context, repository, songRepository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
