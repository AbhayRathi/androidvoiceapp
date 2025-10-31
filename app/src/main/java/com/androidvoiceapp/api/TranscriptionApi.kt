package com.androidvoiceapp.api

import com.androidvoiceapp.data.room.TranscriptSegmentEntity
import java.io.File

/**
 * Interface for transcription API implementations
 */
interface TranscriptionApi {
    /**
     * Transcribe audio chunk
     * @param chunkFile The WAV file to transcribe
     * @param meetingId The meeting ID
     * @param chunkId The chunk ID
     * @param sequenceNumber The sequence number of the chunk
     * @param chunkStartTime The start time of this chunk in the meeting (ms)
     * @return List of transcript segments
     */
    suspend fun transcribe(
        chunkFile: File,
        meetingId: Long,
        chunkId: Long,
        sequenceNumber: Int,
        chunkStartTime: Long
    ): List<TranscriptSegmentEntity>
    
    /**
     * Check if transcription is available
     */
    fun isAvailable(): Boolean
}
