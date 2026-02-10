package com.bandtrack.data.repository

import android.content.Context
import com.bandtrack.data.local.PendingActionDao
import com.bandtrack.data.local.PendingActionEntity
import com.bandtrack.data.local.PerformanceDao
import com.bandtrack.data.local.toEntity
import com.bandtrack.data.local.toModel
import com.bandtrack.data.models.Performance
import com.bandtrack.workers.SyncWorker
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.util.UUID

/**
 * Repository pour la gestion des événements (concerts, répétitions)
 * Offline-First implementation
 */
class PerformanceRepository(
    private val context: Context? = null,
    private val performanceDao: PerformanceDao? = null,
    private val pendingActionDao: PendingActionDao? = null
) {
    private val db: FirebaseFirestore = FirebaseFirestore.getInstance()

    /**
     * Créer un nouvel événement
     */
    suspend fun createPerformance(performance: Performance): Result<String> = try {
        if (performanceDao != null && pendingActionDao != null && context != null) {
            // Mode Offline
            val newId = UUID.randomUUID().toString()
            val performanceWithId = performance.copy(id = newId)
            
            // 1. Local Save
            performanceDao.insertPerformance(performanceWithId.toEntity())
            
            // 2. Queue Action
            val action = PendingActionEntity(
                actionType = "CREATE",
                entityType = "PERFORMANCE",
                entityId = newId,
                parentId = performance.groupId,
                payload = Json.encodeToString(performanceWithId),
                createdAt = System.currentTimeMillis()
            )
            pendingActionDao.insert(action)
            
            // 3. Sync
            enqueueSync()
            
            Result.success(newId)
        } else {
            // Fallback Online
            val docRef = db.collection("groups")
                .document(performance.groupId)
                .collection("performances")
                .document()
            
            val performanceWithId = performance.copy(id = docRef.id)
            docRef.set(performanceWithId).await()
            Result.success(docRef.id)
        }
    } catch (e: Exception) {
        Result.failure(e)
    }

    /**
     * Observer les événements (Single Source of Truth: Local DB)
     */
    fun observeGroupPerformances(groupId: String): Flow<List<Performance>> {
        if (performanceDao == null) {
            return callbackFlow {
                val listener = db.collection("groups")
                    .document(groupId)
                    .collection("performances")
                    .orderBy("date", Query.Direction.ASCENDING)
                    .addSnapshotListener { snapshot, error ->
                        if (error != null) {
                            close(error)
                            return@addSnapshotListener
                        }
                        val perfs = snapshot?.documents?.mapNotNull { 
                            it.toObject(Performance::class.java) 
                        } ?: emptyList()
                        trySend(perfs)
                    }
                awaitClose { listener.remove() }
            }
        }

        return callbackFlow {
            // 1. Emit Local Data
            val localFlow = performanceDao.getPerformancesByGroup(groupId)
            val job = launch {
                localFlow.collect { entities ->
                    trySend(entities.map { it.toModel() })
                }
            }
            
            // 2. Sync from Remote
            val listener = db.collection("groups")
                .document(groupId)
                .collection("performances")
                .addSnapshotListener { snapshot, error ->
                    if (error == null && snapshot != null) {
                        val perfs = snapshot.documents.mapNotNull { 
                            it.toObject(Performance::class.java) 
                        }
                        launch {
                            try {
                                val entities = perfs.map { it.toEntity() }
                                performanceDao.replaceGroupPerformances(entities)
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
     * Mettre à jour un événement
     */
    suspend fun updatePerformance(
        groupId: String, 
        performanceId: String, 
        updates: Map<String, Any>
    ): Result<Unit> = try {
        if (performanceDao != null && pendingActionDao != null && context != null) {
            // Mode Offline-First : fetch local, apply updates, save + queue
            val entity = performanceDao.getPerformanceById(performanceId)
            if (entity != null) {
                val current = entity.toModel()
                // Appliquer les updates connus
                var updated = current
                updates.forEach { (key, value) ->
                    updated = when (key) {
                        "title" -> updated.copy(title = value as String)
                        "location" -> updated.copy(location = value as String)
                        "notes" -> updated.copy(notes = value as String)
                        "date" -> updated.copy(date = value as Long)
                        "durationMinutes" -> updated.copy(durationMinutes = (value as Number).toInt())
                        else -> updated
                    }
                }
                performanceDao.insertPerformance(updated.toEntity())
                
                val action = PendingActionEntity(
                    actionType = "UPDATE",
                    entityType = "PERFORMANCE",
                    entityId = performanceId,
                    parentId = groupId,
                    payload = Json.encodeToString(updated),
                    createdAt = System.currentTimeMillis()
                )
                pendingActionDao.insert(action)
                enqueueSync()
            }
            Result.success(Unit)
        } else {
            // Fallback Online
            db.collection("groups")
                .document(groupId)
                .collection("performances")
                .document(performanceId)
                .update(updates)
                .await()
            Result.success(Unit)
        }
    } catch (e: Exception) {
        Result.failure(e)
    }

    /**
     * Mettre à jour la setlist d'un événement
     */
    suspend fun updateSetlist(
        groupId: String, 
        performanceId: String, 
        setlist: List<String>
    ): Result<Unit> = try {
        if (performanceDao != null && pendingActionDao != null && context != null) {
            // Local Update
            val performance = performanceDao.getPerformanceById(performanceId)
            if (performance != null) {
                // Créer une copie avec la nouvelle setlist
                // Note: PerformanceEntity stores setlist as String (via converter). 
                // but we need to update it.
                // Best way: Update the entity field.
                
                // Hack: We don't have a partial update method in DAO yet for setlist only, 
                // but we can fetch, modify, insert (REPLACE).
                val updatedModel = performance.toModel().copy(setlist = setlist)
                performanceDao.insertPerformance(updatedModel.toEntity())
                
                // Queue Action (We need a special action for SETLIST_UPDATE or just UPDATE with payload)
                // Let's use generic UPDATE with full payload for simplicity
                val action = PendingActionEntity(
                    actionType = "UPDATE",
                    entityType = "PERFORMANCE",
                    entityId = performanceId,
                    parentId = groupId,
                    payload = Json.encodeToString(updatedModel),
                    createdAt = System.currentTimeMillis()
                )
                pendingActionDao.insert(action)
                
                enqueueSync()
            }
            Result.success(Unit)
        } else {
            db.collection("groups")
                .document(groupId)
                .collection("performances")
                .document(performanceId)
                .update("setlist", setlist)
                .await()
            Result.success(Unit)
        }
    } catch (e: Exception) {
        Result.failure(e)
    }

    /**
     * Supprimer un événement
     */
    suspend fun deletePerformance(groupId: String, performanceId: String): Result<Unit> = try {
        if (performanceDao != null && pendingActionDao != null && context != null) {
            performanceDao.deletePerformance(performanceId)
            
            val action = PendingActionEntity(
                actionType = "DELETE",
                entityType = "PERFORMANCE",
                entityId = performanceId,
                parentId = groupId,
                payload = "",
                createdAt = System.currentTimeMillis()
            )
            pendingActionDao.insert(action)
            enqueueSync()
            
            Result.success(Unit)
        } else {
            db.collection("groups")
                .document(groupId)
                .collection("performances")
                .document(performanceId)
                .delete()
                .await()
            Result.success(Unit)
        }
    } catch (e: Exception) {
        Result.failure(e)
    }

    private fun enqueueSync() {
        context?.let { ctx ->
            SyncWorker.enqueueOneTimeSync(ctx)
        }
    }
}
