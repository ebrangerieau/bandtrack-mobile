package com.bandtrack.data.repository

import com.bandtrack.data.models.Group
import com.bandtrack.data.models.GroupMember
import com.bandtrack.data.models.InvitationCode
import com.bandtrack.data.remote.FirestoreService
import kotlinx.coroutines.flow.Flow

/**
 * Repository pour la gestion des groupes
 */
class GroupRepository(
    private val firestoreService: FirestoreService = FirestoreService()
) {

    /**
     * Créer un nouveau groupe
     */
    suspend fun createGroup(
        name: String,
        description: String,
        creatorUserId: String,
        creatorDisplayName: String
    ): Result<String> {
        val group = Group(
            name = name,
            description = description,
            createdBy = creatorUserId,
            memberCount = 1
        )
        
        return firestoreService.createGroup(group, creatorUserId)
    }

    /**
     * Récupérer un groupe par ID
     */
    suspend fun getGroup(groupId: String): Result<Group> {
        return firestoreService.getGroup(groupId)
    }

    /**
     * Observer un groupe en temps réel
     */
    fun observeGroup(groupId: String): Flow<Group?> {
        return firestoreService.observeGroup(groupId)
    }

    /**
     * Récupérer les groupes d'un utilisateur
     */
    suspend fun getUserGroups(userId: String): Result<List<Group>> {
        return firestoreService.getUserGroups(userId)
    }

    /**
     * Récupérer les membres d'un groupe
     */
    suspend fun getGroupMembers(groupId: String): Result<List<GroupMember>> {
        return firestoreService.getGroupMembers(groupId)
    }

    /**
     * Ajouter un membre à un groupe
     */
    suspend fun addMember(
        groupId: String,
        userId: String,
        displayName: String,
        instrument: String? = null
    ): Result<Unit> {
        val member = GroupMember(
            userId = userId,
            groupId = groupId,
            displayName = displayName,
            instrument = instrument
        )
        
        return firestoreService.addGroupMember(groupId, member)
    }

    /**
     * Créer un code d'invitation
     */
    suspend fun createInvitationCode(
        groupId: String,
        creatorUserId: String,
        expiresInDays: Int? = null,
        maxUses: Int = 0
    ): Result<InvitationCode> {
        val code = InvitationCode.generateCode()
        val expiresAt = expiresInDays?.let {
            System.currentTimeMillis() + (it * 24 * 60 * 60 * 1000L)
        } ?: 0L
        
        val invitation = InvitationCode(
            groupId = groupId,
            code = code,
            createdBy = creatorUserId,
            expiresAt = expiresAt,
            maxUses = maxUses
        )
        
        val result = firestoreService.createInvitationCode(invitation)
        
        return if (result.isSuccess) {
            val invitationId = result.getOrNull()!!
            Result.success(invitation.copy(id = invitationId))
        } else {
            Result.failure(result.exceptionOrNull()!!)
        }
    }

    /**
     * Rejoindre un groupe avec un code d'invitation
     */
    suspend fun joinGroupWithCode(
        code: String,
        userId: String,
        displayName: String
    ): Result<String> {
        // 1. Trouver l'invitation
        val invitationResult = firestoreService.getInvitationByCode(code)
        if (invitationResult.isFailure) {
            return Result.failure(invitationResult.exceptionOrNull()!!)
        }
        
        val invitation = invitationResult.getOrNull()
            ?: return Result.failure(Exception("Code d'invitation invalide"))
        
        if (!invitation.canBeUsed()) {
            return Result.failure(Exception("Ce code d'invitation n'est plus valide"))
        }
        
        // 2. Vérifier si l'utilisateur n'est pas déjà membre
        val isMemberResult = firestoreService.isGroupMember(invitation.groupId, userId)
        if (isMemberResult.isSuccess && isMemberResult.getOrNull() == true) {
            return Result.failure(Exception("Vous êtes déjà membre de ce groupe"))
        }
        
        // 3. Ajouter l'utilisateur comme membre
        val addMemberResult = addMember(invitation.groupId, userId, displayName)
        if (addMemberResult.isFailure) {
            return Result.failure(addMemberResult.exceptionOrNull()!!)
        }
        
        // 4. Incrémenter le compteur d'utilisation
        firestoreService.useInvitationCode(invitation.id, invitation.groupId)
        
        return Result.success(invitation.groupId)
    }
}
