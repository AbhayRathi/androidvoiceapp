package com.voicerecorder.data.room

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface ChunkDao {
    @Query("SELECT * FROM chunks WHERE meetingId = :meetingId ORDER BY chunkNumber ASC")
    fun getChunksForMeeting(meetingId: Long): Flow<List<Chunk>>

    @Query("SELECT * FROM chunks WHERE id = :chunkId")
    suspend fun getChunkById(chunkId: Long): Chunk?

    @Query("SELECT * FROM chunks WHERE meetingId = :meetingId AND chunkNumber = :chunkNumber")
    suspend fun getChunkByNumber(meetingId: Long, chunkNumber: Int): Chunk?

    @Insert
    suspend fun insert(chunk: Chunk): Long

    @Update
    suspend fun update(chunk: Chunk)

    @Delete
    suspend fun delete(chunk: Chunk)

    @Query("UPDATE chunks SET status = :status WHERE id = :chunkId")
    suspend fun updateStatus(chunkId: Long, status: ChunkStatus)

    @Query("UPDATE chunks SET transcriptionStatus = :status, retryCount = :retryCount WHERE id = :chunkId")
    suspend fun updateTranscriptionStatus(chunkId: Long, status: TranscriptionStatus, retryCount: Int)

    @Query("UPDATE chunks SET transcriptionStatus = :status WHERE meetingId = :meetingId")
    suspend fun updateAllTranscriptionStatus(meetingId: Long, status: TranscriptionStatus)

    @Query("SELECT * FROM chunks WHERE meetingId = :meetingId AND transcriptionStatus = :status ORDER BY chunkNumber ASC")
    suspend fun getChunksByTranscriptionStatus(meetingId: Long, status: TranscriptionStatus): List<Chunk>
}
