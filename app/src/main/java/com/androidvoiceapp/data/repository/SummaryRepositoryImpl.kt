package com.androidvoiceapp.data.repository

import com.androidvoiceapp.data.room.Summary
import com.androidvoiceapp.data.room.SummaryDao
import com.androidvoiceapp.data.room.SummaryStatus
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SummaryRepositoryImpl @Inject constructor(
    private val summaryDao: SummaryDao
) : SummaryRepository {
    
    override fun getSummaryByMeetingId(meetingId: Long): Flow<Summary?> {
        return summaryDao.getSummaryByMeetingId(meetingId)
    }
    
    override suspend fun getSummaryByMeetingIdOnce(meetingId: Long): Summary? {
        return summaryDao.getSummaryByMeetingIdOnce(meetingId)
    }
    
    override suspend fun createSummary(meetingId: Long): Long {
        val now = System.currentTimeMillis()
        val summary = Summary(
            meetingId = meetingId,
            status = SummaryStatus.PENDING,
            createdAt = now,
            updatedAt = now
        )
        return summaryDao.insertSummary(summary)
    }
    
    override suspend fun updateSummary(summary: Summary) {
        summaryDao.updateSummary(summary)
    }
    
    override suspend fun updateTitle(meetingId: Long, title: String?) {
        summaryDao.updateTitle(meetingId, title, System.currentTimeMillis())
    }
    
    override suspend fun updateSummaryText(meetingId: Long, summary: String?) {
        summaryDao.updateSummaryText(meetingId, summary, System.currentTimeMillis())
    }
    
    override suspend fun updateActionItems(meetingId: Long, actionItems: String?) {
        summaryDao.updateActionItems(meetingId, actionItems, System.currentTimeMillis())
    }
    
    override suspend fun updateKeyPoints(meetingId: Long, keyPoints: String?) {
        summaryDao.updateKeyPoints(meetingId, keyPoints, System.currentTimeMillis())
    }
    
    override suspend fun updateSummaryStatus(meetingId: Long, status: SummaryStatus) {
        summaryDao.updateSummaryStatus(meetingId, status, System.currentTimeMillis())
    }
    
    override suspend fun updateSummaryError(meetingId: Long, errorMessage: String) {
        summaryDao.updateSummaryError(meetingId, SummaryStatus.ERROR, errorMessage, System.currentTimeMillis())
    }
}
