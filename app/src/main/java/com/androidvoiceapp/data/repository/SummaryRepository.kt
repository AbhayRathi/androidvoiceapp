package com.androidvoiceapp.data.repository

import com.androidvoiceapp.data.room.SummaryEntity
import kotlinx.coroutines.flow.Flow

interface SummaryRepository {
    fun getSummaryByMeetingId(meetingId: Long): Flow<SummaryEntity?>
    suspend fun getSummaryByMeetingIdOnce(meetingId: Long): SummaryEntity?
    suspend fun createOrUpdateSummary(summary: SummaryEntity): Long
    suspend fun updateSummaryContent(
        meetingId: Long,
        title: String,
        summary: String,
        actionItems: String,
        keyPoints: String,
        progress: Float,
        status: String
    )
    suspend fun updateSummaryStatus(meetingId: Long, status: String, error: String? = null)
}
