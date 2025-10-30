package com.androidvoiceapp.data.room

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
    val title: String? = null,
    val summary: String? = null,
    val actionItems: String? = null, // JSON array of strings
    val keyPoints: String? = null,   // JSON array of strings
    val status: SummaryStatus,
    val createdAt: Long,
    val updatedAt: Long,
    val errorMessage: String? = null
)

enum class SummaryStatus {
    PENDING,
    STREAMING,
    COMPLETED,
    ERROR
}
