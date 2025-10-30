package com.voicerecorder.data.room

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "meetings")
data class Meeting(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val startTime: Long,
    val endTime: Long? = null,
    val status: MeetingStatus,
    val title: String = "Meeting",
    val duration: Long = 0 // in milliseconds
)

enum class MeetingStatus {
    RECORDING,
    PAUSED,
    STOPPED,
    PROCESSING,
    COMPLETED,
    ERROR
}
