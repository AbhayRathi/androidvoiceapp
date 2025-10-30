package com.androidvoiceapp.data.repository

import com.androidvoiceapp.data.room.Meeting
import com.androidvoiceapp.data.room.MeetingStatus
import kotlinx.coroutines.flow.Flow

interface MeetingRepository {
    fun getAllMeetings(): Flow<List<Meeting>>
    fun getMeetingById(meetingId: Long): Flow<Meeting?>
    suspend fun getMeetingByIdOnce(meetingId: Long): Meeting?
    suspend fun createMeeting(startTime: Long): Long
    suspend fun updateMeeting(meeting: Meeting)
    suspend fun updateMeetingStatus(meetingId: Long, status: MeetingStatus)
    suspend fun endMeeting(meetingId: Long, endTime: Long, durationMs: Long)
    suspend fun deleteMeeting(meeting: Meeting)
}
