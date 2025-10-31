package com.androidvoiceapp.audio

import android.annotation.SuppressLint
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import android.util.Log

/**
 * Wrapper around Android AudioRecord for audio capture
 */
class AudioRecordWrapper(
    private val sampleRate: Int = 16000,
    private val channelConfig: Int = AudioFormat.CHANNEL_IN_MONO,
    private val audioFormat: Int = AudioFormat.ENCODING_PCM_16BIT
) {
    
    companion object {
        private const val TAG = "AudioRecordWrapper"
    }
    
    private var audioRecord: AudioRecord? = null
    private val bufferSize: Int = AudioRecord.getMinBufferSize(sampleRate, channelConfig, audioFormat)
    
    @SuppressLint("MissingPermission")
    fun start(): Boolean {
        return try {
            if (bufferSize == AudioRecord.ERROR || bufferSize == AudioRecord.ERROR_BAD_VALUE) {
                Log.e(TAG, "Invalid buffer size: $bufferSize")
                return false
            }
            
            audioRecord = AudioRecord(
                MediaRecorder.AudioSource.MIC,
                sampleRate,
                channelConfig,
                audioFormat,
                bufferSize * 2
            )
            
            if (audioRecord?.state != AudioRecord.STATE_INITIALIZED) {
                Log.e(TAG, "AudioRecord not initialized")
                return false
            }
            
            audioRecord?.startRecording()
            true
        } catch (e: Exception) {
            Log.e(TAG, "Failed to start recording", e)
            false
        }
    }
    
    fun read(buffer: ShortArray): Int {
        return try {
            audioRecord?.read(buffer, 0, buffer.size) ?: 0
        } catch (e: Exception) {
            Log.e(TAG, "Failed to read audio", e)
            0
        }
    }
    
    fun stop() {
        try {
            audioRecord?.stop()
        } catch (e: Exception) {
            Log.e(TAG, "Failed to stop recording", e)
        }
    }
    
    fun release() {
        try {
            audioRecord?.release()
            audioRecord = null
        } catch (e: Exception) {
            Log.e(TAG, "Failed to release audio record", e)
        }
    }
    
    fun getBufferSize(): Int = bufferSize
    
    fun isRecording(): Boolean {
        return audioRecord?.recordingState == AudioRecord.RECORDSTATE_RECORDING
    }
}
