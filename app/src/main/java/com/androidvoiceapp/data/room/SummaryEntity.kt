package com.androidvoiceapp.data.room

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "summaries",
    foreignKeys = [
        ForeignKey(
            entity = MeetingEntity::class,
            parentColumns = ["id"],
            childColumns = ["meetingId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("meetingId")]
)
data class SummaryEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val meetingId: Long,
    val title: String = "",
    val summary: String = "",
    val actionItems: String = "", // JSON array as string
    val keyPoints: String = "", // JSON array as string
    val status: String, // "generating", "completed", "failed"
    val progress: Float = 0f, // 0.0 to 1.0 for streaming progress
    val error: String? = null,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)
