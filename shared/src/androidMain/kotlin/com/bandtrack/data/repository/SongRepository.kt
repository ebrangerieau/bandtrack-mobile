package com.bandtrack.data.repository

import com.bandtrack.data.models.Song
import com.bandtrack.data.models.Suggestion
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await

/**
 * Repository pour la gestion du répertoire de morceaux
 */
class SongRepository {
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
    fun observeGroupSongs(groupId: String): Flow<List<Song>> = callbackFlow {
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
