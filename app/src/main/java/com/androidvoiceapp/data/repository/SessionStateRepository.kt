package com.androidvoiceapp.data.repository

import com.androidvoiceapp.data.room.SessionStateEntity
import kotlinx.coroutines.flow.Flow

interface SessionStateRepository {
    fun getSessionState(meetingId: Long): Flow<SessionStateEntity?>
    suspend fun getSessionStateOnce(meetingId: Long): SessionStateEntity?
    suspend fun getActiveSession(): SessionStateEntity?
    suspend fun saveSessionState(sessionState: SessionStateEntity)
    suspend fun updateSessionState(sessionState: SessionStateEntity)
    suspend fun deleteSessionState(meetingId: Long)
}
