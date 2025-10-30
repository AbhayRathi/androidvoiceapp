package com.androidvoiceapp.data.room

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "meetings")
data class Meeting(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val startTime: Long,
    val endTime: Long? = null,
    val status: MeetingStatus,
    val title: String? = null,
    val totalDurationMs: Long = 0
)

enum class MeetingStatus {
    RECORDING,
    PAUSED,
    STOPPED,
    TRANSCRIBING,
    SUMMARIZING,
    COMPLETED,
    ERROR
}
