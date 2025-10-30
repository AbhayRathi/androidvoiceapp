package com.androidvoiceapp.data.repository

import com.androidvoiceapp.data.room.Chunk
import com.androidvoiceapp.data.room.ChunkStatus
import kotlinx.coroutines.flow.Flow

interface ChunkRepository {
    fun getChunksByMeetingId(meetingId: Long): Flow<List<Chunk>>
    suspend fun getChunkById(chunkId: Long): Chunk?
    suspend fun getLastChunkForMeeting(meetingId: Long): Chunk?
    suspend fun createChunk(
        meetingId: Long,
        sequenceNumber: Int,
        startTime: Long,
        tempFilePath: String
    ): Long
    suspend fun updateChunk(chunk: Chunk)
    suspend fun updateChunkStatus(chunkId: Long, status: ChunkStatus)
    suspend fun finalizeChunk(chunkId: Long, filePath: String, fileSize: Long)
    suspend fun getTranscriptionProgress(meetingId: Long): Pair<Int, Int>
}
