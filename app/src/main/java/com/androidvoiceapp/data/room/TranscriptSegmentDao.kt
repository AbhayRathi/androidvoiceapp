package com.androidvoiceapp.data.room

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface TranscriptSegmentDao {
    
    @Query("SELECT * FROM transcript_segments WHERE meetingId = :meetingId ORDER BY startTime ASC")
    fun getSegmentsByMeetingId(meetingId: Long): Flow<List<TranscriptSegmentEntity>>
    
    @Query("SELECT * FROM transcript_segments WHERE chunkId = :chunkId ORDER BY startTime ASC")
    suspend fun getSegmentsByChunkId(chunkId: Long): List<TranscriptSegmentEntity>
    
    @Insert
    suspend fun insert(segment: TranscriptSegmentEntity): Long
    
    @Insert
    suspend fun insertAll(segments: List<TranscriptSegmentEntity>)
    
    @Query("DELETE FROM transcript_segments WHERE meetingId = :meetingId")
    suspend fun deleteByMeetingId(meetingId: Long)
    
    @Query("SELECT COUNT(*) FROM transcript_segments WHERE meetingId = :meetingId")
    suspend fun getSegmentCount(meetingId: Long): Int
}
