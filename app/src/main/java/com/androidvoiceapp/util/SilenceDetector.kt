package com.androidvoiceapp.util

/**
 * Detects silence in audio samples
 */
class SilenceDetector(
    private val silenceThresholdAmplitude: Int = 500, // Amplitude threshold below which is considered silence
    private val silenceDurationMs: Long = 10000 // 10 seconds
) {
    private var silenceStartTime: Long? = null
    private var isSilent = false
    
    /**
     * Process audio samples and check for silence
     * @param samples PCM 16-bit audio samples
     * @return true if silence has been detected for the configured duration
     */
    fun processSamples(samples: ShortArray): Boolean {
        val maxAmplitude = samples.maxOfOrNull { kotlin.math.abs(it.toInt()) } ?: 0
        
        if (maxAmplitude < silenceThresholdAmplitude) {
            // Audio is silent
            val now = System.currentTimeMillis()
            if (silenceStartTime == null) {
                silenceStartTime = now
            } else {
                val silenceDuration = now - silenceStartTime!!
                if (silenceDuration >= silenceDurationMs && !isSilent) {
                    isSilent = true
                    return true
                }
            }
        } else {
            // Audio is not silent, reset
            reset()
        }
        
        return false
    }
    
    fun reset() {
        silenceStartTime = null
        isSilent = false
    }
    
    fun isSilenceDetected(): Boolean = isSilent
}
