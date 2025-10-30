package com.abhay.voiceapp.data.entity

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
    indices = [Index(value = ["meetingId"])]
)
data class Summary(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val meetingId: Long,
    val title: String? = null,
    val summaryText: String? = null,
    val actionItems: String? = null, // JSON array
    val keyPoints: String? = null,   // JSON array
    val status: SummaryStatus = SummaryStatus.PENDING,
    val error: String? = null,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)

enum class SummaryStatus {
    PENDING,
    GENERATING,
    COMPLETED,
    FAILED
}
