package com.voicerecorder.data.room

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "summaries",
    foreignKeys = [
        ForeignKey(
            entity = Meeting::class,
            parentColumns = ["id"],
            childColumns = ["meetingId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("meetingId")]
)
data class Summary(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val meetingId: Long,
    val title: String = "",
    val summary: String = "",
    val actionItems: String = "", // JSON array
    val keyPoints: String = "", // JSON array
    val status: SummaryStatus,
    val progress: Int = 0, // 0-100
    val errorMessage: String? = null,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)

enum class SummaryStatus {
    PENDING,
    IN_PROGRESS,
    COMPLETED,
    ERROR
}
