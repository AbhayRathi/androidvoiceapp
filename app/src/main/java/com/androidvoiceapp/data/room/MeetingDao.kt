package com.androidvoiceapp.data.room

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface MeetingDao {
    @Query("SELECT * FROM meetings ORDER BY startTime DESC")
    fun getAllMeetings(): Flow<List<Meeting>>
    
    @Query("SELECT * FROM meetings WHERE id = :meetingId")
    fun getMeetingById(meetingId: Long): Flow<Meeting?>
    
    @Query("SELECT * FROM meetings WHERE id = :meetingId")
    suspend fun getMeetingByIdOnce(meetingId: Long): Meeting?
    
    @Insert
    suspend fun insertMeeting(meeting: Meeting): Long
    
    @Update
    suspend fun updateMeeting(meeting: Meeting)
    
    @Delete
    suspend fun deleteMeeting(meeting: Meeting)
    
    @Query("UPDATE meetings SET status = :status WHERE id = :meetingId")
    suspend fun updateMeetingStatus(meetingId: Long, status: MeetingStatus)
    
    @Query("UPDATE meetings SET endTime = :endTime, totalDurationMs = :durationMs WHERE id = :meetingId")
    suspend fun updateMeetingEnd(meetingId: Long, endTime: Long, durationMs: Long)
}
