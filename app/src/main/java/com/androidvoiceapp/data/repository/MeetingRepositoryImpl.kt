package com.androidvoiceapp.data.repository

import com.androidvoiceapp.data.room.Meeting
import com.androidvoiceapp.data.room.MeetingDao
import com.androidvoiceapp.data.room.MeetingStatus
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class MeetingRepositoryImpl @Inject constructor(
    private val meetingDao: MeetingDao
) : MeetingRepository {
    
    override fun getAllMeetings(): Flow<List<Meeting>> {
        return meetingDao.getAllMeetings()
    }
    
    override fun getMeetingById(meetingId: Long): Flow<Meeting?> {
        return meetingDao.getMeetingById(meetingId)
    }
    
    override suspend fun getMeetingByIdOnce(meetingId: Long): Meeting? {
        return meetingDao.getMeetingByIdOnce(meetingId)
    }
    
    override suspend fun createMeeting(startTime: Long): Long {
        val meeting = Meeting(
            startTime = startTime,
            status = MeetingStatus.RECORDING
        )
        return meetingDao.insertMeeting(meeting)
    }
    
    override suspend fun updateMeeting(meeting: Meeting) {
        meetingDao.updateMeeting(meeting)
    }
    
    override suspend fun updateMeetingStatus(meetingId: Long, status: MeetingStatus) {
        meetingDao.updateMeetingStatus(meetingId, status)
    }
    
    override suspend fun endMeeting(meetingId: Long, endTime: Long, durationMs: Long) {
        meetingDao.updateMeetingEnd(meetingId, endTime, durationMs)
    }
    
    override suspend fun deleteMeeting(meeting: Meeting) {
        meetingDao.deleteMeeting(meeting)
    }
}
