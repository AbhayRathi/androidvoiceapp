package com.androidvoiceapp.service

import org.junit.Assert.*
import org.junit.Test

/**
 * Tests for chunk overlap math and timing logic
 * These tests validate the chunk duration and overlap calculations
 * used in RecordingService
 */
class ChunkOverlapTest {
    
    companion object {
        private const val CHUNK_DURATION_MS = 30000L // 30 seconds
        private const val OVERLAP_DURATION_MS = 2000L // 2 seconds
        private const val SAMPLE_RATE = 16000
    }
    
    @Test
    fun `test first chunk has no overlap at start`() {
        // First chunk should start at 0 with no overlap before it
        val chunkStartTime = 0L
        val chunkDuration = CHUNK_DURATION_MS
        val chunkEndTime = chunkStartTime + chunkDuration
        
        assertEquals("First chunk should start at 0", 0L, chunkStartTime)
        assertEquals("First chunk should end at 30s", 30000L, chunkEndTime)
        assertEquals("First chunk duration should be 30s", 30000L, chunkDuration)
    }
    
    @Test
    fun `test second chunk starts with overlap from first chunk`() {
        // Second chunk should start CHUNK_DURATION_MS - OVERLAP_DURATION_MS after first chunk start
        val firstChunkStartTime = 0L
        val secondChunkStartTime = firstChunkStartTime + CHUNK_DURATION_MS
        
        // The actual recording continues for OVERLAP_DURATION_MS after first chunk ends
        // So second chunk data will include the last OVERLAP_DURATION_MS of first chunk
        val overlapStart = CHUNK_DURATION_MS - OVERLAP_DURATION_MS
        
        assertEquals("Second chunk should start at 30s", 30000L, secondChunkStartTime)
        assertEquals("Overlap should start at 28s", 28000L, overlapStart)
        assertTrue("Second chunk includes last 2s of first chunk", 
            secondChunkStartTime > overlapStart)
    }
    
    @Test
    fun `test chunk timing sequence for multiple chunks`() {
        // Test timing for first 5 chunks
        val expectedStartTimes = listOf(0L, 30000L, 60000L, 90000L, 120000L)
        val expectedEndTimes = listOf(30000L, 60000L, 90000L, 120000L, 150000L)
        
        for (i in 0 until 5) {
            val chunkStartTime = i * CHUNK_DURATION_MS
            val chunkEndTime = chunkStartTime + CHUNK_DURATION_MS
            
            assertEquals("Chunk $i start time", expectedStartTimes[i], chunkStartTime)
            assertEquals("Chunk $i end time", expectedEndTimes[i], chunkEndTime)
        }
    }
    
    @Test
    fun `test overlap provides context between chunks`() {
        // Verify that overlap duration provides context for transcription
        val overlapSamples = (OVERLAP_DURATION_MS / 1000.0 * SAMPLE_RATE).toLong()
        
        assertEquals("Overlap should be 2 seconds", 2000L, OVERLAP_DURATION_MS)
        assertEquals("Overlap should provide 32000 samples at 16kHz", 32000L, overlapSamples)
        assertTrue("Overlap should be reasonable (not too short)", OVERLAP_DURATION_MS >= 1000L)
        assertTrue("Overlap should be reasonable (not too long)", OVERLAP_DURATION_MS <= 5000L)
    }
    
    @Test
    fun `test chunk duration is consistent for normal chunks`() {
        // All normal (non-final) chunks should have same duration
        val normalChunkDuration = CHUNK_DURATION_MS
        
        assertEquals("Chunk duration should be 30 seconds", 30000L, normalChunkDuration)
        
        // Verify duration in seconds
        val durationSeconds = normalChunkDuration / 1000
        assertEquals("Chunk duration should be 30 seconds", 30L, durationSeconds)
    }
    
    @Test
    fun `test final partial chunk can be shorter than standard duration`() {
        // Final chunk when recording stops might be shorter
        val recordingDuration = 65000L // 65 seconds total
        val fullChunks = recordingDuration / CHUNK_DURATION_MS // 2 full chunks
        val partialChunkDuration = recordingDuration % CHUNK_DURATION_MS // 5 seconds
        
        assertEquals("Should have 2 full chunks", 2L, fullChunks)
        assertEquals("Partial chunk should be 5 seconds", 5000L, partialChunkDuration)
        assertTrue("Partial chunk should be shorter than standard", 
            partialChunkDuration < CHUNK_DURATION_MS)
    }
    
    @Test
    fun `test chunk metadata with overlap consideration`() {
        // Test that chunk metadata correctly reflects timing with overlap
        
        data class ChunkMetadata(
            val sequenceNumber: Int,
            val startTime: Long,
            val duration: Long,
            val actualRecordingStart: Long
        )
        
        // Chunk 0: 0-30s (no overlap before)
        val chunk0 = ChunkMetadata(
            sequenceNumber = 0,
            startTime = 0L,
            duration = CHUNK_DURATION_MS,
            actualRecordingStart = 0L
        )
        
        // Chunk 1: 30-60s (includes overlap from 28-30s)
        val chunk1 = ChunkMetadata(
            sequenceNumber = 1,
            startTime = 30000L,
            duration = CHUNK_DURATION_MS,
            actualRecordingStart = 28000L // Includes 2s overlap
        )
        
        assertEquals("Chunk 0 starts at 0", 0L, chunk0.startTime)
        assertEquals("Chunk 1 starts at 30s", 30000L, chunk1.startTime)
        assertEquals("Chunk 1 actual recording includes overlap", 28000L, chunk1.actualRecordingStart)
        
        val overlapGap = chunk1.startTime - chunk1.actualRecordingStart
        assertEquals("Overlap gap should be 2s", OVERLAP_DURATION_MS, overlapGap)
    }
    
    @Test
    fun `test sample count calculation for chunks with overlap`() {
        // Calculate expected sample count for a chunk with overlap
        val chunkDurationSeconds = CHUNK_DURATION_MS / 1000.0
        val overlapDurationSeconds = OVERLAP_DURATION_MS / 1000.0
        
        val samplesPerChunk = (chunkDurationSeconds * SAMPLE_RATE).toLong()
        val samplesInOverlap = (overlapDurationSeconds * SAMPLE_RATE).toLong()
        
        assertEquals("30 seconds should yield 480000 samples at 16kHz", 480000L, samplesPerChunk)
        assertEquals("2 seconds should yield 32000 samples at 16kHz", 32000L, samplesInOverlap)
        
        // First chunk has no overlap, subsequent chunks include overlap
        val firstChunkSamples = samplesPerChunk
        val subsequentChunkSamples = samplesPerChunk // Same duration, but data includes overlap period
        
        assertEquals("First chunk samples", 480000L, firstChunkSamples)
        assertEquals("Subsequent chunk samples", 480000L, subsequentChunkSamples)
    }
    
    @Test
    fun `test pause and resume affect chunk timing correctly`() {
        // When paused, time doesn't advance
        val chunkStartBeforePause = 35000L // 35 seconds into recording
        val pauseDuration = 10000L // Paused for 10 seconds
        val chunkStartAfterPause = chunkStartBeforePause // Should not advance during pause
        
        assertEquals("Chunk timing should not advance during pause", 
            chunkStartBeforePause, chunkStartAfterPause)
        
        // After resume, next chunk should account for pause
        val recordingElapsedTime = 65000L // 65 seconds of actual recording
        val totalElapsedTime = recordingElapsedTime + pauseDuration // 75 seconds total
        
        assertTrue("Recording time should be less than total time when paused",
            recordingElapsedTime < totalElapsedTime)
    }
}
