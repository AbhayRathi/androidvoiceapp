package com.voicerecorder.data.room

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "session_state")
data class SessionState(
    @PrimaryKey
    val id: Int = 1, // Single row
    val meetingId: Long? = null,
    val isRecording: Boolean = false,
    val isPaused: Boolean = false,
    val currentChunkNumber: Int = 0,
    val recordingStartTime: Long = 0,
    val pauseTime: Long = 0,
    val totalPausedDuration: Long = 0,
    val lastChunkPath: String? = null,
    val statusMessage: String = "",
    val needsRecovery: Boolean = false
)
