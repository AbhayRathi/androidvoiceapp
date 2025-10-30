package com.androidvoiceapp.data.room

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
    indices = [Index("meetingId"), Index("sequenceNumber")]
)
data class Chunk(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val meetingId: Long,
    val sequenceNumber: Int,
    val filePath: String,
    val startTime: Long,
    val endTime: Long,
    val durationMs: Long,
    val status: ChunkStatus,
    val tempFilePath: String? = null,
    val fileSize: Long = 0
)

enum class ChunkStatus {
    RECORDING,
    FINALIZED,
    TRANSCRIBING,
    TRANSCRIBED,
    ERROR
}
