package com.androidvoiceapp.util

import kotlin.math.abs

/**
 * Detects silence in audio data.
 * Silence is detected when audio amplitude stays below threshold for a specified duration.
 */
class SilenceDetector(
    private val silenceThreshold: Short = 500,
    private val silenceDurationMs: Long = 10_000 // 10 seconds
) {
    private var silenceStartTime: Long? = null
    private var lastCheckTime = System.currentTimeMillis()
    
    /**
     * Process audio buffer and check for silence.
     * @param buffer Audio data in 16-bit PCM format
     * @param size Number of bytes to process
     * @return true if silence has been detected for the configured duration
     */
    fun processSamples(buffer: ByteArray, size: Int): Boolean {
        val currentTime = System.currentTimeMillis()
        val isSilent = isSilentBuffer(buffer, size)
        
        if (isSilent) {
            if (silenceStartTime == null) {
                silenceStartTime = currentTime
            }
            
            val silenceDuration = currentTime - (silenceStartTime ?: currentTime)
            return silenceDuration >= silenceDurationMs
        } else {
            silenceStartTime = null
        }
        
        lastCheckTime = currentTime
        return false
    }
    
    /**
     * Check if the audio buffer is silent (below threshold).
     */
    private fun isSilentBuffer(buffer: ByteArray, size: Int): Boolean {
        var maxAmplitude: Short = 0
        
        // Process as 16-bit samples (2 bytes per sample)
        for (i in 0 until size - 1 step 2) {
            val sample = ((buffer[i + 1].toInt() shl 8) or (buffer[i].toInt() and 0xFF)).toShort()
            val amplitude = abs(sample.toInt()).toShort()
            if (amplitude > maxAmplitude) {
                maxAmplitude = amplitude
            }
        }
        
        return maxAmplitude < silenceThreshold
    }
    
    /**
     * Reset the silence detector state.
     */
    fun reset() {
        silenceStartTime = null
        lastCheckTime = System.currentTimeMillis()
    }
    
    /**
     * Get current silence duration in milliseconds.
     */
    fun getCurrentSilenceDuration(): Long {
        val start = silenceStartTime ?: return 0
        return System.currentTimeMillis() - start
    }
    
    /**
     * Check if currently in silence state.
     */
    fun isSilent(): Boolean {
        return silenceStartTime != null
    }
}
