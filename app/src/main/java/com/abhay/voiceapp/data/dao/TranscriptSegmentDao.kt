package com.abhay.voiceapp.data.dao

import androidx.room.*
import com.abhay.voiceapp.data.entity.TranscriptSegment
import kotlinx.coroutines.flow.Flow

@Dao
interface TranscriptSegmentDao {
    
    @Query("SELECT * FROM transcript_segments WHERE meetingId = :meetingId ORDER BY startTime ASC")
    fun getTranscriptSegmentsByMeetingId(meetingId: Long): Flow<List<TranscriptSegment>>
    
    @Query("SELECT * FROM transcript_segments WHERE chunkId = :chunkId ORDER BY startTime ASC")
    fun getTranscriptSegmentsByChunkId(chunkId: Long): Flow<List<TranscriptSegment>>
    
    @Query("SELECT * FROM transcript_segments WHERE meetingId = :meetingId ORDER BY startTime ASC")
    suspend fun getTranscriptSegmentsByMeetingIdSync(meetingId: Long): List<TranscriptSegment>
    
    @Insert
    suspend fun insertTranscriptSegment(segment: TranscriptSegment): Long
    
    @Insert
    suspend fun insertTranscriptSegments(segments: List<TranscriptSegment>)
    
    @Update
    suspend fun updateTranscriptSegment(segment: TranscriptSegment)
    
    @Delete
    suspend fun deleteTranscriptSegment(segment: TranscriptSegment)
    
    @Query("DELETE FROM transcript_segments WHERE meetingId = :meetingId")
    suspend fun deleteTranscriptSegmentsByMeetingId(meetingId: Long)
}
