package com.voicerecorder.data.room

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "chunks",
    foreignKeys = [
        ForeignKey(
            entity = Meeting::class,
            parentColumns = ["id"],
            childColumns = ["meetingId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("meetingId"), Index("chunkNumber")]
)
data class Chunk(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val meetingId: Long,
    val chunkNumber: Int,
    val filePath: String,
    val startTime: Long,
    val endTime: Long,
    val duration: Long, // in milliseconds
    val status: ChunkStatus,
    val transcriptionStatus: TranscriptionStatus = TranscriptionStatus.PENDING,
    val retryCount: Int = 0
)

enum class ChunkStatus {
    RECORDING,
    FINALIZING,
    FINALIZED,
    ERROR
}

enum class TranscriptionStatus {
    PENDING,
    IN_PROGRESS,
    COMPLETED,
    FAILED
}
