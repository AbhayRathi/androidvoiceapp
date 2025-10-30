package com.androidvoiceapp.data.room

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "session_state")
data class SessionState(
    @PrimaryKey
    val id: Int = 1, // Single row
    val meetingId: Long? = null,
    val isRecording: Boolean = false,
    val isPaused: Boolean = false,
    val currentChunkId: Long? = null,
    val recordingStartTime: Long? = null,
    val pauseReason: String? = null,
    val lastUpdateTime: Long
)
