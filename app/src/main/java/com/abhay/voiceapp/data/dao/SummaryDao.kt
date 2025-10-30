package com.abhay.voiceapp.data.dao

import androidx.room.*
import com.abhay.voiceapp.data.entity.Summary
import com.abhay.voiceapp.data.entity.SummaryStatus
import kotlinx.coroutines.flow.Flow

@Dao
interface SummaryDao {
    
    @Query("SELECT * FROM summaries WHERE meetingId = :meetingId")
    fun getSummaryByMeetingId(meetingId: Long): Flow<Summary?>
    
    @Query("SELECT * FROM summaries WHERE meetingId = :meetingId")
    suspend fun getSummaryByMeetingIdSync(meetingId: Long): Summary?
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSummary(summary: Summary): Long
    
    @Update
    suspend fun updateSummary(summary: Summary)
    
    @Query("UPDATE summaries SET status = :status, updatedAt = :updatedAt WHERE meetingId = :meetingId")
    suspend fun updateSummaryStatus(meetingId: Long, status: SummaryStatus, updatedAt: Long = System.currentTimeMillis())
    
    @Query("UPDATE summaries SET title = :title, updatedAt = :updatedAt WHERE meetingId = :meetingId")
    suspend fun updateSummaryTitle(meetingId: Long, title: String, updatedAt: Long = System.currentTimeMillis())
    
    @Query("UPDATE summaries SET summaryText = :summaryText, updatedAt = :updatedAt WHERE meetingId = :meetingId")
    suspend fun updateSummaryText(meetingId: Long, summaryText: String, updatedAt: Long = System.currentTimeMillis())
    
    @Query("UPDATE summaries SET actionItems = :actionItems, updatedAt = :updatedAt WHERE meetingId = :meetingId")
    suspend fun updateActionItems(meetingId: Long, actionItems: String, updatedAt: Long = System.currentTimeMillis())
    
    @Query("UPDATE summaries SET keyPoints = :keyPoints, updatedAt = :updatedAt WHERE meetingId = :meetingId")
    suspend fun updateKeyPoints(meetingId: Long, keyPoints: String, updatedAt: Long = System.currentTimeMillis())
    
    @Delete
    suspend fun deleteSummary(summary: Summary)
}
