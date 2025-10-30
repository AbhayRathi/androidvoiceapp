package com.androidvoiceapp.audio

import com.androidvoiceapp.util.StorageChecker
import java.io.File
import java.io.FileOutputStream
import java.util.concurrent.atomic.AtomicInteger

/**
 * Manages audio chunk creation with overlap.
 * Creates 30-second chunks with ~2-second overlap for speech continuity.
 */
class ChunkManager(
    private val storageChecker: StorageChecker,
    private val sampleRate: Int = 44100
) {
    companion object {
        private const val CHUNK_DURATION_MS = 30_000L // 30 seconds
        private const val OVERLAP_DURATION_MS = 2_000L // 2 seconds
        private const val BYTES_PER_SAMPLE = 2 // 16-bit audio
        private const val CHANNELS = 1 // Mono
    }
    
    private var currentChunkFile: File? = null
    private var currentWavWriter: WavWriter? = null
    private var chunkStartTime = 0L
    private var overlapBuffer: ByteArray? = null
    private val sequenceNumber = AtomicInteger(0)
    
    var onChunkComplete: ((File, Int, Long, Long) -> Unit)? = null
    
    /**
     * Start a new chunk with overlap from previous chunk.
     */
    fun startNewChunk(meetingId: Long): File? {
        // Calculate overlap bytes to preserve
        val overlapBytes = calculateOverlapBytes()
        
        // Create temp file for new chunk
        val tempDir = storageChecker.getTempDirectory()
        val chunkFile = File(tempDir, "chunk_${meetingId}_${sequenceNumber.get()}_temp.wav")
        
        // Start WAV writer
        val wavWriter = WavWriter(chunkFile, sampleRate, CHANNELS, 16)
        wavWriter.start()
        
        // Write overlap from previous chunk if available
        overlapBuffer?.let { overlap ->
            wavWriter.write(overlap, overlap.size)
        }
        
        currentChunkFile = chunkFile
        currentWavWriter = wavWriter
        chunkStartTime = System.currentTimeMillis()
        
        return chunkFile
    }
    
    /**
     * Write audio data to current chunk.
     */
    fun writeAudioData(buffer: ByteArray, size: Int) {
        currentWavWriter?.write(buffer, size)
        
        // Save the last N seconds for overlap
        saveForOverlap(buffer, size)
        
        // Check if chunk duration reached
        val elapsedTime = System.currentTimeMillis() - chunkStartTime
        if (elapsedTime >= CHUNK_DURATION_MS) {
            finalizeCurrentChunk()
        }
    }
    
    /**
     * Finalize the current chunk.
     */
    fun finalizeCurrentChunk() {
        val file = currentChunkFile ?: return
        val startTime = chunkStartTime
        val endTime = System.currentTimeMillis()
        val sequence = sequenceNumber.getAndIncrement()
        
        currentWavWriter?.stop()
        currentWavWriter = null
        
        onChunkComplete?.invoke(file, sequence, startTime, endTime)
        
        currentChunkFile = null
    }
    
    /**
     * Stop chunk recording and finalize.
     */
    fun stop() {
        finalizeCurrentChunk()
        overlapBuffer = null
        sequenceNumber.set(0)
    }
    
    /**
     * Save the last portion of audio for overlap with next chunk.
     */
    private fun saveForOverlap(buffer: ByteArray, size: Int) {
        val overlapBytes = calculateOverlapBytes()
        
        if (overlapBuffer == null) {
            overlapBuffer = ByteArray(overlapBytes)
        }
        
        val existingBuffer = overlapBuffer ?: return
        
        if (size >= overlapBytes) {
            // Copy last N bytes to overlap buffer
            System.arraycopy(buffer, size - overlapBytes, existingBuffer, 0, overlapBytes)
        } else {
            // Shift existing buffer and append new data
            System.arraycopy(existingBuffer, size, existingBuffer, 0, overlapBytes - size)
            System.arraycopy(buffer, 0, existingBuffer, overlapBytes - size, size)
        }
    }
    
    /**
     * Calculate bytes needed for overlap duration.
     */
    private fun calculateOverlapBytes(): Int {
        val samplesPerMs = sampleRate / 1000
        val overlapSamples = samplesPerMs * OVERLAP_DURATION_MS
        return (overlapSamples * BYTES_PER_SAMPLE * CHANNELS).toInt()
    }
    
    /**
     * Check if currently recording a chunk.
     */
    fun isRecordingChunk(): Boolean = currentChunkFile != null
    
    /**
     * Get current chunk sequence number.
     */
    fun getCurrentSequence(): Int = sequenceNumber.get()
    
    /**
     * Reset sequence numbering (for new meeting).
     */
    fun resetSequence() {
        sequenceNumber.set(0)
        overlapBuffer = null
    }
}
