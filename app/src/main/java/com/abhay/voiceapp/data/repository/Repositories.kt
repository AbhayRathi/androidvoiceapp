package com.abhay.voiceapp.data.repository

import com.abhay.voiceapp.data.entity.*
import kotlinx.coroutines.flow.Flow

interface MeetingRepository {
    fun getAllMeetings(): Flow<List<Meeting>>
    fun getMeetingById(id: Long): Flow<Meeting?>
    suspend fun getMeetingByIdSync(id: Long): Meeting?
    suspend fun createMeeting(title: String, startTime: Long): Long
    suspend fun updateMeeting(meeting: Meeting)
    suspend fun updateMeetingStatus(id: Long, status: MeetingStatus)
    suspend fun endMeeting(id: Long, endTime: Long)
    suspend fun deleteMeeting(meeting: Meeting)
}

interface ChunkRepository {
    fun getChunksByMeetingId(meetingId: Long): Flow<List<Chunk>>
    suspend fun getChunkById(id: Long): Chunk?
    suspend fun getChunksByMeetingIdSync(meetingId: Long): List<Chunk>
    suspend fun createChunk(chunk: Chunk): Long
    suspend fun updateChunk(chunk: Chunk)
    suspend fun updateChunkStatus(id: Long, status: ChunkStatus)
    suspend fun finalizeChunk(id: Long, filePath: String)
    suspend fun getChunkCount(meetingId: Long): Int
}

interface TranscriptRepository {
    fun getTranscriptByMeetingId(meetingId: Long): Flow<List<TranscriptSegment>>
    fun getTranscriptByChunkId(chunkId: Long): Flow<List<TranscriptSegment>>
    suspend fun getTranscriptByMeetingIdSync(meetingId: Long): List<TranscriptSegment>
    suspend fun saveTranscriptSegments(segments: List<TranscriptSegment>)
    suspend fun deleteTranscriptByMeetingId(meetingId: Long)
}

interface SummaryRepository {
    fun getSummaryByMeetingId(meetingId: Long): Flow<Summary?>
    suspend fun getSummaryByMeetingIdSync(meetingId: Long): Summary?
    suspend fun createOrUpdateSummary(summary: Summary): Long
    suspend fun updateSummaryStatus(meetingId: Long, status: SummaryStatus)
    suspend fun updateSummaryTitle(meetingId: Long, title: String)
    suspend fun updateSummaryText(meetingId: Long, text: String)
    suspend fun updateActionItems(meetingId: Long, actionItems: String)
    suspend fun updateKeyPoints(meetingId: Long, keyPoints: String)
}

interface SessionStateRepository {
    fun getSessionState(meetingId: Long): Flow<SessionState?>
    suspend fun getSessionStateSync(meetingId: Long): SessionState?
    suspend fun getActiveSession(): SessionState?
    suspend fun saveSessionState(sessionState: SessionState)
    suspend fun updateSessionState(sessionState: SessionState)
    suspend fun deleteSessionState(meetingId: Long)
}
