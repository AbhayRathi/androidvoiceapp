package com.abhay.voiceapp.audio

import android.content.Context
import android.util.Log
import com.abhay.voiceapp.data.entity.Chunk
import com.abhay.voiceapp.data.entity.ChunkStatus
import com.abhay.voiceapp.data.repository.ChunkRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.util.concurrent.atomic.AtomicInteger
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ChunkManager @Inject constructor(
    @ApplicationContext private val context: Context,
    private val chunkRepository: ChunkRepository,
    private val wavFileWriter: WavFileWriter
) {
    companion object {
        private const val TAG = "ChunkManager"
        const val CHUNK_DURATION_MS = 30000L // 30 seconds
        const val OVERLAP_DURATION_MS = 2000L // 2 seconds
        
        // Calculate bytes for durations
        private const val BYTES_PER_SECOND = AudioRecorder.SAMPLE_RATE * 
                AudioRecorder.CHANNELS * AudioRecorder.BITS_PER_SAMPLE / 8
        const val CHUNK_SIZE_BYTES = (BYTES_PER_SECOND * CHUNK_DURATION_MS / 1000).toInt()
        const val OVERLAP_SIZE_BYTES = (BYTES_PER_SECOND * OVERLAP_DURATION_MS / 1000).toInt()
    }
    
    private val chunkBuffer = mutableListOf<Byte>()
    private val overlapBuffer = mutableListOf<Byte>()
    private var currentChunkIndex = AtomicInteger(0)
    private var currentMeetingId: Long = -1
    private var chunkStartTime: Long = 0
    
    fun startNewRecording(meetingId: Long) {
        currentMeetingId = meetingId
        currentChunkIndex.set(0)
        chunkBuffer.clear()
        overlapBuffer.clear()
        chunkStartTime = System.currentTimeMillis()
        Log.d(TAG, "Started new recording for meeting: $meetingId")
    }
    
    suspend fun processAudioData(audioData: ByteArray, bytesRead: Int): Chunk? {
        if (currentMeetingId == -1L) {
            Log.w(TAG, "No active meeting")
            return null
        }
        
        // Add data to chunk buffer
        for (i in 0 until bytesRead) {
            chunkBuffer.add(audioData[i])
        }
        
        // Check if chunk is complete
        if (chunkBuffer.size >= CHUNK_SIZE_BYTES) {
            return finalizeCurrentChunk()
        }
        
        return null
    }
    
    private suspend fun finalizeCurrentChunk(): Chunk? = withContext(Dispatchers.IO) {
        try {
            val chunkIndex = currentChunkIndex.getAndIncrement()
            val chunkEndTime = System.currentTimeMillis()
            
            // Create temp file for this chunk
            val tempFile = getTempChunkFile(currentMeetingId, chunkIndex)
            val finalFile = getFinalChunkFile(currentMeetingId, chunkIndex)
            
            // Prepare chunk data (main chunk + overlap from previous)
            val chunkData = if (overlapBuffer.isNotEmpty() && chunkIndex > 0) {
                // Include overlap from previous chunk
                (overlapBuffer + chunkBuffer.take(CHUNK_SIZE_BYTES)).toByteArray()
            } else {
                chunkBuffer.take(CHUNK_SIZE_BYTES).toByteArray()
            }
            
            // Write raw PCM data to temp file
            wavFileWriter.appendToRawFile(tempFile, chunkData)
            
            // Save overlap data for next chunk (last 2 seconds)
            overlapBuffer.clear()
            if (chunkBuffer.size >= OVERLAP_SIZE_BYTES) {
                val overlapStart = chunkBuffer.size - OVERLAP_SIZE_BYTES
                overlapBuffer.addAll(chunkBuffer.subList(overlapStart, chunkBuffer.size))
            }
            
            // Remove processed data from buffer (keep any excess for next chunk)
            if (chunkBuffer.size > CHUNK_SIZE_BYTES) {
                val excess = chunkBuffer.subList(CHUNK_SIZE_BYTES, chunkBuffer.size)
                chunkBuffer.clear()
                chunkBuffer.addAll(excess)
            } else {
                chunkBuffer.clear()
            }
            
            // Create chunk entity
            val chunk = Chunk(
                meetingId = currentMeetingId,
                chunkIndex = chunkIndex,
                filePath = finalFile.absolutePath,
                tempFilePath = tempFile.absolutePath,
                startTime = chunkStartTime,
                endTime = chunkEndTime,
                duration = chunkEndTime - chunkStartTime,
                fileSize = tempFile.length(),
                status = ChunkStatus.RECORDING
            )
            
            // Save to database
            val chunkId = chunkRepository.createChunk(chunk)
            
            // Update start time for next chunk
            chunkStartTime = chunkEndTime
            
            Log.d(TAG, "Finalized chunk $chunkIndex for meeting $currentMeetingId")
            
            chunk.copy(id = chunkId)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to finalize chunk", e)
            null
        }
    }
    
    suspend fun finalizeFinalChunk(): Chunk? {
        if (chunkBuffer.isEmpty()) return null
        
        // Pad the buffer if needed or just finalize what we have
        return finalizeCurrentChunk()
    }
    
    fun getTempChunkFile(meetingId: Long, chunkIndex: Int): File {
        val dir = File(context.filesDir, "recordings/$meetingId")
        dir.mkdirs()
        return File(dir, "chunk_${chunkIndex}_temp.raw")
    }
    
    fun getFinalChunkFile(meetingId: Long, chunkIndex: Int): File {
        val dir = File(context.filesDir, "recordings/$meetingId")
        dir.mkdirs()
        return File(dir, "chunk_${chunkIndex}.wav")
    }
    
    fun reset() {
        chunkBuffer.clear()
        overlapBuffer.clear()
        currentChunkIndex.set(0)
        currentMeetingId = -1
        chunkStartTime = 0
        Log.d(TAG, "ChunkManager reset")
    }
}
