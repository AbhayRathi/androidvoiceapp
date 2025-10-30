package com.voicerecorder.data.repository

import com.voicerecorder.data.room.*
import kotlinx.coroutines.flow.Flow

interface MeetingRepository {
    fun getAllMeetings(): Flow<List<Meeting>>
    fun getMeetingById(meetingId: Long): Flow<Meeting?>
    suspend fun getMeeting(meetingId: Long): Meeting?
    suspend fun createMeeting(meeting: Meeting): Long
    suspend fun updateMeeting(meeting: Meeting)
    suspend fun deleteMeeting(meeting: Meeting)
    suspend fun updateStatus(meetingId: Long, status: MeetingStatus)
    suspend fun updateEndTime(meetingId: Long, endTime: Long, duration: Long)
}

interface ChunkRepository {
    fun getChunksForMeeting(meetingId: Long): Flow<List<Chunk>>
    suspend fun getChunkById(chunkId: Long): Chunk?
    suspend fun getChunkByNumber(meetingId: Long, chunkNumber: Int): Chunk?
    suspend fun createChunk(chunk: Chunk): Long
    suspend fun updateChunk(chunk: Chunk)
    suspend fun deleteChunk(chunk: Chunk)
    suspend fun updateStatus(chunkId: Long, status: ChunkStatus)
    suspend fun updateTranscriptionStatus(chunkId: Long, status: TranscriptionStatus, retryCount: Int)
    suspend fun updateAllTranscriptionStatus(meetingId: Long, status: TranscriptionStatus)
    suspend fun getChunksByTranscriptionStatus(meetingId: Long, status: TranscriptionStatus): List<Chunk>
}

interface TranscriptRepository {
    fun getSegmentsForMeeting(meetingId: Long): Flow<List<TranscriptSegment>>
    fun getSegmentsForChunk(chunkId: Long): Flow<List<TranscriptSegment>>
    suspend fun insertSegment(segment: TranscriptSegment): Long
    suspend fun insertSegments(segments: List<TranscriptSegment>)
    suspend fun updateSegment(segment: TranscriptSegment)
    suspend fun deleteSegment(segment: TranscriptSegment)
    suspend fun deleteAllForMeeting(meetingId: Long)
    suspend fun getSegmentCount(meetingId: Long): Int
}

interface SummaryRepository {
    fun getSummaryForMeeting(meetingId: Long): Flow<Summary?>
    suspend fun getSummary(meetingId: Long): Summary?
    suspend fun insertSummary(summary: Summary): Long
    suspend fun updateSummary(summary: Summary)
    suspend fun deleteSummary(summary: Summary)
    suspend fun updateStatus(meetingId: Long, status: SummaryStatus, errorMessage: String? = null)
    suspend fun updateProgress(meetingId: Long, progress: Int, updatedAt: Long)
}

interface SessionStateRepository {
    fun getSessionState(): Flow<SessionState?>
    suspend fun getSessionStateSync(): SessionState?
    suspend fun saveSessionState(state: SessionState)
    suspend fun updateSessionState(state: SessionState)
    suspend fun clearSessionState()
}
