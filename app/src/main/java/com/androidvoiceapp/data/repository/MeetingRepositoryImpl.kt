package com.androidvoiceapp.data.repository

import com.androidvoiceapp.data.room.MeetingDao
import com.androidvoiceapp.data.room.MeetingEntity
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class MeetingRepositoryImpl @Inject constructor(
    private val meetingDao: MeetingDao
) : MeetingRepository {
    
    override fun getAllMeetings(): Flow<List<MeetingEntity>> =
        meetingDao.getAllMeetings()
    
    override fun getMeetingById(meetingId: Long): Flow<MeetingEntity?> =
        meetingDao.getMeetingById(meetingId)
    
    override suspend fun getMeetingByIdOnce(meetingId: Long): MeetingEntity? =
        meetingDao.getMeetingByIdOnce(meetingId)
    
    override suspend fun createMeeting(title: String): Long {
        val meeting = MeetingEntity(
            title = title,
            startTime = System.currentTimeMillis(),
            status = "recording"
        )
        return meetingDao.insert(meeting)
    }
    
    override suspend fun updateMeeting(meeting: MeetingEntity) {
        meetingDao.update(meeting)
    }
    
    override suspend fun updateMeetingStatus(meetingId: Long, status: String) {
        meetingDao.updateStatus(meetingId, status)
    }
    
    override suspend fun endMeeting(meetingId: Long) {
        meetingDao.updateEndTime(meetingId, System.currentTimeMillis())
        meetingDao.updateStatus(meetingId, "stopped")
    }
    
    override suspend fun deleteMeeting(meeting: MeetingEntity) {
        meetingDao.delete(meeting)
    }
}
