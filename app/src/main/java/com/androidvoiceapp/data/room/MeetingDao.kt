package com.androidvoiceapp.data.room

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface MeetingDao {
    
    @Query("SELECT * FROM meetings ORDER BY startTime DESC")
    fun getAllMeetings(): Flow<List<MeetingEntity>>
    
    @Query("SELECT * FROM meetings WHERE id = :meetingId")
    fun getMeetingById(meetingId: Long): Flow<MeetingEntity?>
    
    @Query("SELECT * FROM meetings WHERE id = :meetingId")
    suspend fun getMeetingByIdOnce(meetingId: Long): MeetingEntity?
    
    @Insert
    suspend fun insert(meeting: MeetingEntity): Long
    
    @Update
    suspend fun update(meeting: MeetingEntity)
    
    @Delete
    suspend fun delete(meeting: MeetingEntity)
    
    @Query("UPDATE meetings SET status = :status WHERE id = :meetingId")
    suspend fun updateStatus(meetingId: Long, status: String)
    
    @Query("UPDATE meetings SET endTime = :endTime WHERE id = :meetingId")
    suspend fun updateEndTime(meetingId: Long, endTime: Long)
}
