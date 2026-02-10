package com.bandtrack.workers

import android.content.Context
import android.util.Log
import androidx.work.*
import com.bandtrack.data.local.AppDatabase
import com.bandtrack.data.models.Performance
import com.bandtrack.data.models.Song
import com.bandtrack.data.models.Suggestion
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await
import kotlinx.serialization.json.Json
import java.util.concurrent.TimeUnit

class SyncWorker(
    appContext: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(appContext, workerParams) {

    private val db = FirebaseFirestore.getInstance()
    private val pendingActionDao = AppDatabase.getDatabase(appContext).pendingActionDao()

    override suspend fun doWork(): Result {
        return try {
            val actions = pendingActionDao.getAll()
            if (actions.isEmpty()) {
                return Result.success()
            }

            Log.d(TAG, "Syncing ${actions.size} pending actions")

            for (action in actions) {
                try {
                    processAction(action)
                    // Remove from queue after success
                    pendingActionDao.delete(action)
                } catch (e: Exception) {
                    Log.e(TAG, "Failed to sync action ${action.id}: ${action.entityType}/${action.actionType}", e)
                    // Continue with other actions, don't block queue
                }
            }

            Result.success()
        } catch (e: Exception) {
            Log.e(TAG, "Sync failed", e)
            if (runAttemptCount < 3) {
                Result.retry()
            } else {
                Result.failure()
            }
        }
    }

    private suspend fun processAction(action: com.bandtrack.data.local.PendingActionEntity) {
        when (action.entityType) {
            "SONG" -> processSongAction(action)
            "SUGGESTION" -> processSuggestionAction(action)
            "PERFORMANCE" -> processPerformanceAction(action)
        }
    }

    private suspend fun processSongAction(action: com.bandtrack.data.local.PendingActionEntity) {
        val groupId = action.parentId ?: throw IllegalStateException("Song action must have parentId (groupId)")
        val collectionRef = db.collection("groups").document(groupId).collection("songs")

        when (action.actionType) {
            "CREATE", "UPDATE" -> {
                 val song = Json.decodeFromString<Song>(action.payload)
                 collectionRef.document(action.entityId).set(song).await()
            }
            "DELETE" -> {
                collectionRef.document(action.entityId).delete().await()
            }
        }
    }

    private suspend fun processSuggestionAction(action: com.bandtrack.data.local.PendingActionEntity) {
        val groupId = action.parentId ?: throw IllegalStateException("Suggestion action must have parentId (groupId)")
        val collectionRef = db.collection("groups").document(groupId).collection("suggestions")

        when (action.actionType) {
            "CREATE", "UPDATE" -> {
                 val suggestion = Json.decodeFromString<Suggestion>(action.payload)
                 collectionRef.document(action.entityId).set(suggestion).await()
            }
            "DELETE" -> {
                collectionRef.document(action.entityId).delete().await()
            }
        }
    }

    private suspend fun processPerformanceAction(action: com.bandtrack.data.local.PendingActionEntity) {
        val groupId = action.parentId ?: throw IllegalStateException("Performance action must have parentId (groupId)")
        val collectionRef = db.collection("groups").document(groupId).collection("performances")

        when (action.actionType) {
            "CREATE", "UPDATE" -> {
                val performance = Json.decodeFromString<Performance>(action.payload)
                collectionRef.document(action.entityId).set(performance).await()
            }
            "DELETE" -> {
                collectionRef.document(action.entityId).delete().await()
            }
        }
    }

    companion object {
        private const val TAG = "SyncWorker"
        private const val PERIODIC_SYNC_WORK_NAME = "bandtrack_periodic_sync"

        /**
         * Planifie un OneTimeWorkRequest avec contrainte réseau
         */
        fun enqueueOneTimeSync(context: Context) {
            val constraints = Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build()

            val workRequest = OneTimeWorkRequestBuilder<SyncWorker>()
                .setConstraints(constraints)
                .build()

            WorkManager.getInstance(context).enqueue(workRequest)
        }

        /**
         * Planifie une synchronisation périodique (toutes les 15 min, minimum Android)
         */
        fun enqueuePeriodicSync(context: Context) {
            val constraints = Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build()

            val periodicRequest = PeriodicWorkRequestBuilder<SyncWorker>(
                15, TimeUnit.MINUTES
            )
                .setConstraints(constraints)
                .build()

            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                PERIODIC_SYNC_WORK_NAME,
                ExistingPeriodicWorkPolicy.KEEP,
                periodicRequest
            )
        }
    }
}
