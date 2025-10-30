package com.androidvoiceapp.data.room

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "chunks",
    foreignKeys = [
        ForeignKey(
            entity = MeetingEntity::class,
            parentColumns = ["id"],
            childColumns = ["meetingId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("meetingId"), Index("sequenceNumber")]
)
data class ChunkEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val meetingId: Long,
    val sequenceNumber: Int,
    val filePath: String,
    val duration: Long, // in milliseconds
    val startTime: Long, // recording start time for this chunk
    val status: String, // "recording", "finalized", "transcribing", "transcribed", "failed"
    val createdAt: Long = System.currentTimeMillis()
)
