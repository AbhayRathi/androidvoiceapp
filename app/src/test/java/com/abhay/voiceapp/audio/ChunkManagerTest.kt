package com.abhay.voiceapp.audio

import org.junit.Assert.*
import org.junit.Test

class ChunkManagerTest {
    
    @Test
    fun `test chunk duration and overlap calculations`() {
        // Verify chunk duration is 30 seconds
        assertEquals(30000L, ChunkManager.CHUNK_DURATION_MS)
        
        // Verify overlap is 2 seconds
        assertEquals(2000L, ChunkManager.OVERLAP_DURATION_MS)
        
        // Verify chunk size in bytes
        val expectedChunkSize = 44100 * 1 * 2 * 30 // sampleRate * channels * bytesPerSample * seconds
        assertEquals(expectedChunkSize, ChunkManager.CHUNK_SIZE_BYTES)
        
        // Verify overlap size in bytes
        val expectedOverlapSize = 44100 * 1 * 2 * 2 // sampleRate * channels * bytesPerSample * seconds
        assertEquals(expectedOverlapSize, ChunkManager.OVERLAP_SIZE_BYTES)
    }
    
    @Test
    fun `test chunk size is correct for 30 seconds`() {
        // At 44100 Hz, mono, 16-bit:
        // 44100 samples/sec * 2 bytes/sample * 30 sec = 2,646,000 bytes
        val expected = 44100 * 2 * 30
        assertEquals(expected, ChunkManager.CHUNK_SIZE_BYTES)
    }
    
    @Test
    fun `test overlap size is correct for 2 seconds`() {
        // At 44100 Hz, mono, 16-bit:
        // 44100 samples/sec * 2 bytes/sample * 2 sec = 176,400 bytes
        val expected = 44100 * 2 * 2
        assertEquals(expected, ChunkManager.OVERLAP_SIZE_BYTES)
    }
    
    @Test
    fun `test overlap is less than chunk size`() {
        assertTrue(ChunkManager.OVERLAP_SIZE_BYTES < ChunkManager.CHUNK_SIZE_BYTES)
    }
    
    @Test
    fun `test overlap percentage`() {
        val overlapPercentage = (ChunkManager.OVERLAP_SIZE_BYTES.toDouble() / 
                                 ChunkManager.CHUNK_SIZE_BYTES.toDouble()) * 100
        
        // Overlap should be approximately 6.67% (2/30)
        assertTrue(overlapPercentage > 6.0 && overlapPercentage < 7.0)
    }
}
