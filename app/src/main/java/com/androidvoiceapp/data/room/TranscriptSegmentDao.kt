package com.androidvoiceapp.data.room

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface TranscriptSegmentDao {
    @Query("SELECT * FROM transcript_segments WHERE meetingId = :meetingId ORDER BY startTimeMs ASC")
    fun getTranscriptsByMeetingId(meetingId: Long): Flow<List<TranscriptSegment>>
    
    @Query("SELECT * FROM transcript_segments WHERE chunkId = :chunkId ORDER BY startTimeMs ASC")
    suspend fun getTranscriptsByChunkId(chunkId: Long): List<TranscriptSegment>
    
    @Insert
    suspend fun insertTranscriptSegment(segment: TranscriptSegment): Long
    
    @Insert
    suspend fun insertTranscriptSegments(segments: List<TranscriptSegment>)
    
    @Query("DELETE FROM transcript_segments WHERE meetingId = :meetingId")
    suspend fun deleteTranscriptsByMeetingId(meetingId: Long)
    
    @Query("SELECT COUNT(*) FROM transcript_segments WHERE meetingId = :meetingId")
    suspend fun getTranscriptCountForMeeting(meetingId: Long): Int
}
