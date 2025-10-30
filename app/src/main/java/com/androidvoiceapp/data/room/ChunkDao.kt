package com.androidvoiceapp.data.room

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface ChunkDao {
    @Query("SELECT * FROM chunks WHERE meetingId = :meetingId ORDER BY sequenceNumber ASC")
    fun getChunksByMeetingId(meetingId: Long): Flow<List<Chunk>>
    
    @Query("SELECT * FROM chunks WHERE id = :chunkId")
    suspend fun getChunkById(chunkId: Long): Chunk?
    
    @Query("SELECT * FROM chunks WHERE meetingId = :meetingId ORDER BY sequenceNumber DESC LIMIT 1")
    suspend fun getLastChunkForMeeting(meetingId: Long): Chunk?
    
    @Insert
    suspend fun insertChunk(chunk: Chunk): Long
    
    @Update
    suspend fun updateChunk(chunk: Chunk)
    
    @Query("UPDATE chunks SET status = :status WHERE id = :chunkId")
    suspend fun updateChunkStatus(chunkId: Long, status: ChunkStatus)
    
    @Query("UPDATE chunks SET filePath = :filePath, status = :status, fileSize = :fileSize WHERE id = :chunkId")
    suspend fun finalizeChunk(chunkId: Long, filePath: String, status: ChunkStatus, fileSize: Long)
    
    @Query("SELECT COUNT(*) FROM chunks WHERE meetingId = :meetingId AND status IN ('TRANSCRIBING', 'TRANSCRIBED')")
    suspend fun getTranscribedChunkCount(meetingId: Long): Int
    
    @Query("SELECT COUNT(*) FROM chunks WHERE meetingId = :meetingId")
    suspend fun getTotalChunkCount(meetingId: Long): Int
}
