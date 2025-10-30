package com.abhay.voiceapp.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "session_state")
data class SessionState(
    @PrimaryKey
    val meetingId: Long,
    val isRecording: Boolean,
    val isPaused: Boolean,
    val currentChunkIndex: Int,
    val currentChunkStartTime: Long,
    val pauseReason: String? = null,
    val lastUpdateTime: Long = System.currentTimeMillis()
)
