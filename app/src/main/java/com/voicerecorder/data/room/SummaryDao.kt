package com.voicerecorder.data.room

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface SummaryDao {
    @Query("SELECT * FROM summaries WHERE meetingId = :meetingId")
    fun getSummaryForMeeting(meetingId: Long): Flow<Summary?>

    @Query("SELECT * FROM summaries WHERE meetingId = :meetingId")
    suspend fun getSummary(meetingId: Long): Summary?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(summary: Summary): Long

    @Update
    suspend fun update(summary: Summary)

    @Delete
    suspend fun delete(summary: Summary)

    @Query("UPDATE summaries SET status = :status, errorMessage = :errorMessage WHERE meetingId = :meetingId")
    suspend fun updateStatus(meetingId: Long, status: SummaryStatus, errorMessage: String? = null)

    @Query("UPDATE summaries SET progress = :progress, updatedAt = :updatedAt WHERE meetingId = :meetingId")
    suspend fun updateProgress(meetingId: Long, progress: Int, updatedAt: Long)
}
