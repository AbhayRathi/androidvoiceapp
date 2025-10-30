package com.androidvoiceapp.data.room

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface SummaryDao {
    @Query("SELECT * FROM summaries WHERE meetingId = :meetingId")
    fun getSummaryByMeetingId(meetingId: Long): Flow<Summary?>
    
    @Query("SELECT * FROM summaries WHERE meetingId = :meetingId")
    suspend fun getSummaryByMeetingIdOnce(meetingId: Long): Summary?
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSummary(summary: Summary): Long
    
    @Update
    suspend fun updateSummary(summary: Summary)
    
    @Query("UPDATE summaries SET title = :title, updatedAt = :updatedAt WHERE meetingId = :meetingId")
    suspend fun updateTitle(meetingId: Long, title: String?, updatedAt: Long)
    
    @Query("UPDATE summaries SET summary = :summary, updatedAt = :updatedAt WHERE meetingId = :meetingId")
    suspend fun updateSummaryText(meetingId: Long, summary: String?, updatedAt: Long)
    
    @Query("UPDATE summaries SET actionItems = :actionItems, updatedAt = :updatedAt WHERE meetingId = :meetingId")
    suspend fun updateActionItems(meetingId: Long, actionItems: String?, updatedAt: Long)
    
    @Query("UPDATE summaries SET keyPoints = :keyPoints, updatedAt = :updatedAt WHERE meetingId = :meetingId")
    suspend fun updateKeyPoints(meetingId: Long, keyPoints: String?, updatedAt: Long)
    
    @Query("UPDATE summaries SET status = :status, updatedAt = :updatedAt WHERE meetingId = :meetingId")
    suspend fun updateSummaryStatus(meetingId: Long, status: SummaryStatus, updatedAt: Long)
    
    @Query("UPDATE summaries SET status = :status, errorMessage = :errorMessage, updatedAt = :updatedAt WHERE meetingId = :meetingId")
    suspend fun updateSummaryError(meetingId: Long, status: SummaryStatus, errorMessage: String, updatedAt: Long)
}
