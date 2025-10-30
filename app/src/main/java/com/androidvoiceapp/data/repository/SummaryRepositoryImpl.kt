package com.androidvoiceapp.data.repository

import com.androidvoiceapp.data.room.SummaryDao
import com.androidvoiceapp.data.room.SummaryEntity
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SummaryRepositoryImpl @Inject constructor(
    private val summaryDao: SummaryDao
) : SummaryRepository {
    
    override fun getSummaryByMeetingId(meetingId: Long): Flow<SummaryEntity?> =
        summaryDao.getSummaryByMeetingId(meetingId)
    
    override suspend fun getSummaryByMeetingIdOnce(meetingId: Long): SummaryEntity? =
        summaryDao.getSummaryByMeetingIdOnce(meetingId)
    
    override suspend fun createOrUpdateSummary(summary: SummaryEntity): Long =
        summaryDao.insert(summary)
    
    override suspend fun updateSummaryContent(
        meetingId: Long,
        title: String,
        summary: String,
        actionItems: String,
        keyPoints: String,
        progress: Float,
        status: String
    ) {
        summaryDao.updateContent(
            meetingId,
            title,
            summary,
            actionItems,
            keyPoints,
            progress,
            status,
            System.currentTimeMillis()
        )
    }
    
    override suspend fun updateSummaryStatus(meetingId: Long, status: String, error: String?) {
        summaryDao.updateStatus(meetingId, status, error)
    }
}
