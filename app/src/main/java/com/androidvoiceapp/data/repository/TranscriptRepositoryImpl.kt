package com.androidvoiceapp.data.repository

import com.androidvoiceapp.data.room.TranscriptSegmentDao
import com.androidvoiceapp.data.room.TranscriptSegmentEntity
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class TranscriptRepositoryImpl @Inject constructor(
    private val transcriptSegmentDao: TranscriptSegmentDao
) : TranscriptRepository {
    
    override fun getSegmentsByMeetingId(meetingId: Long): Flow<List<TranscriptSegmentEntity>> =
        transcriptSegmentDao.getSegmentsByMeetingId(meetingId)
    
    override suspend fun getSegmentsByChunkId(chunkId: Long): List<TranscriptSegmentEntity> =
        transcriptSegmentDao.getSegmentsByChunkId(chunkId)
    
    override suspend fun insertSegment(segment: TranscriptSegmentEntity): Long =
        transcriptSegmentDao.insert(segment)
    
    override suspend fun insertSegments(segments: List<TranscriptSegmentEntity>) {
        transcriptSegmentDao.insertAll(segments)
    }
    
    override suspend fun deleteByMeetingId(meetingId: Long) {
        transcriptSegmentDao.deleteByMeetingId(meetingId)
    }
    
    override suspend fun getSegmentCount(meetingId: Long): Int =
        transcriptSegmentDao.getSegmentCount(meetingId)
}
