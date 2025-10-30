package com.androidvoiceapp.data.room

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface SummaryDao {
    
    @Query("SELECT * FROM summaries WHERE meetingId = :meetingId")
    fun getSummaryByMeetingId(meetingId: Long): Flow<SummaryEntity?>
    
    @Query("SELECT * FROM summaries WHERE meetingId = :meetingId")
    suspend fun getSummaryByMeetingIdOnce(meetingId: Long): SummaryEntity?
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(summary: SummaryEntity): Long
    
    @Update
    suspend fun update(summary: SummaryEntity)
    
    @Query("UPDATE summaries SET title = :title, summary = :summary, actionItems = :actionItems, keyPoints = :keyPoints, progress = :progress, status = :status, updatedAt = :updatedAt WHERE meetingId = :meetingId")
    suspend fun updateContent(
        meetingId: Long,
        title: String,
        summary: String,
        actionItems: String,
        keyPoints: String,
        progress: Float,
        status: String,
        updatedAt: Long
    )
    
    @Query("UPDATE summaries SET status = :status, error = :error WHERE meetingId = :meetingId")
    suspend fun updateStatus(meetingId: Long, status: String, error: String? = null)
}
