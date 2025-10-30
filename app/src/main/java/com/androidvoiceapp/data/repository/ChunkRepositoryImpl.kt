package com.androidvoiceapp.data.repository

import com.androidvoiceapp.data.room.Chunk
import com.androidvoiceapp.data.room.ChunkDao
import com.androidvoiceapp.data.room.ChunkStatus
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ChunkRepositoryImpl @Inject constructor(
    private val chunkDao: ChunkDao
) : ChunkRepository {
    
    override fun getChunksByMeetingId(meetingId: Long): Flow<List<Chunk>> {
        return chunkDao.getChunksByMeetingId(meetingId)
    }
    
    override suspend fun getChunkById(chunkId: Long): Chunk? {
        return chunkDao.getChunkById(chunkId)
    }
    
    override suspend fun getLastChunkForMeeting(meetingId: Long): Chunk? {
        return chunkDao.getLastChunkForMeeting(meetingId)
    }
    
    override suspend fun createChunk(
        meetingId: Long,
        sequenceNumber: Int,
        startTime: Long,
        tempFilePath: String
    ): Long {
        val chunk = Chunk(
            meetingId = meetingId,
            sequenceNumber = sequenceNumber,
            filePath = "", // Will be finalized later
            startTime = startTime,
            endTime = startTime,
            durationMs = 0,
            status = ChunkStatus.RECORDING,
            tempFilePath = tempFilePath
        )
        return chunkDao.insertChunk(chunk)
    }
    
    override suspend fun updateChunk(chunk: Chunk) {
        chunkDao.updateChunk(chunk)
    }
    
    override suspend fun updateChunkStatus(chunkId: Long, status: ChunkStatus) {
        chunkDao.updateChunkStatus(chunkId, status)
    }
    
    override suspend fun finalizeChunk(chunkId: Long, filePath: String, fileSize: Long) {
        chunkDao.finalizeChunk(chunkId, filePath, ChunkStatus.FINALIZED, fileSize)
    }
    
    override suspend fun getTranscriptionProgress(meetingId: Long): Pair<Int, Int> {
        val transcribedCount = chunkDao.getTranscribedChunkCount(meetingId)
        val totalCount = chunkDao.getTotalChunkCount(meetingId)
        return Pair(transcribedCount, totalCount)
    }
}
