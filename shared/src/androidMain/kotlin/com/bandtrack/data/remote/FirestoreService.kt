package com.bandtrack.data.remote

import com.bandtrack.data.models.*
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow

/**
 * Service Firestore pour la gestion des données utilisateur et groupes
 */
class FirestoreService {
    private val db: FirebaseFirestore = FirebaseFirestore.getInstance()

    // Collections Firestore
    private val usersCollection = db.collection("users")
    private val groupsCollection = db.collection("groups")

    // ---------------------------
    // USERS
    // ---------------------------

    /**
     * Créer ou mettre à jour un utilisateur
     */
    suspend fun saveUser(user: User): Result<Unit> = try {
        usersCollection.document(user.id).set(user).await()
        Result.success(Unit)
    } catch (e: Exception) {
        Result.failure(e)
    }

    /**
     * Récupérer un utilisateur par ID
     */
    suspend fun getUser(userId: String): Result<User> = try {
        val snapshot = usersCollection.document(userId).get().await()
        val user = snapshot.toObject(User::class.java) ?: User.empty()
        Result.success(user)
    } catch (e: Exception) {
        Result.failure(e)
    }

    /**
     * Supprimer un document utilisateur
     */
    suspend fun deleteUser(userId: String): Result<Unit> = try {
        usersCollection.document(userId).delete().await()
        Result.success(Unit)
    } catch (e: Exception) {
        Result.failure(e)
    }

