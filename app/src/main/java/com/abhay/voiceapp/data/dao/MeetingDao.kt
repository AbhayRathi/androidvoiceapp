package com.abhay.voiceapp.data.dao

import androidx.room.*
import com.abhay.voiceapp.data.entity.Meeting
import com.abhay.voiceapp.data.entity.MeetingStatus
import kotlinx.coroutines.flow.Flow

@Dao
interface MeetingDao {
    
    @Query("SELECT * FROM meetings ORDER BY startTime DESC")
    fun getAllMeetings(): Flow<List<Meeting>>
    
    @Query("SELECT * FROM meetings WHERE id = :id")
    fun getMeetingById(id: Long): Flow<Meeting?>
    
    @Query("SELECT * FROM meetings WHERE id = :id")
    suspend fun getMeetingByIdSync(id: Long): Meeting?
    
    @Insert
    suspend fun insertMeeting(meeting: Meeting): Long
    
    @Update
    suspend fun updateMeeting(meeting: Meeting)
    
    @Query("UPDATE meetings SET status = :status WHERE id = :id")
    suspend fun updateMeetingStatus(id: Long, status: MeetingStatus)
    
    @Query("UPDATE meetings SET endTime = :endTime WHERE id = :id")
    suspend fun updateMeetingEndTime(id: Long, endTime: Long)
    
    @Delete
    suspend fun deleteMeeting(meeting: Meeting)
}
