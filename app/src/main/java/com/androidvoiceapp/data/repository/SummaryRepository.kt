package com.androidvoiceapp.data.repository

import com.androidvoiceapp.data.room.Summary
import com.androidvoiceapp.data.room.SummaryStatus
import kotlinx.coroutines.flow.Flow

interface SummaryRepository {
    fun getSummaryByMeetingId(meetingId: Long): Flow<Summary?>
    suspend fun getSummaryByMeetingIdOnce(meetingId: Long): Summary?
    suspend fun createSummary(meetingId: Long): Long
    suspend fun updateSummary(summary: Summary)
    suspend fun updateTitle(meetingId: Long, title: String?)
    suspend fun updateSummaryText(meetingId: Long, summary: String?)
    suspend fun updateActionItems(meetingId: Long, actionItems: String?)
    suspend fun updateKeyPoints(meetingId: Long, keyPoints: String?)
    suspend fun updateSummaryStatus(meetingId: Long, status: SummaryStatus)
    suspend fun updateSummaryError(meetingId: Long, errorMessage: String)
}