    /**
     * Observer les changements d'un utilisateur en temps réel
     */
    fun observeUser(userId: String): Flow<User?> = callbackFlow {
        val listener = usersCollection.document(userId)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }
                val user = snapshot?.toObject(User::class.java)
                trySend(user)
            }
        awaitClose { listener.remove() }
    }

    // ---------------------------
    // GROUPS
    // ---------------------------

    /**
     * Créer un nouveau groupe
     */
    suspend fun createGroup(group: Group, creatorUserId: String): Result<String> = try {
        // Créer le groupe
        val groupRef = groupsCollection.document()
        val groupWithId = group.copy(
            id = groupRef.id, 
            createdBy = creatorUserId,
            memberIds = listOf(creatorUserId),
            memberCount = 1
        )
        groupRef.set(groupWithId).await()

        // Ajouter le créateur comme admin
        val member = GroupMember(
            userId = creatorUserId,
            groupId = groupRef.id,
            role = GroupRole.ADMIN.name
        )
        groupRef.collection("members").document(creatorUserId).set(member).await()

        Result.success(groupRef.id)
    } catch (e: Exception) {
        Result.failure(e)
    }

    /**
     * Récupérer un groupe par ID
     */
    suspend fun getGroup(groupId: String): Result<Group> = try {
        val snapshot = groupsCollection.document(groupId).get().await()
        val group = snapshot.toObject(Group::class.java) ?: Group.empty()
        Result.success(group)
    } catch (e: Exception) {
        Result.failure(e)
    }

    /**
     * Observer un groupe en temps réel
     */
    fun observeGroup(groupId: String): Flow<Group?> = callbackFlow {
        val listener = groupsCollection.document(groupId)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }
                val group = snapshot?.toObject(Group::class.java)
                trySend(group)
            }
        awaitClose { listener.remove() }
    }

    /**
     * Récupérer les groupes d'un utilisateur
     */
    suspend fun getUserGroups(userId: String): Result<List<Group>> = try {
        val snapshot = groupsCollection
            .whereArrayContains("memberIds", userId)
            .get()
            .await()
        
        val groups = snapshot.documents.mapNotNull { 
            it.toObject(Group::class.java) 
        }
        
        Result.success(groups)
    } catch (e: Exception) {
        Result.failure(e)
    }

    /**
     * Ajouter un groupId à la liste des groupes de l'utilisateur
     */
    suspend fun addGroupToUser(userId: String, groupId: String): Result<Unit> = try {
        usersCollection.document(userId).update(
            "groupIds", FieldValue.arrayUnion(groupId)
        ).await()
        Result.success(Unit)
    } catch (e: Exception) {
        Result.failure(e)
    }

    /**
     * Retirer un groupId de la liste des groupes de l'utilisateur
     */
    suspend fun removeGroupFromUser(userId: String, groupId: String): Result<Unit> = try {
        usersCollection.document(userId).update(
            "groupIds", FieldValue.arrayRemove(groupId)
        ).await()
        Result.success(Unit)
    } catch (e: Exception) {
        Result.failure(e)
    }

    // ---------------------------
    // GROUP MEMBERS
    // ---------------------------

    /**
     * Ajouter un membre à un groupe
     */
    suspend fun addGroupMember(groupId: String, member: GroupMember): Result<Unit> = try {
        val batch = db.batch()
        
        // 1. Ajouter le membre dans la sous-collection
        val memberRef = groupsCollection
            .document(groupId)
            .collection("members")
            .document(member.userId)
        batch.set(memberRef, member)
        
        // 2. Mettre à jour le groupe (memberIds + compteur)
        val groupRef = groupsCollection.document(groupId)
        batch.update(groupRef, "memberIds", FieldValue.arrayUnion(member.userId))
        batch.update(groupRef, "memberCount", FieldValue.increment(1))
        
        batch.commit().await()
        
        Result.success(Unit)
    } catch (e: Exception) {
        Result.failure(e)
    }

    /**
     * Récupérer les membres d'un groupe
     */
    suspend fun getGroupMembers(groupId: String): Result<List<GroupMember>> = try {
        val snapshot = groupsCollection
            .document(groupId)
            .collection("members")
            .get()
            .await()
        
        val members = snapshot.documents.mapNotNull { 
            it.toObject(GroupMember::class.java) 
        }
        Result.success(members)
    } catch (e: Exception) {
        Result.failure(e)
    }

    /**
     * Vérifier si un utilisateur est membre d'un groupe
     */
    suspend fun isGroupMember(groupId: String, userId: String): Result<Boolean> = try {
        val doc = groupsCollection
            .document(groupId)
            .collection("members")
            .document(userId)
            .get()
            .await()
        Result.success(doc.exists())
    } catch (e: Exception) {
        Result.failure(e)
    }

    // ---------------------------
    // INVITATIONS
    // ---------------------------

    /**
     * Créer un code d'invitation
     */
    suspend fun createInvitationCode(invitation: InvitationCode): Result<String> = try {
        val invitationRef = groupsCollection
            .document(invitation.groupId)
            .collection("invitations")
            .document()
        
        val invitationWithId = invitation.copy(id = invitationRef.id)
        invitationRef.set(invitationWithId).await()
        
        Result.success(invitationRef.id)
    } catch (e: Exception) {
        Result.failure(e)
    }

    /**
     * Récupérer une invitation par code
     */
    suspend fun getInvitationByCode(code: String): Result<InvitationCode?> = try {
        // Optimisation : Collection Group Query
        val snapshot = db.collectionGroup("invitations")
            .whereEqualTo("code", code)
            .whereEqualTo("isActive", true)
            .get()
            .await()
        
        val invitation = if (!snapshot.isEmpty) {
            snapshot.documents.first().toObject(InvitationCode::class.java)
        } else {
            null
        }
        
        Result.success(invitation)
    } catch (e: Exception) {
        Result.failure(e)
    }

    /**
     * Utiliser un code d'invitation (incrémenter le compteur)
     */
    suspend fun useInvitationCode(invitationId: String, groupId: String): Result<Unit> = try {
        val invitationRef = groupsCollection
            .document(groupId)
            .collection("invitations")
            .document(invitationId)
        
        db.runTransaction { transaction ->
            val snapshot = transaction.get(invitationRef)
            val invitation = snapshot.toObject(InvitationCode::class.java)
            
            if (invitation != null && invitation.canBeUsed()) {
                transaction.update(invitationRef, "usedCount", invitation.usedCount + 1)
            } else {
                throw Exception("Invitation code is no longer valid")
            }
        }.await()
        
        Result.success(Unit)
    } catch (e: Exception) {
        Result.failure(e)
    }
}
