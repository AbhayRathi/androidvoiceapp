package com.abhay.voiceapp.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "meetings")
data class Meeting(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val title: String,
    val startTime: Long,
    val endTime: Long? = null,
    val status: MeetingStatus,
    val createdAt: Long = System.currentTimeMillis()
)

enum class MeetingStatus {
    RECORDING,
    PAUSED,
    STOPPED,
    PROCESSING,
    COMPLETED,
    FAILED
}
