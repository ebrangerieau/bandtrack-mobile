package com.bandtrack.ui.viewmodels

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.bandtrack.data.repository.AudioNoteRepository

class AudioNoteViewModelFactory(
    private val context: Context,
    private val repository: AudioNoteRepository
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(AudioNoteViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return AudioNoteViewModel(context, repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
