package com.abhay.voiceapp.data.repository

import com.abhay.voiceapp.data.dao.*
import com.abhay.voiceapp.data.entity.*
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class MeetingRepositoryImpl @Inject constructor(
    private val meetingDao: MeetingDao
) : MeetingRepository {
    
    override fun getAllMeetings(): Flow<List<Meeting>> = meetingDao.getAllMeetings()
    
    override fun getMeetingById(id: Long): Flow<Meeting?> = meetingDao.getMeetingById(id)
    
    override suspend fun getMeetingByIdSync(id: Long): Meeting? = meetingDao.getMeetingByIdSync(id)
    
    override suspend fun createMeeting(title: String, startTime: Long): Long {
        val meeting = Meeting(
            title = title,
            startTime = startTime,
            status = MeetingStatus.RECORDING
        )
        return meetingDao.insertMeeting(meeting)
    }
    
    override suspend fun updateMeeting(meeting: Meeting) = meetingDao.updateMeeting(meeting)
    
    override suspend fun updateMeetingStatus(id: Long, status: MeetingStatus) {
        meetingDao.updateMeetingStatus(id, status)
    }
    
    override suspend fun endMeeting(id: Long, endTime: Long) {
        meetingDao.updateMeetingEndTime(id, endTime)
    }
    
    override suspend fun deleteMeeting(meeting: Meeting) = meetingDao.deleteMeeting(meeting)
}

@Singleton
class ChunkRepositoryImpl @Inject constructor(
    private val chunkDao: ChunkDao
) : ChunkRepository {
    
    override fun getChunksByMeetingId(meetingId: Long): Flow<List<Chunk>> = 
        chunkDao.getChunksByMeetingId(meetingId)
    
    override suspend fun getChunkById(id: Long): Chunk? = chunkDao.getChunkById(id)
    
    override suspend fun getChunksByMeetingIdSync(meetingId: Long): List<Chunk> = 
        chunkDao.getChunksByMeetingIdSync(meetingId)
    
    override suspend fun createChunk(chunk: Chunk): Long = chunkDao.insertChunk(chunk)
    
    override suspend fun updateChunk(chunk: Chunk) = chunkDao.updateChunk(chunk)
    
    override suspend fun updateChunkStatus(id: Long, status: ChunkStatus) {
        chunkDao.updateChunkStatus(id, status)
    }
    
    override suspend fun finalizeChunk(id: Long, filePath: String) {
        chunkDao.finalizeChunk(id, filePath, ChunkStatus.FINALIZED)
    }
    
    override suspend fun getChunkCount(meetingId: Long): Int = chunkDao.getChunkCount(meetingId)
}

@Singleton
class TranscriptRepositoryImpl @Inject constructor(
    private val transcriptSegmentDao: TranscriptSegmentDao
) : TranscriptRepository {
    
    override fun getTranscriptByMeetingId(meetingId: Long): Flow<List<TranscriptSegment>> = 
        transcriptSegmentDao.getTranscriptSegmentsByMeetingId(meetingId)
    
    override fun getTranscriptByChunkId(chunkId: Long): Flow<List<TranscriptSegment>> = 
        transcriptSegmentDao.getTranscriptSegmentsByChunkId(chunkId)
    
    override suspend fun getTranscriptByMeetingIdSync(meetingId: Long): List<TranscriptSegment> = 
        transcriptSegmentDao.getTranscriptSegmentsByMeetingIdSync(meetingId)
    
    override suspend fun saveTranscriptSegments(segments: List<TranscriptSegment>) {
        transcriptSegmentDao.insertTranscriptSegments(segments)
    }
    
    override suspend fun deleteTranscriptByMeetingId(meetingId: Long) {
        transcriptSegmentDao.deleteTranscriptSegmentsByMeetingId(meetingId)
    }
}

@Singleton
class SummaryRepositoryImpl @Inject constructor(
    private val summaryDao: SummaryDao
) : SummaryRepository {
    
    override fun getSummaryByMeetingId(meetingId: Long): Flow<Summary?> = 
        summaryDao.getSummaryByMeetingId(meetingId)
    
    override suspend fun getSummaryByMeetingIdSync(meetingId: Long): Summary? = 
        summaryDao.getSummaryByMeetingIdSync(meetingId)
    
    override suspend fun createOrUpdateSummary(summary: Summary): Long = 
        summaryDao.insertSummary(summary)
    
    override suspend fun updateSummaryStatus(meetingId: Long, status: SummaryStatus) {
        summaryDao.updateSummaryStatus(meetingId, status)
    }
    
    override suspend fun updateSummaryTitle(meetingId: Long, title: String) {
        summaryDao.updateSummaryTitle(meetingId, title)
    }
    
    override suspend fun updateSummaryText(meetingId: Long, text: String) {
        summaryDao.updateSummaryText(meetingId, text)
    }
    
    override suspend fun updateActionItems(meetingId: Long, actionItems: String) {
        summaryDao.updateActionItems(meetingId, actionItems)
    }
    
    override suspend fun updateKeyPoints(meetingId: Long, keyPoints: String) {
        summaryDao.updateKeyPoints(meetingId, keyPoints)
    }
}

@Singleton
class SessionStateRepositoryImpl @Inject constructor(
    private val sessionStateDao: SessionStateDao
) : SessionStateRepository {
    
    override fun getSessionState(meetingId: Long): Flow<SessionState?> = 
        sessionStateDao.getSessionState(meetingId)
    
    override suspend fun getSessionStateSync(meetingId: Long): SessionState? = 
        sessionStateDao.getSessionStateSync(meetingId)
    
    override suspend fun getActiveSession(): SessionState? = 
        sessionStateDao.getActiveSession()
    
    override suspend fun saveSessionState(sessionState: SessionState) {
        sessionStateDao.insertSessionState(sessionState)
    }
    
    override suspend fun updateSessionState(sessionState: SessionState) {
        sessionStateDao.updateSessionState(sessionState)
    }
    
    override suspend fun deleteSessionState(meetingId: Long) {
        sessionStateDao.deleteSessionStateByMeetingId(meetingId)
    }
}
