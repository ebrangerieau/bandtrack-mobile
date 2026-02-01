package com.bandtrack.ui.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.bandtrack.data.models.User
import com.bandtrack.data.repository.AuthRepository
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

/**
 * États possibles de l'écran d'authentification
 */
sealed class AuthUiState {
    object Initial : AuthUiState()
    object Loading : AuthUiState()
    data class Success(val user: User) : AuthUiState()
    data class Error(val message: String) : AuthUiState()
}

/**
 * ViewModel pour l'authentification
 */
class AuthViewModel(
    private val authRepository: AuthRepository = AuthRepository()
) : ViewModel() {

    private val _uiState = MutableStateFlow<AuthUiState>(AuthUiState.Initial)
    val uiState: StateFlow<AuthUiState> = _uiState.asStateFlow()

    /**
     * Observable de l'utilisateur connecté
     */
    val currentUser: StateFlow<User?> = authRepository.authStateFlow
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = null
        )

    /**
     * Inscription avec email et mot de passe
     */
    fun signUp(email: String, password: String, displayName: String) {
        viewModelScope.launch {
            _uiState.value = AuthUiState.Loading
            
            val result = authRepository.signUp(email, password, displayName)
            
            _uiState.value = if (result.isSuccess) {
                AuthUiState.Success(result.getOrNull()!!)
            } else {
                AuthUiState.Error(result.exceptionOrNull()?.message ?: "Erreur lors de l'inscription")
            }
        }
    }

    /**
     * Connexion avec email et mot de passe
     */
    fun signIn(email: String, password: String) {
        viewModelScope.launch {
            _uiState.value = AuthUiState.Loading
            
            val result = authRepository.signIn(email, password)
            
            _uiState.value = if (result.isSuccess) {
                AuthUiState.Success(result.getOrNull()!!)
            } else {
                AuthUiState.Error(result.exceptionOrNull()?.message ?: "Erreur lors de la connexion")
            }
        }
    }

    /**
     * Déconnexion
     */
    fun signOut() {
        authRepository.signOut()
        _uiState.value = AuthUiState.Initial
    }

    /**
     * Envoyer un email de réinitialisation
     */
    fun sendPasswordResetEmail(email: String) {
        viewModelScope.launch {
            _uiState.value = AuthUiState.Loading
            
            val result = authRepository.sendPasswordResetEmail(email)
            
            _uiState.value = if (result.isSuccess) {
                AuthUiState.Initial
            } else {
                AuthUiState.Error(result.exceptionOrNull()?.message ?: "Erreur lors de l'envoi de l'email")
            }
        }
    }

    /**
     * Réinitialiser l'état
     */
    fun resetState() {
        _uiState.value = AuthUiState.Initial
    }

    /**
     * Vérifier si un utilisateur est déjà connecté
     */
    fun isUserSignedIn(): Boolean = authRepository.isUserSignedIn()
}
