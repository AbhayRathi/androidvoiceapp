package com.voicerecorder.data.repository

import com.voicerecorder.data.room.*
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class MeetingRepositoryImpl @Inject constructor(
    private val meetingDao: MeetingDao
) : MeetingRepository {
    override fun getAllMeetings(): Flow<List<Meeting>> = meetingDao.getAllMeetings()
    override fun getMeetingById(meetingId: Long): Flow<Meeting?> = meetingDao.getMeetingById(meetingId)
    override suspend fun getMeeting(meetingId: Long): Meeting? = meetingDao.getMeeting(meetingId)
    override suspend fun createMeeting(meeting: Meeting): Long = meetingDao.insert(meeting)
    override suspend fun updateMeeting(meeting: Meeting) = meetingDao.update(meeting)
    override suspend fun deleteMeeting(meeting: Meeting) = meetingDao.delete(meeting)
    override suspend fun updateStatus(meetingId: Long, status: MeetingStatus) = meetingDao.updateStatus(meetingId, status)
    override suspend fun updateEndTime(meetingId: Long, endTime: Long, duration: Long) = meetingDao.updateEndTime(meetingId, endTime, duration)
}

@Singleton
class ChunkRepositoryImpl @Inject constructor(
    private val chunkDao: ChunkDao
) : ChunkRepository {
    override fun getChunksForMeeting(meetingId: Long): Flow<List<Chunk>> = chunkDao.getChunksForMeeting(meetingId)
    override suspend fun getChunkById(chunkId: Long): Chunk? = chunkDao.getChunkById(chunkId)
    override suspend fun getChunkByNumber(meetingId: Long, chunkNumber: Int): Chunk? = chunkDao.getChunkByNumber(meetingId, chunkNumber)
    override suspend fun createChunk(chunk: Chunk): Long = chunkDao.insert(chunk)
    override suspend fun updateChunk(chunk: Chunk) = chunkDao.update(chunk)
    override suspend fun deleteChunk(chunk: Chunk) = chunkDao.delete(chunk)
    override suspend fun updateStatus(chunkId: Long, status: ChunkStatus) = chunkDao.updateStatus(chunkId, status)
    override suspend fun updateTranscriptionStatus(chunkId: Long, status: TranscriptionStatus, retryCount: Int) = 
        chunkDao.updateTranscriptionStatus(chunkId, status, retryCount)
    override suspend fun updateAllTranscriptionStatus(meetingId: Long, status: TranscriptionStatus) = 
        chunkDao.updateAllTranscriptionStatus(meetingId, status)
    override suspend fun getChunksByTranscriptionStatus(meetingId: Long, status: TranscriptionStatus): List<Chunk> = 
        chunkDao.getChunksByTranscriptionStatus(meetingId, status)
}

@Singleton
class TranscriptRepositoryImpl @Inject constructor(
    private val transcriptDao: TranscriptSegmentDao
) : TranscriptRepository {
    override fun getSegmentsForMeeting(meetingId: Long): Flow<List<TranscriptSegment>> = transcriptDao.getSegmentsForMeeting(meetingId)
    override fun getSegmentsForChunk(chunkId: Long): Flow<List<TranscriptSegment>> = transcriptDao.getSegmentsForChunk(chunkId)
    override suspend fun insertSegment(segment: TranscriptSegment): Long = transcriptDao.insert(segment)
    override suspend fun insertSegments(segments: List<TranscriptSegment>) = transcriptDao.insertAll(segments)
    override suspend fun updateSegment(segment: TranscriptSegment) = transcriptDao.update(segment)
    override suspend fun deleteSegment(segment: TranscriptSegment) = transcriptDao.delete(segment)
    override suspend fun deleteAllForMeeting(meetingId: Long) = transcriptDao.deleteAllForMeeting(meetingId)
    override suspend fun getSegmentCount(meetingId: Long): Int = transcriptDao.getSegmentCount(meetingId)
}

@Singleton
class SummaryRepositoryImpl @Inject constructor(
    private val summaryDao: SummaryDao
) : SummaryRepository {
    override fun getSummaryForMeeting(meetingId: Long): Flow<Summary?> = summaryDao.getSummaryForMeeting(meetingId)
    override suspend fun getSummary(meetingId: Long): Summary? = summaryDao.getSummary(meetingId)
    override suspend fun insertSummary(summary: Summary): Long = summaryDao.insert(summary)
    override suspend fun updateSummary(summary: Summary) = summaryDao.update(summary)
    override suspend fun deleteSummary(summary: Summary) = summaryDao.delete(summary)
    override suspend fun updateStatus(meetingId: Long, status: SummaryStatus, errorMessage: String?) = 
        summaryDao.updateStatus(meetingId, status, errorMessage)
    override suspend fun updateProgress(meetingId: Long, progress: Int, updatedAt: Long) = 
        summaryDao.updateProgress(meetingId, progress, updatedAt)
}

@Singleton
class SessionStateRepositoryImpl @Inject constructor(
    private val sessionStateDao: SessionStateDao
) : SessionStateRepository {
    override fun getSessionState(): Flow<SessionState?> = sessionStateDao.getSessionState()
    override suspend fun getSessionStateSync(): SessionState? = sessionStateDao.getSessionStateSync()
    override suspend fun saveSessionState(state: SessionState) = sessionStateDao.insert(state)
    override suspend fun updateSessionState(state: SessionState) = sessionStateDao.update(state)
    override suspend fun clearSessionState() = sessionStateDao.clear()
}
