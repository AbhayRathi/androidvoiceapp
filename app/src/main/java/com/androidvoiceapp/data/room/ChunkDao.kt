package com.androidvoiceapp.data.room

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface ChunkDao {
    
    @Query("SELECT * FROM chunks WHERE meetingId = :meetingId ORDER BY sequenceNumber ASC")
    fun getChunksByMeetingId(meetingId: Long): Flow<List<ChunkEntity>>
    
    @Query("SELECT * FROM chunks WHERE id = :chunkId")
    suspend fun getChunkById(chunkId: Long): ChunkEntity?
    
    @Query("SELECT * FROM chunks WHERE meetingId = :meetingId AND status = :status ORDER BY sequenceNumber ASC")
    suspend fun getChunksByStatus(meetingId: Long, status: String): List<ChunkEntity>
    
    @Insert
    suspend fun insert(chunk: ChunkEntity): Long
    
    @Update
    suspend fun update(chunk: ChunkEntity)
    
    @Query("UPDATE chunks SET status = :status WHERE id = :chunkId")
    suspend fun updateStatus(chunkId: Long, status: String)
    
    @Query("SELECT MAX(sequenceNumber) FROM chunks WHERE meetingId = :meetingId")
    suspend fun getMaxSequenceNumber(meetingId: Long): Int?
}
