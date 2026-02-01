package com.bandtrack.data.repository

import com.bandtrack.data.models.Performance
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await

/**
 * Repository pour la gestion des événements (concerts, répétitions)
 */
class PerformanceRepository {
    private val db: FirebaseFirestore = FirebaseFirestore.getInstance()

    /**
     * Créer un nouvel événement
     */
    suspend fun createPerformance(performance: Performance): Result<String> = try {
        val docRef = db.collection("groups")
            .document(performance.groupId)
            .collection("performances")
            .document()
        
        val performanceWithId = performance.copy(id = docRef.id)
        
        docRef.set(performanceWithId).await()
        
        Result.success(docRef.id)
    } catch (e: Exception) {
        Result.failure(e)
    }

    /**
     * Récupérer tous les événements d'un groupe, triés par date
     */
    suspend fun getGroupPerformances(groupId: String): Result<List<Performance>> = try {
        val snapshot = db.collection("groups")
            .document(groupId)
            .collection("performances")
            .orderBy("date", Query.Direction.ASCENDING)
            .get()
            .await()
        
        val performances = snapshot.documents.mapNotNull { 
            it.toObject(Performance::class.java) 
        }
        
        Result.success(performances)
    } catch (e: Exception) {
        Result.failure(e)
    }

    /**
     * Observer les événements en temps réel
     */
    fun observeGroupPerformances(groupId: String): Flow<List<Performance>> = callbackFlow {
        val listener = db.collection("groups")
            .document(groupId)
            .collection("performances")
            .orderBy("date", Query.Direction.ASCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }
                
                val performances = snapshot?.documents?.mapNotNull { 
                    it.toObject(Performance::class.java) 
                } ?: emptyList()
                
                trySend(performances)
            }
        
        awaitClose { listener.remove() }
    }

    /**
     * Mettre à jour un événement
     */
    suspend fun updatePerformance(
        groupId: String, 
        performanceId: String, 
        updates: Map<String, Any>
    ): Result<Unit> = try {
        db.collection("groups")
            .document(groupId)
            .collection("performances")
            .document(performanceId)
            .update(updates)
            .await()
        
        Result.success(Unit)
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
        db.collection("groups")
            .document(groupId)
            .collection("performances")
            .document(performanceId)
            .update("setlist", setlist)
            .await()
        
        Result.success(Unit)
    } catch (e: Exception) {
        Result.failure(e)
    }

    /**
     * Supprimer un événement
     */
    suspend fun deletePerformance(groupId: String, performanceId: String): Result<Unit> = try {
        db.collection("groups")
            .document(groupId)
            .collection("performances")
            .document(performanceId)
            .delete()
            .await()
        
        Result.success(Unit)
    } catch (e: Exception) {
        Result.failure(e)
    }
}
