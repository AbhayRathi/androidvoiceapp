package com.androidvoiceapp.data.room

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface SessionStateDao {
    
    @Query("SELECT * FROM session_state WHERE meetingId = :meetingId")
    fun getSessionState(meetingId: Long): Flow<SessionStateEntity?>
    
    @Query("SELECT * FROM session_state WHERE meetingId = :meetingId")
    suspend fun getSessionStateOnce(meetingId: Long): SessionStateEntity?
    
    @Query("SELECT * FROM session_state WHERE isRecording = 1 LIMIT 1")
    suspend fun getActiveSession(): SessionStateEntity?
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(sessionState: SessionStateEntity)
    
    @Update
    suspend fun update(sessionState: SessionStateEntity)
    
    @Delete
    suspend fun delete(sessionState: SessionStateEntity)
    
    @Query("DELETE FROM session_state WHERE meetingId = :meetingId")
    suspend fun deleteByMeetingId(meetingId: Long)
}
