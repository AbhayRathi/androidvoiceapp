package com.androidvoiceapp.data.room

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "session_state")
data class SessionStateEntity(
    @PrimaryKey
    val meetingId: Long,
    val isRecording: Boolean,
    val isPaused: Boolean,
    val currentChunkSequence: Int,
    val currentChunkPath: String?,
    val recordingStartTime: Long,
    val pausedTime: Long? = null,
    val status: String, // Current status message
    val updatedAt: Long = System.currentTimeMillis()
)
