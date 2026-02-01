package com.bandtrack.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey
import kotlinx.serialization.Serializable

@Entity(tableName = "pending_actions")
data class PendingActionEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val actionType: String, // CREATE, UPDATE, DELETE
    val entityType: String, // SONG, PERFORMANCE, GROUP
    val entityId: String,
    val parentId: String? = null, // e.g. groupId
    val payload: String, // JSON
    val createdAt: Long
)
