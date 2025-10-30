package com.abhay.voiceapp.audio

import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

class SilenceDetectorTest {
    
    private lateinit var silenceDetector: SilenceDetector
    
    @Before
    fun setup() {
        silenceDetector = SilenceDetector()
    }
    
    @Test
    fun `test silence detection with silent buffer`() {
        // Create a buffer with low amplitude (silence)
        val silentBuffer = ByteArray(1000) { 0 }
        
        // Initially should not detect silence
        assertFalse(silenceDetector.detectSilence(silentBuffer, silentBuffer.size))
        
        // After 10 seconds of silence, should detect
        Thread.sleep(10100) // Wait more than 10 seconds
        assertTrue(silenceDetector.detectSilence(silentBuffer, silentBuffer.size))
    }
    
    @Test
    fun `test silence detection with loud buffer`() {
        // Create a buffer with high amplitude (sound)
        val loudBuffer = ByteArray(1000) { i ->
            (Math.sin(i * 0.1) * 5000).toByte()
        }
        
        // Should not detect silence with loud buffer
        assertFalse(silenceDetector.detectSilence(loudBuffer, loudBuffer.size))
    }
    
    @Test
    fun `test silence detector reset`() {
        val silentBuffer = ByteArray(1000) { 0 }
        
        // Detect some silence
        silenceDetector.detectSilence(silentBuffer, silentBuffer.size)
        assertTrue(silenceDetector.isSilent())
        
        // Reset
        silenceDetector.reset()
        assertFalse(silenceDetector.isSilent())
        assertEquals(0, silenceDetector.getSilenceDuration())
    }
    
    @Test
    fun `test silence duration tracking`() {
        val silentBuffer = ByteArray(1000) { 0 }
        
        assertFalse(silenceDetector.isSilent())
        assertEquals(0, silenceDetector.getSilenceDuration())
        
        // Trigger silence detection
        silenceDetector.detectSilence(silentBuffer, silentBuffer.size)
        
        assertTrue(silenceDetector.isSilent())
        assertTrue(silenceDetector.getSilenceDuration() > 0)
    }
    
    @Test
    fun `test alternating silence and sound`() {
        val silentBuffer = ByteArray(1000) { 0 }
        val loudBuffer = ByteArray(1000) { i ->
            (Math.sin(i * 0.1) * 5000).toByte()
        }
        
        // Start with silence
        silenceDetector.detectSilence(silentBuffer, silentBuffer.size)
        assertTrue(silenceDetector.isSilent())
        
        // Introduce sound - should reset silence
        silenceDetector.detectSilence(loudBuffer, loudBuffer.size)
        assertFalse(silenceDetector.isSilent())
    }
}
