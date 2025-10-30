package com.androidvoiceapp.data.repository

import com.androidvoiceapp.data.room.ChunkDao
import com.androidvoiceapp.data.room.ChunkEntity
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ChunkRepositoryImpl @Inject constructor(
    private val chunkDao: ChunkDao
) : ChunkRepository {
    
    override fun getChunksByMeetingId(meetingId: Long): Flow<List<ChunkEntity>> =
        chunkDao.getChunksByMeetingId(meetingId)
    
    override suspend fun getChunkById(chunkId: Long): ChunkEntity? =
        chunkDao.getChunkById(chunkId)
    
    override suspend fun createChunk(
        meetingId: Long,
        sequenceNumber: Int,
        filePath: String,
        duration: Long,
        startTime: Long
    ): Long {
        val chunk = ChunkEntity(
            meetingId = meetingId,
            sequenceNumber = sequenceNumber,
            filePath = filePath,
            duration = duration,
            startTime = startTime,
            status = "recording"
        )
        return chunkDao.insert(chunk)
    }
    
    override suspend fun updateChunk(chunk: ChunkEntity) {
        chunkDao.update(chunk)
    }
    
    override suspend fun updateChunkStatus(chunkId: Long, status: String) {
        chunkDao.updateStatus(chunkId, status)
    }
    
    override suspend fun getNextSequenceNumber(meetingId: Long): Int {
        val maxSeq = chunkDao.getMaxSequenceNumber(meetingId)
        return (maxSeq ?: -1) + 1
    }
    
    override suspend fun getChunksByStatus(meetingId: Long, status: String): List<ChunkEntity> =
        chunkDao.getChunksByStatus(meetingId, status)
}
