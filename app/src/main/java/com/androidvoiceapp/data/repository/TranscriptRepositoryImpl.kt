package com.androidvoiceapp.data.repository

import com.androidvoiceapp.data.room.TranscriptSegment
import com.androidvoiceapp.data.room.TranscriptSegmentDao
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class TranscriptRepositoryImpl @Inject constructor(
    private val transcriptSegmentDao: TranscriptSegmentDao
) : TranscriptRepository {
    
    override fun getTranscriptsByMeetingId(meetingId: Long): Flow<List<TranscriptSegment>> {
        return transcriptSegmentDao.getTranscriptsByMeetingId(meetingId)
    }
    
    override suspend fun getTranscriptsByChunkId(chunkId: Long): List<TranscriptSegment> {
        return transcriptSegmentDao.getTranscriptsByChunkId(chunkId)
    }
    
    override suspend fun insertTranscriptSegment(segment: TranscriptSegment): Long {
        return transcriptSegmentDao.insertTranscriptSegment(segment)
    }
    
    override suspend fun insertTranscriptSegments(segments: List<TranscriptSegment>) {
        transcriptSegmentDao.insertTranscriptSegments(segments)
    }
    
    override suspend fun deleteTranscriptsByMeetingId(meetingId: Long) {
        transcriptSegmentDao.deleteTranscriptsByMeetingId(meetingId)
    }
    
    override suspend fun hasTranscripts(meetingId: Long): Boolean {
        return transcriptSegmentDao.getTranscriptCountForMeeting(meetingId) > 0
    }
}
