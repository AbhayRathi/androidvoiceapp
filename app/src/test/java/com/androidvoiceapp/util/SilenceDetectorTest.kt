package com.androidvoiceapp.util

import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

class SilenceDetectorTest {
    
    private lateinit var silenceDetector: SilenceDetector
    
    @Before
    fun setup() {
        silenceDetector = SilenceDetector(
            silenceThresholdAmplitude = 500,
            silenceDurationMs = 10000
        )
    }
    
    @Test
    fun `test silence detection with silent samples`() {
        // Create silent samples (amplitude below threshold)
        val silentSamples = ShortArray(1000) { 100 }
        
        // Process samples for less than silence duration
        var isSilent = false
        for (i in 0 until 9) {
            isSilent = silenceDetector.processSamples(silentSamples)
            Thread.sleep(1000) // Simulate 1 second between samples
        }
        
        // Should not be detected as silent yet
        assertFalse("Should not detect silence before threshold", isSilent)
        
        // Process one more second to exceed threshold
        isSilent = silenceDetector.processSamples(silentSamples)
        Thread.sleep(1000)
        
        // Should now be detected as silent
        assertTrue("Should detect silence after threshold", isSilent || silenceDetector.isSilenceDetected())
    }
    
    @Test
    fun `test no silence detection with loud samples`() {
        // Create loud samples (amplitude above threshold)
        val loudSamples = ShortArray(1000) { 2000 }
        
        // Process samples for more than silence duration
        var isSilent = false
        for (i in 0 until 12) {
            isSilent = silenceDetector.processSamples(loudSamples)
            Thread.sleep(1000) // Simulate 1 second between samples
        }
        
        // Should never be detected as silent
        assertFalse("Should not detect silence with loud audio", isSilent)
        assertFalse("Silence flag should not be set", silenceDetector.isSilenceDetected())
    }
    
    @Test
    fun `test silence detection reset with loud sample`() {
        // Create silent samples
        val silentSamples = ShortArray(1000) { 100 }
        
        // Process silent samples for 5 seconds
        for (i in 0 until 5) {
            silenceDetector.processSamples(silentSamples)
            Thread.sleep(1000)
        }
        
        // Introduce loud sample (should reset detection)
        val loudSamples = ShortArray(1000) { 2000 }
        silenceDetector.processSamples(loudSamples)
        
        // Continue with silent samples
        for (i in 0 until 9) {
            silenceDetector.processSamples(silentSamples)
            Thread.sleep(1000)
        }
        
        // Should not be detected as silent yet (reset happened)
        assertFalse("Should not detect silence after reset", silenceDetector.isSilenceDetected())
    }
    
    @Test
    fun `test manual reset`() {
        // Create silent samples
        val silentSamples = ShortArray(1000) { 100 }
        
        // Process samples to trigger silence
        for (i in 0 until 11) {
            silenceDetector.processSamples(silentSamples)
            Thread.sleep(1000)
        }
        
        // Reset manually
        silenceDetector.reset()
        
        // Should not be silent after reset
        assertFalse("Should not be silent after manual reset", silenceDetector.isSilenceDetected())
    }
    
    @Test
    fun `test amplitude threshold boundary`() {
        // Test with amplitude exactly at threshold
        val thresholdSamples = ShortArray(1000) { 500 }
        
        // This should be considered silent (below threshold, not equal)
        for (i in 0 until 11) {
            silenceDetector.processSamples(thresholdSamples)
            Thread.sleep(1000)
        }
        
        // Should detect as silent since amplitude is not above threshold
        // Note: The implementation uses < not <=, so 500 would be silent
        val result = silenceDetector.isSilenceDetected()
        // This test documents the behavior - adjust based on your requirements
        assertTrue("Threshold amplitude should be considered silent", result)
    }
    
    @Test
    fun `test mixed amplitude samples`() {
        val silentSamples = ShortArray(1000) { 100 }
        
        // Alternating silent and loud samples
        for (i in 0 until 10) {
            if (i % 2 == 0) {
                silenceDetector.processSamples(silentSamples)
            } else {
                val loudSamples = ShortArray(1000) { 2000 }
                silenceDetector.processSamples(loudSamples)
            }
            Thread.sleep(1000)
        }
        
        // Should not detect silence due to regular loud samples resetting detection
        assertFalse("Should not detect silence with mixed samples", silenceDetector.isSilenceDetected())
    }
}
