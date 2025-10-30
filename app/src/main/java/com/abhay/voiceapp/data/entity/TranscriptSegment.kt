package com.abhay.voiceapp.data.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "transcript_segments",
    foreignKeys = [
        ForeignKey(
            entity = Meeting::class,
            parentColumns = ["id"],
            childColumns = ["meetingId"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = Chunk::class,
            parentColumns = ["id"],
            childColumns = ["chunkId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index(value = ["meetingId"]), Index(value = ["chunkId"])]
)
data class TranscriptSegment(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val meetingId: Long,
    val chunkId: Long,
    val text: String,
    val startTime: Long,
    val endTime: Long,
    val confidence: Float? = null,
    val speakerLabel: String? = null,
    val createdAt: Long = System.currentTimeMillis()
)
