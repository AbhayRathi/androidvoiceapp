package com.androidvoiceapp.data.repository

import com.androidvoiceapp.data.room.ChunkEntity
import kotlinx.coroutines.flow.Flow

interface ChunkRepository {
    fun getChunksByMeetingId(meetingId: Long): Flow<List<ChunkEntity>>
    suspend fun getChunkById(chunkId: Long): ChunkEntity?
    suspend fun createChunk(
        meetingId: Long,
        sequenceNumber: Int,
        filePath: String,
        duration: Long,
        startTime: Long
    ): Long
    suspend fun updateChunk(chunk: ChunkEntity)
    suspend fun updateChunkStatus(chunkId: Long, status: String)
    suspend fun getNextSequenceNumber(meetingId: Long): Int
    suspend fun getChunksByStatus(meetingId: Long, status: String): List<ChunkEntity>
}
