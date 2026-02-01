package com.bandtrack.ui.groups

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.bandtrack.data.models.Group
import com.bandtrack.data.models.InvitationCode
import com.bandtrack.data.repository.AuthRepository
import com.bandtrack.data.repository.GroupRepository
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

/**
 * États de l'écran des groupes
 */
sealed class GroupUiState {
    object Loading : GroupUiState()
    data class Success(val groups: List<Group>) : GroupUiState()
    data class Error(val message: String) : GroupUiState()
}

/**
 * ViewModel pour la sélection et gestion des groupes
 */
class GroupSelectorViewModel(
    private val authRepository: AuthRepository = AuthRepository(),
    private val groupRepository: GroupRepository = GroupRepository()
) : ViewModel() {

    private val _uiState = MutableStateFlow<GroupUiState>(GroupUiState.Loading)
    val uiState: StateFlow<GroupUiState> = _uiState.asStateFlow()

    private val _selectedGroup = MutableStateFlow<Group?>(null)
    val selectedGroup: StateFlow<Group?> = _selectedGroup.asStateFlow()

    private val _invitationCode = MutableStateFlow<InvitationCode?>(null)
    val invitationCode: StateFlow<InvitationCode?> = _invitationCode.asStateFlow()

    init {
        loadUserGroups()
    }

    /**
     * Charger les groupes de l'utilisateur
     */
    fun loadUserGroups() {
        viewModelScope.launch {
            val userId = authRepository.currentUserId
            if (userId == null) {
                _uiState.value = GroupUiState.Error("Utilisateur non connecté")
                return@launch
            }

            _uiState.value = GroupUiState.Loading

            val result = groupRepository.getUserGroups(userId)

            _uiState.value = if (result.isSuccess) {
                val groups = result.getOrNull() ?: emptyList()
                GroupUiState.Success(groups)
            } else {
                GroupUiState.Error(result.exceptionOrNull()?.message ?: "Erreur lors du chargement des groupes")
            }
        }
    }

    /**
     * Sélectionner un groupe
     */
    fun selectGroup(group: Group) {
        _selectedGroup.value = group
    }

    /**
     * Créer un nouveau groupe
     */
    fun createGroup(name: String, description: String, creatorDisplayName: String) {
        viewModelScope.launch {
            val userId = authRepository.currentUserId
            if (userId == null) {
                _uiState.value = GroupUiState.Error("Utilisateur non connecté")
                return@launch
            }

            _uiState.value = GroupUiState.Loading

            val result = groupRepository.createGroup(name, description, userId, creatorDisplayName)

            if (result.isSuccess) {
                // Recharger les groupes
                loadUserGroups()
            } else {
                _uiState.value = GroupUiState.Error(
                    result.exceptionOrNull()?.message ?: "Erreur lors de la création du groupe"
                )
            }
        }
    }

    /**
     * Générer un code d'invitation pour le groupe sélectionné
     */
    fun generateInvitationCode(expiresInDays: Int? = 7, maxUses: Int = 0) {
        viewModelScope.launch {
            val group = _selectedGroup.value
            val userId = authRepository.currentUserId

            if (group == null || userId == null) {
                return@launch
            }

            val result = groupRepository.createInvitationCode(
                groupId = group.id,
                creatorUserId = userId,
                expiresInDays = expiresInDays,
                maxUses = maxUses
            )

            if (result.isSuccess) {
                _invitationCode.value = result.getOrNull()
            }
        }
    }

    /**
     * Rejoindre un groupe avec un code
     */
    fun joinGroupWithCode(code: String, displayName: String) {
        viewModelScope.launch {
            val userId = authRepository.currentUserId
            if (userId == null) {
                _uiState.value = GroupUiState.Error("Utilisateur non connecté")
                return@launch
            }

            _uiState.value = GroupUiState.Loading

            val result = groupRepository.joinGroupWithCode(code, userId, displayName)

            if (result.isSuccess) {
                // Recharger les groupes
                loadUserGroups()
            } else {
                _uiState.value = GroupUiState.Error(
                    result.exceptionOrNull()?.message ?: "Erreur lors de l'adhésion au groupe"
                )
            }
        }
    }

    /**
     * Réinitialiser le code d'invitation
     */
    fun clearInvitationCode() {
        _invitationCode.value = null
    }
}
