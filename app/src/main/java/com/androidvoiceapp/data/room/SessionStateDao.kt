package com.androidvoiceapp.data.room

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface SessionStateDao {
    @Query("SELECT * FROM session_state WHERE id = 1")
    fun getSessionState(): Flow<SessionState?>
    
    @Query("SELECT * FROM session_state WHERE id = 1")
    suspend fun getSessionStateOnce(): SessionState?
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSessionState(state: SessionState)
    
    @Update
    suspend fun updateSessionState(state: SessionState)
    
    @Query("DELETE FROM session_state WHERE id = 1")
    suspend fun clearSessionState()
}
