package com.abhay.voiceapp.audio

import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.io.RandomAccessFile
import java.nio.ByteBuffer
import java.nio.ByteOrder
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AudioRecorder @Inject constructor() {
    
    companion object {
        private const val TAG = "AudioRecorder"
        const val SAMPLE_RATE = 44100
        const val CHANNEL_CONFIG = AudioFormat.CHANNEL_IN_MONO
        const val AUDIO_FORMAT = AudioFormat.ENCODING_PCM_16BIT
        const val CHANNELS = 1
        const val BITS_PER_SAMPLE = 16
    }
    
    private var audioRecord: AudioRecord? = null
    private val bufferSize = AudioRecord.getMinBufferSize(SAMPLE_RATE, CHANNEL_CONFIG, AUDIO_FORMAT)
    
    @Volatile
    private var isRecording = false
    
    fun startRecording(): Boolean {
        try {
            if (isRecording) {
                Log.w(TAG, "Already recording")
                return false
            }
            
            audioRecord = AudioRecord(
                MediaRecorder.AudioSource.MIC,
                SAMPLE_RATE,
                CHANNEL_CONFIG,
                AUDIO_FORMAT,
                bufferSize
            )
            
            if (audioRecord?.state != AudioRecord.STATE_INITIALIZED) {
                Log.e(TAG, "AudioRecord not initialized")
                return false
            }
            
            audioRecord?.startRecording()
            isRecording = true
            Log.d(TAG, "Recording started")
            return true
        } catch (e: Exception) {
            Log.e(TAG, "Failed to start recording", e)
            return false
        }
    }
    
    fun stopRecording() {
        try {
            isRecording = false
            audioRecord?.stop()
            audioRecord?.release()
            audioRecord = null
            Log.d(TAG, "Recording stopped")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to stop recording", e)
        }
    }
    
    suspend fun readAudioData(buffer: ByteArray): Int = withContext(Dispatchers.IO) {
        if (!isRecording || audioRecord == null) {
            return@withContext -1
        }
        
        return@withContext try {
            val read = audioRecord?.read(buffer, 0, buffer.size) ?: -1
            if (read < 0) {
                Log.w(TAG, "Error reading audio data: $read")
            }
            read
        } catch (e: Exception) {
            Log.e(TAG, "Exception reading audio data", e)
            -1
        }
    }
    
    fun isRecordingActive(): Boolean = isRecording
    
    fun getBufferSize(): Int = bufferSize
}
