package com.abhay.voiceapp.data.dao

import androidx.room.*
import com.abhay.voiceapp.data.entity.Chunk
import com.abhay.voiceapp.data.entity.ChunkStatus
import kotlinx.coroutines.flow.Flow

@Dao
interface ChunkDao {
    
    @Query("SELECT * FROM chunks WHERE meetingId = :meetingId ORDER BY chunkIndex ASC")
    fun getChunksByMeetingId(meetingId: Long): Flow<List<Chunk>>
    
    @Query("SELECT * FROM chunks WHERE id = :id")
    suspend fun getChunkById(id: Long): Chunk?
    
    @Query("SELECT * FROM chunks WHERE meetingId = :meetingId ORDER BY chunkIndex ASC")
    suspend fun getChunksByMeetingIdSync(meetingId: Long): List<Chunk>
    
    @Insert
    suspend fun insertChunk(chunk: Chunk): Long
    
    @Update
    suspend fun updateChunk(chunk: Chunk)
    
    @Query("UPDATE chunks SET status = :status WHERE id = :id")
    suspend fun updateChunkStatus(id: Long, status: ChunkStatus)
    
    @Query("UPDATE chunks SET filePath = :filePath, status = :status WHERE id = :id")
    suspend fun finalizeChunk(id: Long, filePath: String, status: ChunkStatus)
    
    @Delete
    suspend fun deleteChunk(chunk: Chunk)
    
    @Query("SELECT COUNT(*) FROM chunks WHERE meetingId = :meetingId")
    suspend fun getChunkCount(meetingId: Long): Int
}
