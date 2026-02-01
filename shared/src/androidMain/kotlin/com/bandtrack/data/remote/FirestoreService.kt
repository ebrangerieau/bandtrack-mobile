package com.bandtrack.data.remote

import com.bandtrack.data.models.*
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
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
        val groupWithId = group.copy(id = groupRef.id, createdBy = creatorUserId)
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
        val groups = mutableListOf<Group>()
        
        // Récupérer tous les groupes où l'utilisateur est membre
        val groupsSnapshot = groupsCollection.get().await()
        
        for (groupDoc in groupsSnapshot.documents) {
            val memberDoc = groupDoc.reference
                .collection("members")
                .document(userId)
                .get()
                .await()
            
            if (memberDoc.exists()) {
                groupDoc.toObject(Group::class.java)?.let { groups.add(it) }
            }
        }
        
        Result.success(groups)
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
        groupsCollection
            .document(groupId)
            .collection("members")
            .document(member.userId)
            .set(member)
            .await()
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
        // Chercher dans tous les groupes (optimisable avec indexation)
        val groupsSnapshot = groupsCollection.get().await()
        
        var foundInvitation: InvitationCode? = null
        
        for (groupDoc in groupsSnapshot.documents) {
            val invitationsSnapshot = groupDoc.reference
                .collection("invitations")
                .whereEqualTo("code", code)
                .whereEqualTo("isActive", true)
                .get()
                .await()
            
            if (!invitationsSnapshot.isEmpty) {
                foundInvitation = invitationsSnapshot.documents.first()
                    .toObject(InvitationCode::class.java)
                break
            }
        }
        
        Result.success(foundInvitation)
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
