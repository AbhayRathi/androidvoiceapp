package com.abhay.voiceapp.data.dao

import androidx.room.*
import com.abhay.voiceapp.data.entity.SessionState
import kotlinx.coroutines.flow.Flow

@Dao
interface SessionStateDao {
    
    @Query("SELECT * FROM session_state WHERE meetingId = :meetingId")
    fun getSessionState(meetingId: Long): Flow<SessionState?>
    
    @Query("SELECT * FROM session_state WHERE meetingId = :meetingId")
    suspend fun getSessionStateSync(meetingId: Long): SessionState?
    
    @Query("SELECT * FROM session_state WHERE isRecording = 1 LIMIT 1")
    suspend fun getActiveSession(): SessionState?
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSessionState(sessionState: SessionState)
    
    @Update
    suspend fun updateSessionState(sessionState: SessionState)
    
    @Delete
    suspend fun deleteSessionState(sessionState: SessionState)
    
    @Query("DELETE FROM session_state WHERE meetingId = :meetingId")
    suspend fun deleteSessionStateByMeetingId(meetingId: Long)
}
