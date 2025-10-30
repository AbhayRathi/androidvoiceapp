package com.abhay.voiceapp.audio

import android.util.Log
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.abs

@Singleton
class SilenceDetector @Inject constructor() {
    
    companion object {
        private const val TAG = "SilenceDetector"
        private const val SILENCE_THRESHOLD = 500 // Amplitude threshold for silence
        private const val SILENCE_DURATION_MS = 10000L // 10 seconds
    }
    
    private var silenceStartTime: Long? = null
    private var lastCheckTime = System.currentTimeMillis()
    
    /**
     * Analyzes audio buffer to detect silence
     * Returns true if silence has been detected for more than SILENCE_DURATION_MS
     */
    fun detectSilence(audioBuffer: ByteArray, bytesRead: Int): Boolean {
        val currentTime = System.currentTimeMillis()
        
        // Calculate RMS (Root Mean Square) amplitude
        val isSilent = isBufferSilent(audioBuffer, bytesRead)
        
        if (isSilent) {
            if (silenceStartTime == null) {
                silenceStartTime = currentTime
                Log.d(TAG, "Silence detected, starting timer")
            }
            
            val silenceDuration = currentTime - (silenceStartTime ?: currentTime)
            if (silenceDuration >= SILENCE_DURATION_MS) {
                Log.w(TAG, "Silence duration exceeded threshold: ${silenceDuration}ms")
                return true
            }
        } else {
            // Reset silence timer if sound is detected
            if (silenceStartTime != null) {
                Log.d(TAG, "Sound detected, resetting silence timer")
                silenceStartTime = null
            }
        }
        
        lastCheckTime = currentTime
        return false
    }
    
    /**
     * Check if the audio buffer contains silence based on amplitude threshold
     */
    private fun isBufferSilent(buffer: ByteArray, bytesRead: Int): Boolean {
        if (bytesRead <= 0) return true
        
        // Convert bytes to 16-bit samples and calculate RMS
        var sum = 0L
        var sampleCount = 0
        
        for (i in 0 until bytesRead - 1 step 2) {
            val sample = ((buffer[i + 1].toInt() shl 8) or (buffer[i].toInt() and 0xFF)).toShort()
            sum += (sample.toInt() * sample.toInt())
            sampleCount++
        }
        
        if (sampleCount == 0) return true
        
        val rms = Math.sqrt((sum.toDouble() / sampleCount))
        
        return rms < SILENCE_THRESHOLD
    }
    
    /**
     * Reset the silence detector state
     */
    fun reset() {
        silenceStartTime = null
        lastCheckTime = System.currentTimeMillis()
        Log.d(TAG, "Silence detector reset")
    }
    
    /**
     * Get current silence duration in milliseconds
     */
    fun getSilenceDuration(): Long {
        val startTime = silenceStartTime ?: return 0
        return System.currentTimeMillis() - startTime
    }
    
    /**
     * Check if currently in silence period
     */
    fun isSilent(): Boolean = silenceStartTime != null
}
