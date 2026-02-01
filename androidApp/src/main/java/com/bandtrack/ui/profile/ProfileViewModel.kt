package com.bandtrack.ui.profile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.bandtrack.data.models.User
import com.bandtrack.data.repository.AuthRepository
import com.bandtrack.data.repository.SongRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.datetime.Instant
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime

class ProfileViewModel(
    private val authRepository: AuthRepository,
    private val songRepository: SongRepository? = null // Optional for stats
) : ViewModel() {

    private val _user = MutableStateFlow<User?>(null)
    val user: StateFlow<User?> = _user.asStateFlow()

    private val _songsAddedCount = MutableStateFlow(0)
    val songsAddedCount: StateFlow<Int> = _songsAddedCount.asStateFlow()

    init {
        // Observer l'utilisateur courant
        viewModelScope.launch {
            authRepository.authStateFlow.collect { currentUser ->
                _user.value = currentUser
                // Si on voulait charger des stats, on le ferait ici
            }
        }
    }

    fun signOut() {
        authRepository.signOut()
    }

    fun getFormattedJoinDate(timestamp: Long): String {
        if (timestamp == 0L) return "Inconnue"
        try {
            val date = Instant.fromEpochMilliseconds(timestamp)
                .toLocalDateTime(TimeZone.currentSystemDefault())
            return "${date.dayOfMonth}/${date.monthNumber}/${date.year}"
        } catch (e: Exception) {
            return "Inconnue"
        }
    }
    
    // Placeholder pour mise à jour du nom (nécessiterait une méthode updateProfile dans AuthRepository)
    fun updateDisplayName(newName: String) {
        // TODO: Implémenter updateProfile dans AuthRepository
    }
}
