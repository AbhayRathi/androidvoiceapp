package com.voicerecorder.util

import kotlin.math.abs
import kotlin.math.sqrt

class SilenceDetector(
    private val silenceThresholdDb: Double = -40.0, // dB
    private val silenceDurationMs: Long = 10000 // 10 seconds
) {
    private var silenceStartTime: Long = 0
    private var isSilent = false

    fun detectSilence(audioData: ShortArray, sampleRate: Int): Boolean {
        val rms = calculateRMS(audioData)
        val db = amplitudeToDb(rms)

        if (db < silenceThresholdDb) {
            // Audio is silent
            if (silenceStartTime == 0L) {
                silenceStartTime = System.currentTimeMillis()
            }
            
            val silenceDuration = System.currentTimeMillis() - silenceStartTime
            if (!isSilent && silenceDuration >= silenceDurationMs) {
                isSilent = true
                return true // Silence threshold exceeded
            }
        } else {
            // Audio is not silent, reset
            silenceStartTime = 0
            isSilent = false
        }

        return false
    }

    fun reset() {
        silenceStartTime = 0
        isSilent = false
    }

    private fun calculateRMS(audioData: ShortArray): Double {
        var sum = 0.0
        for (sample in audioData) {
            sum += (sample * sample).toDouble()
        }
        return sqrt(sum / audioData.size)
    }

    private fun amplitudeToDb(amplitude: Double): Double {
        if (amplitude <= 0) return -100.0
        return 20 * kotlin.math.log10(amplitude / Short.MAX_VALUE)
    }

    fun getSilenceDuration(): Long {
        return if (silenceStartTime > 0) {
            System.currentTimeMillis() - silenceStartTime
        } else {
            0
        }
    }
}
