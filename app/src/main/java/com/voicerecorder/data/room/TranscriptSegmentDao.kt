package com.voicerecorder.data.room

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface TranscriptSegmentDao {
    @Query("SELECT * FROM transcript_segments WHERE meetingId = :meetingId ORDER BY timestamp ASC")
    fun getSegmentsForMeeting(meetingId: Long): Flow<List<TranscriptSegment>>

    @Query("SELECT * FROM transcript_segments WHERE chunkId = :chunkId ORDER BY timestamp ASC")
    fun getSegmentsForChunk(chunkId: Long): Flow<List<TranscriptSegment>>

    @Insert
    suspend fun insert(segment: TranscriptSegment): Long

    @Insert
    suspend fun insertAll(segments: List<TranscriptSegment>)

    @Update
    suspend fun update(segment: TranscriptSegment)

    @Delete
    suspend fun delete(segment: TranscriptSegment)

    @Query("DELETE FROM transcript_segments WHERE meetingId = :meetingId")
    suspend fun deleteAllForMeeting(meetingId: Long)

    @Query("SELECT COUNT(*) FROM transcript_segments WHERE meetingId = :meetingId")
    suspend fun getSegmentCount(meetingId: Long): Int
}
