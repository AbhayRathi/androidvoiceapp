package com.androidvoiceapp.data.repository

import com.androidvoiceapp.data.room.SessionStateDao
import com.androidvoiceapp.data.room.SessionStateEntity
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SessionStateRepositoryImpl @Inject constructor(
    private val sessionStateDao: SessionStateDao
) : SessionStateRepository {
    
    override fun getSessionState(meetingId: Long): Flow<SessionStateEntity?> =
        sessionStateDao.getSessionState(meetingId)
    
    override suspend fun getSessionStateOnce(meetingId: Long): SessionStateEntity? =
        sessionStateDao.getSessionStateOnce(meetingId)
    
    override suspend fun getActiveSession(): SessionStateEntity? =
        sessionStateDao.getActiveSession()
    
    override suspend fun saveSessionState(sessionState: SessionStateEntity) {
        sessionStateDao.insert(sessionState)
    }
    
    override suspend fun updateSessionState(sessionState: SessionStateEntity) {
        sessionStateDao.update(sessionState)
    }
    
    override suspend fun deleteSessionState(meetingId: Long) {
        sessionStateDao.deleteByMeetingId(meetingId)
    }
}
