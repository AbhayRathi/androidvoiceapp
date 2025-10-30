package com.abhay.voiceapp.data.entity

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
    indices = [Index(value = ["meetingId"])]
)
data class Chunk(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val meetingId: Long,
    val chunkIndex: Int,
    val filePath: String,
    val tempFilePath: String? = null,
    val startTime: Long,
    val endTime: Long,
    val duration: Long,
    val fileSize: Long,
    val status: ChunkStatus = ChunkStatus.RECORDING,
    val createdAt: Long = System.currentTimeMillis()
)

enum class ChunkStatus {
    RECORDING,
    FINALIZING,
    FINALIZED,
    TRANSCRIBING,
    TRANSCRIBED,
    FAILED
}
