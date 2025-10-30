package com.voicerecorder.util

import com.google.common.truth.Truth.assertThat
import org.junit.Before
import org.junit.Test

class SilenceDetectorTest {

    private lateinit var silenceDetector: SilenceDetector
    private val sampleRate = 16000

    @Before
    fun setup() {
        silenceDetector = SilenceDetector(
            silenceThresholdDb = -40.0,
            silenceDurationMs = 10000
        )
    }

    @Test
    fun `detectSilence returns false for loud audio`() {
        // Create audio buffer with high amplitude
        val buffer = ShortArray(1024) { (Short.MAX_VALUE / 2).toShort() }
        
        val result = silenceDetector.detectSilence(buffer, sampleRate)
        
        assertThat(result).isFalse()
    }

    @Test
    fun `detectSilence returns false for silent audio below threshold duration`() {
        // Create silent audio buffer
        val buffer = ShortArray(1024) { 0 }
        
        // First detection should return false (not enough time elapsed)
        val result = silenceDetector.detectSilence(buffer, sampleRate)
        
        assertThat(result).isFalse()
    }

    @Test
    fun `detectSilence returns true after silence threshold duration`() {
        // Create silent audio buffer
        val buffer = ShortArray(1024) { 0 }
        
        // Manually set silence start time to simulate 10+ seconds of silence
        // This is a simplified test - in real scenarios you'd need to call detectSilence
        // repeatedly over time
        val startTime = System.currentTimeMillis() - 11000
        
        // Since we can't directly set the internal state, we test the behavior
        // by calling detectSilence multiple times
        repeat(10) {
            silenceDetector.detectSilence(buffer, sampleRate)
            Thread.sleep(1100) // Sleep for 1.1 seconds
        }
        
        val result = silenceDetector.detectSilence(buffer, sampleRate)
        assertThat(result).isTrue()
    }

    @Test
    fun `reset clears silence state`() {
        val buffer = ShortArray(1024) { 0 }
        
        // Detect silence to start tracking
        silenceDetector.detectSilence(buffer, sampleRate)
        
        // Reset should clear the state
        silenceDetector.reset()
        
        // Silence duration should be 0 after reset
        assertThat(silenceDetector.getSilenceDuration()).isEqualTo(0)
    }

    @Test
    fun `detectSilence resets when loud audio detected`() {
        // Start with silent audio
        val silentBuffer = ShortArray(1024) { 0 }
        silenceDetector.detectSilence(silentBuffer, sampleRate)
        
        // Then loud audio
        val loudBuffer = ShortArray(1024) { (Short.MAX_VALUE / 2).toShort() }
        silenceDetector.detectSilence(loudBuffer, sampleRate)
        
        // Silence duration should be reset to 0
        assertThat(silenceDetector.getSilenceDuration()).isEqualTo(0)
    }

    @Test
    fun `getSilenceDuration returns correct duration`() {
        val buffer = ShortArray(1024) { 0 }
        
        val startTime = System.currentTimeMillis()
        silenceDetector.detectSilence(buffer, sampleRate)
        
        // Wait a bit
        Thread.sleep(100)
        
        val duration = silenceDetector.getSilenceDuration()
        
        // Duration should be approximately 100ms (with some tolerance)
        assertThat(duration).isAtLeast(50)
        assertThat(duration).isAtMost(200)
    }
}
