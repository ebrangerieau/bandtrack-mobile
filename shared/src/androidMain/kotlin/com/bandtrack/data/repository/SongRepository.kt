package com.bandtrack.data.repository

import com.bandtrack.data.models.Song
import com.bandtrack.data.models.Suggestion
import com.bandtrack.data.local.toEntity
import com.bandtrack.data.local.toModel
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

/**
 * Repository pour la gestion du répertoire de morceaux
 */
open class SongRepository(
    private val songDao: com.bandtrack.data.local.SongDao? = null
) {
    private val db: FirebaseFirestore = FirebaseFirestore.getInstance()


    /**
     * Créer un nouveau morceau
     */
    suspend fun createSong(
        groupId: String,
        song: Song,
        userId: String
    ): Result<String> = try {
        val docRef = db.collection("groups")
            .document(groupId)
            .collection("songs")
            .document()
        
        val songWithId = song.copy(
            id = docRef.id,
            groupId = groupId,
            addedBy = userId
        )
        
        docRef.set(songWithId).await()
        
        Result.success(docRef.id)
    } catch (e: Exception) {
        Result.failure(e)
    }

    /**
     * Créer un morceau à partir d'une suggestion
     */
    suspend fun createSongFromSuggestion(
        groupId: String,
        suggestion: Suggestion,
        userId: String
    ): Result<String> {
        val song = Song.fromSuggestion(suggestion, userId)
        return createSong(groupId, song, userId)
    }

    /**
     * Récupérer tous les morceaux d'un groupe
     */
    suspend fun getGroupSongs(groupId: String): Result<List<Song>> = try {
        val snapshot = db.collection("groups")
            .document(groupId)
            .collection("songs")
            .get()
            .await()
        
        val songs = snapshot.documents.mapNotNull { 
            it.toObject(Song::class.java) 
        }.sortedBy { it.title }
        
        Result.success(songs)
    } catch (e: Exception) {
        Result.failure(e)
    }

    /**
     * Observer les morceaux en temps réel
     */
    /**
     * Observer les morceaux (Single Source of Truth: DB Locale)
     * 1. Émet immédiatement les données locales (Flow de Room)
     * 2. Lance une écoute Firestore pour mettre à jour la DB locale
     */
    open fun observeGroupSongs(groupId: String): Flow<List<Song>> {
        // Si pas de DAO (cas des tests ou non initialisé), fallback sur Firestore direct (comportement précédent)
        if (songDao == null) {
            return callbackFlow {
                val listener = db.collection("groups")
                    .document(groupId)
                    .collection("songs")
                    .addSnapshotListener { snapshot, error ->
                        if (error != null) {
                            close(error)
                            return@addSnapshotListener
                        }
                        
                        val songs = snapshot?.documents?.mapNotNull { 
                            it.toObject(Song::class.java) 
                        }?.sortedBy { it.title } ?: emptyList()
                        
                        trySend(songs)
                    }
                awaitClose { listener.remove() }
            }
        }

        // Mode Offline-First
        return callbackFlow {
            // A. Observer la DB locale et émettre les mises à jour UI
            val localFlow = songDao.getSongsByGroup(groupId)
            val job = launch {
                localFlow.collect { entities ->
                    trySend(entities.map { it.toModel() })
                }
            }
            
            // B. Écouter Firestore et mettre à jour la DB locale
            val listener = db.collection("groups")
                .document(groupId)
                .collection("songs")
                .addSnapshotListener { snapshot, error ->
                    if (error == null && snapshot != null) {
                        val songs = snapshot.documents.mapNotNull { 
                            it.toObject(Song::class.java) 
                        }
                        // Mise à jour de la cache locale
                        launch {
                            try {
                                val entities = songs.map { it.toEntity() }
                                songDao.replaceGroupSongs(groupId, entities)
                            } catch (e: Exception) {
                                e.printStackTrace() // Log error
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
     * Récupérer un morceau par ID
     */
    suspend fun getSong(groupId: String, songId: String): Result<Song> {
        return try {
            val snapshot = db.collection("groups")
                .document(groupId)
                .collection("songs")
                .document(songId)
                .get()
                .await()
            
            val song = snapshot.toObject(Song::class.java)
                ?: return Result.failure(Exception("Song not found"))
            
            Result.success(song)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Mettre à jour le niveau de maîtrise d'un utilisateur
     */
    suspend fun updateMasteryLevel(
        groupId: String,
        songId: String,
        userId: String,
        level: Int
    ): Result<Unit> = try {
        require(level in 0..10) { "Le niveau doit être entre 0 et 10" }
        
        val docRef = db.collection("groups")
            .document(groupId)
            .collection("songs")
            .document(songId)
        
        db.runTransaction { transaction ->
            val snapshot = transaction.get(docRef)
            val song = snapshot.toObject(Song::class.java)
                ?: throw Exception("Song not found")
            
            val updated = song.updateMasteryLevel(userId, level)
            transaction.set(docRef, updated)
        }.await()
        
        Result.success(Unit)
    } catch (e: Exception) {
        Result.failure(e)
    }

    /**
     * Mettre à jour la configuration d'instrument personnelle
     */
    suspend fun updateMemberConfig(
        groupId: String,
        songId: String,
        userId: String,
        config: String
    ): Result<Unit> = try {
        db.collection("groups")
            .document(groupId)
            .collection("songs")
            .document(songId)
            .update("memberInstrumentConfigs.$userId", config)
            .await()
        Result.success(Unit)
    } catch (e: Exception) {
        Result.failure(e)
    }

    /**
     * Mettre à jour les notes personnelles
     */
    suspend fun updateMemberNotes(
        groupId: String,
        songId: String,
        userId: String,
        notes: String
    ): Result<Unit> = try {
        db.collection("groups")
            .document(groupId)
            .collection("songs")
            .document(songId)
            .update("memberPersonalNotes.$userId", notes)
            .await()
        Result.success(Unit)
    } catch (e: Exception) {
        Result.failure(e)
    }

    /**
     * Mettre à jour les informations d'un morceau
     */
    suspend fun updateSong(
        groupId: String,
        songId: String,
        updates: Map<String, Any>
    ): Result<Unit> = try {
        db.collection("groups")
            .document(groupId)
            .collection("songs")
            .document(songId)
            .update(updates)
            .await()
        
        Result.success(Unit)
    } catch (e: Exception) {
        Result.failure(e)
    }

    /**
     * Supprimer un morceau
     */
    suspend fun deleteSong(
        groupId: String,
        songId: String
    ): Result<Unit> = try {
        db.collection("groups")
            .document(groupId)
            .collection("songs")
            .document(songId)
            .delete()
            .await()
        
        Result.success(Unit)
    } catch (e: Exception) {
        Result.failure(e)
    }
}
