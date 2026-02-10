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

import com.bandtrack.data.local.PendingActionDao
import com.bandtrack.data.local.PendingActionEntity
import com.bandtrack.workers.SyncWorker
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.util.UUID

/**
 * Repository pour la gestion du répertoire de morceaux
 * Offline-First implementation
 */
open class SongRepository(
    private val context: android.content.Context? = null,
    private val songDao: com.bandtrack.data.local.SongDao? = null,
    private val pendingActionDao: PendingActionDao? = null
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
        // Mode Offline-First
        if (songDao != null && pendingActionDao != null && context != null) {
             val newId = UUID.randomUUID().toString()
             val songWithId = song.copy(
                id = newId,
                groupId = groupId,
                addedBy = userId
            )
            
            // 1. Sauvegarde Locale
            songDao.insertSong(songWithId.toEntity())
            
            // 2. Ajouter à la file d'attente
            val action = PendingActionEntity(
                actionType = "CREATE",
                entityType = "SONG",
                entityId = newId,
                parentId = groupId,
                payload = Json.encodeToString(songWithId),
                createdAt = System.currentTimeMillis()
            )
            pendingActionDao.insert(action)
            
            // 3. Déclencher la synchro
            enqueueSync()
            
            Result.success(newId)
        } else {
            // Fallback Online-Only (Legacy)
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
        }
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
            // 1. Essai Local
            if (songDao != null) {
                val localEntity = songDao.getSongById(songId)
                if (localEntity != null) {
                    return Result.success(localEntity.toModel())
                }
            }

            // 2. Fallback Remote (si nécessaire ou si le morceau n'est pas encore sync)
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
        
        if (songDao != null && pendingActionDao != null && context != null) {
            // Mode Offline-First
            val entity = songDao.getSongById(songId)
            if (entity != null) {
                val song = entity.toModel()
                val updated = song.updateMasteryLevel(userId, level)
                songDao.insertSong(updated.toEntity())
                
                val action = PendingActionEntity(
                    actionType = "UPDATE",
                    entityType = "SONG",
                    entityId = songId,
                    parentId = groupId,
                    payload = Json.encodeToString(updated),
                    createdAt = System.currentTimeMillis()
                )
                pendingActionDao.insert(action)
                enqueueSync()
            }
            Result.success(Unit)
        } else {
            // Fallback Online (Transaction Firestore)
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
        }
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
        if (songDao != null && pendingActionDao != null && context != null) {
            val entity = songDao.getSongById(songId)
            if (entity != null) {
                val song = entity.toModel()
                val updatedConfigs = song.memberInstrumentConfigs.toMutableMap()
                updatedConfigs[userId] = config
                val updated = song.copy(memberInstrumentConfigs = updatedConfigs)
                songDao.insertSong(updated.toEntity())
                
                val action = PendingActionEntity(
                    actionType = "UPDATE",
                    entityType = "SONG",
                    entityId = songId,
                    parentId = groupId,
                    payload = Json.encodeToString(updated),
                    createdAt = System.currentTimeMillis()
                )
                pendingActionDao.insert(action)
                enqueueSync()
            }
            Result.success(Unit)
        } else {
            db.collection("groups")
                .document(groupId)
                .collection("songs")
                .document(songId)
                .update("memberInstrumentConfigs.$userId", config)
                .await()
            Result.success(Unit)
        }
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
        if (songDao != null && pendingActionDao != null && context != null) {
            val entity = songDao.getSongById(songId)
            if (entity != null) {
                val song = entity.toModel()
                val updatedNotes = song.memberPersonalNotes.toMutableMap()
                updatedNotes[userId] = notes
                val updated = song.copy(memberPersonalNotes = updatedNotes)
                songDao.insertSong(updated.toEntity())
                
                val action = PendingActionEntity(
                    actionType = "UPDATE",
                    entityType = "SONG",
                    entityId = songId,
                    parentId = groupId,
                    payload = Json.encodeToString(updated),
                    createdAt = System.currentTimeMillis()
                )
                pendingActionDao.insert(action)
                enqueueSync()
            }
            Result.success(Unit)
        } else {
            db.collection("groups")
                .document(groupId)
                .collection("songs")
                .document(songId)
                .update("memberPersonalNotes.$userId", notes)
                .await()
            Result.success(Unit)
        }
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
         // Note: Partial updates are tricky offline because we need the full object to serialize it for the worker
         // For now, we will fetch the song locally, apply updates, and treat it as a full UPDATE
         if (songDao != null && pendingActionDao != null && context != null) {
            // 1. Get current local song
            val currentEntity = songDao.getSongById(songId)
            if (currentEntity != null) {
                // Apply updates (Simplification: we assume we can re-construct the object)
                // Actually, updates Map is hard to map to Entity.
                // LIMITATION: For offline mode, complex partial updates are hard.
                // We will try to fetch the song from DB, update fields, save back.
                // OR: We check if the update is simple.
                
                // CRITICAL FIX: The current architecture makes partial updates hard with JSON payload.
                // We will defer to Online-Only for 'updates' map if complex, OR
                // ideally, the UI should provide the full modified Song object, not a Map.
                
                // For now, let's keep the Online-Only implementation for this generic 'updateSong' 
                // but if we had 'updateSong(song: Song)' it would be easier.
                
                // Let's implement a BEST EFFORT online-first for this generic method
                 db.collection("groups")
                    .document(groupId)
                    .collection("songs")
                    .document(songId)
                    .update(updates)
                    .await()
            } else {
                 db.collection("groups")
                    .document(groupId)
                    .collection("songs")
                    .document(songId)
                    .update(updates)
                    .await()
            }
             Result.success(Unit)
         } else {
            db.collection("groups")
                .document(groupId)
                .collection("songs")
                .document(songId)
                .update(updates)
                .await()
            
            Result.success(Unit)
         }
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
        if (songDao != null && pendingActionDao != null && context != null) {
            // 1. Delete Local
            songDao.deleteSong(songId)
            
            // 2. Queue Action
             val action = PendingActionEntity(
                actionType = "DELETE",
                entityType = "SONG",
                entityId = songId,
                parentId = groupId,
                payload = "", // No payload needed for delete usually, but we forced non-null string
                createdAt = System.currentTimeMillis()
            )
            pendingActionDao.insert(action)
            
            // 3. Sync
            enqueueSync()
            
            Result.success(Unit)
        } else {
            db.collection("groups")
                .document(groupId)
                .collection("songs")
                .document(songId)
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
