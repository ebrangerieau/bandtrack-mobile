package com.bandtrack.data.repository

import com.bandtrack.data.models.Suggestion
import com.bandtrack.data.models.SuggestionStatus
import com.bandtrack.data.remote.FirestoreService
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await

/**
 * Repository pour la gestion des suggestions de morceaux
 */
import android.content.Context
import androidx.work.OneTimeWorkRequest
import androidx.work.WorkManager
import com.bandtrack.data.local.PendingActionDao
import com.bandtrack.data.local.PendingActionEntity
import com.bandtrack.data.local.SuggestionDao
import com.bandtrack.data.local.toEntity
import com.bandtrack.data.local.toModel
import com.bandtrack.workers.SyncWorker
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.coroutines.launch
import java.util.UUID

/**
 * Repository pour la gestion des suggestions de morceaux
 */
open class SuggestionRepository(
    private val context: Context? = null,
    private val suggestionDao: SuggestionDao? = null,
    private val pendingActionDao: PendingActionDao? = null
) {
    private val db: FirebaseFirestore = FirebaseFirestore.getInstance()

    /**
     * Créer une nouvelle suggestion
     */
    suspend fun createSuggestion(
        groupId: String,
        title: String,
        artist: String,
        link: String?,
        userId: String,
        userName: String
    ): Result<String> = try {
        val suggestion = Suggestion(
            groupId = groupId,
            title = title,
            artist = artist,
            link = link,
            createdBy = userId,
            createdByName = userName
        )
        
        val docRef = db.collection("groups")
            .document(groupId)
            .collection("suggestions")
            .document()
        
        val suggestionWithId = suggestion.copy(id = docRef.id)
        docRef.set(suggestionWithId).await()
        
        Result.success(docRef.id)
    } catch (e: Exception) {
        Result.failure(e)
    }

    /**
     * Récupérer toutes les suggestions d'un groupe
     */
    suspend fun getGroupSuggestions(groupId: String): Result<List<Suggestion>> = try {
        val snapshot = db.collection("groups")
            .document(groupId)
            .collection("suggestions")
            .whereEqualTo("status", SuggestionStatus.PENDING.name)
            .get()
            .await()
        
        val suggestions = snapshot.documents.mapNotNull { 
            it.toObject(Suggestion::class.java) 
        }.sortedByDescending { it.voteCount }
        
        Result.success(suggestions)
    } catch (e: Exception) {
        Result.failure(e)
    }

    /**
     * Observer les suggestions en temps réel
     */
    /**
     * Observer les suggestions en temps réel
     */
    fun observeGroupSuggestions(groupId: String): Flow<List<Suggestion>> {
        if (suggestionDao == null) {
            return callbackFlow {
                val listener = db.collection("groups")
                    .document(groupId)
                    .collection("suggestions")
                    .whereEqualTo("status", SuggestionStatus.PENDING.name)
                    .addSnapshotListener { snapshot, error ->
                        if (error != null) {
                            close(error)
                            return@addSnapshotListener
                        }
                        
                        val suggestions = snapshot?.documents?.mapNotNull { 
                            it.toObject(Suggestion::class.java) 
                        }?.sortedByDescending { it.voteCount } ?: emptyList()
                        
                        trySend(suggestions)
                    }
                
                awaitClose { listener.remove() }
            }
        }

        return callbackFlow {
            // A. Local Flow
            val localFlow = suggestionDao.getSuggestionsByGroup(groupId)
            val job = launch {
                localFlow.collect { entities ->
                    trySend(entities.map { it.toModel() })
                }
            }
            
            // B. Remote Flow (Sync)
            val listener = db.collection("groups")
                .document(groupId)
                .collection("suggestions")
                .whereEqualTo("status", SuggestionStatus.PENDING.name)
                .addSnapshotListener { snapshot, error ->
                    if (error == null && snapshot != null) {
                        val suggestions = snapshot.documents.mapNotNull { 
                            it.toObject(Suggestion::class.java) 
                        }
                        
                        launch {
                            try {
                                val entities = suggestions.map { it.toEntity() }
                                suggestionDao.replaceGroupSuggestions(groupId, entities)
                            } catch (e: Exception) {
                                e.printStackTrace()
                            }
                        }
                    }
                }
            
            awaitClose { 
                job.cancel()
                listener.remove() 
            }
        }
    }

    /**
     * Voter pour une suggestion
     */
    /**
     * Voter pour une suggestion
     */
    suspend fun toggleVote(
        groupId: String,
        suggestionId: String,
        userId: String
    ): Result<Unit> = try {
        if (suggestionDao != null && pendingActionDao != null && context != null) {
            // 1. Get local
            val entity = suggestionDao.getSuggestionById(suggestionId)
            if (entity != null) {
                // 2. Toggle vote locally
                val suggestion = entity.toModel()
                val updatedSuggestion = suggestion.toggleVote(userId)
                suggestionDao.insertSuggestion(updatedSuggestion.toEntity())
                
                // 3. Queue Action
                val action = PendingActionEntity(
                    actionType = "UPDATE", // We treat vote as update of the suggestion object
                    entityType = "SUGGESTION",
                    entityId = suggestionId,
                    parentId = groupId,
                    payload = Json.encodeToString(updatedSuggestion),
                    createdAt = System.currentTimeMillis()
                )
                pendingActionDao.insert(action)
                
                // 4. Sync
                enqueueSync()
            }
            Result.success(Unit)
        } else {
            val docRef = db.collection("groups")
                .document(groupId)
                .collection("suggestions")
                .document(suggestionId)
            
            db.runTransaction { transaction ->
                val snapshot = transaction.get(docRef)
                val suggestion = snapshot.toObject(Suggestion::class.java)
                    ?: throw Exception("Suggestion not found")
                
                val updated = suggestion.toggleVote(userId)
                transaction.set(docRef, updated)
            }.await()
            
            Result.success(Unit)
        }
    } catch (e: Exception) {
        Result.failure(e)
    }

    private fun enqueueSync() {
        context?.let { ctx ->
            val workRequest = OneTimeWorkRequest.Builder(SyncWorker::class.java)
                .build()
            WorkManager.getInstance(ctx).enqueue(workRequest)
        }
    }

    /**
     * Supprimer une suggestion
     */
    suspend fun deleteSuggestion(
        groupId: String,
        suggestionId: String
    ): Result<Unit> = try {
        db.collection("groups")
            .document(groupId)
            .collection("suggestions")
            .document(suggestionId)
            .delete()
            .await()
        
        Result.success(Unit)
    } catch (e: Exception) {
        Result.failure(e)
    }

    /**
     * Marquer une suggestion comme acceptée
     */
    suspend fun acceptSuggestion(
        groupId: String,
        suggestionId: String,
        convertedToSongId: String
    ): Result<Unit> = try {
        db.collection("groups")
            .document(groupId)
            .collection("suggestions")
            .document(suggestionId)
            .update(
                mapOf(
                    "status" to SuggestionStatus.ACCEPTED.name,
                    "convertedToSongId" to convertedToSongId
                )
            )
            .await()
        
        Result.success(Unit)
    } catch (e: Exception) {
        Result.failure(e)
    }
}
