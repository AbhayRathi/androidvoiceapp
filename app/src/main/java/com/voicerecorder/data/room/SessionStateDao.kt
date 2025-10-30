package com.voicerecorder.data.room

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface SessionStateDao {
    @Query("SELECT * FROM session_state WHERE id = 1")
    fun getSessionState(): Flow<SessionState?>

    @Query("SELECT * FROM session_state WHERE id = 1")
    suspend fun getSessionStateSync(): SessionState?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(state: SessionState)

    @Update
    suspend fun update(state: SessionState)

    @Query("DELETE FROM session_state")
    suspend fun clear()
}
