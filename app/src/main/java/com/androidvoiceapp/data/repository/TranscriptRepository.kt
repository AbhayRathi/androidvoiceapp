package com.androidvoiceapp.data.repository

import com.androidvoiceapp.data.room.TranscriptSegment
import kotlinx.coroutines.flow.Flow

interface TranscriptRepository {
    fun getTranscriptsByMeetingId(meetingId: Long): Flow<List<TranscriptSegment>>
    suspend fun getTranscriptsByChunkId(chunkId: Long): List<TranscriptSegment>
    suspend fun insertTranscriptSegment(segment: TranscriptSegment): Long
    suspend fun insertTranscriptSegments(segments: List<TranscriptSegment>)
    suspend fun deleteTranscriptsByMeetingId(meetingId: Long)
    suspend fun hasTranscripts(meetingId: Long): Boolean
}
