package com.androidvoiceapp.data.repository

import com.androidvoiceapp.data.room.TranscriptSegmentEntity
import kotlinx.coroutines.flow.Flow

interface TranscriptRepository {
    fun getSegmentsByMeetingId(meetingId: Long): Flow<List<TranscriptSegmentEntity>>
    suspend fun getSegmentsByChunkId(chunkId: Long): List<TranscriptSegmentEntity>
    suspend fun insertSegment(segment: TranscriptSegmentEntity): Long
    suspend fun insertSegments(segments: List<TranscriptSegmentEntity>)
    suspend fun deleteByMeetingId(meetingId: Long)
    suspend fun getSegmentCount(meetingId: Long): Int
}
