package com.androidvoiceapp.data.repository

import com.androidvoiceapp.data.room.MeetingEntity
import kotlinx.coroutines.flow.Flow

interface MeetingRepository {
    fun getAllMeetings(): Flow<List<MeetingEntity>>
    fun getMeetingById(meetingId: Long): Flow<MeetingEntity?>
    suspend fun getMeetingByIdOnce(meetingId: Long): MeetingEntity?
    suspend fun createMeeting(title: String): Long
    suspend fun updateMeeting(meeting: MeetingEntity)
    suspend fun updateMeetingStatus(meetingId: Long, status: String)
    suspend fun endMeeting(meetingId: Long)
    suspend fun deleteMeeting(meeting: MeetingEntity)
}
