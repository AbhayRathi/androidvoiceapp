package com.voicerecorder.data.room

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface MeetingDao {
    @Query("SELECT * FROM meetings ORDER BY startTime DESC")
    fun getAllMeetings(): Flow<List<Meeting>>

    @Query("SELECT * FROM meetings WHERE id = :meetingId")
    fun getMeetingById(meetingId: Long): Flow<Meeting?>

    @Query("SELECT * FROM meetings WHERE id = :meetingId")
    suspend fun getMeeting(meetingId: Long): Meeting?

    @Insert
    suspend fun insert(meeting: Meeting): Long

    @Update
    suspend fun update(meeting: Meeting)

    @Delete
    suspend fun delete(meeting: Meeting)

    @Query("UPDATE meetings SET status = :status WHERE id = :meetingId")
    suspend fun updateStatus(meetingId: Long, status: MeetingStatus)

    @Query("UPDATE meetings SET endTime = :endTime, duration = :duration WHERE id = :meetingId")
    suspend fun updateEndTime(meetingId: Long, endTime: Long, duration: Long)
}
