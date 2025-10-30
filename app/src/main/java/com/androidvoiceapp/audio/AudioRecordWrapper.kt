package com.androidvoiceapp.audio

import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

/**
 * Wrapper for Android AudioRecord with WAV file writing.
 */
class AudioRecordWrapper(
    private val sampleRate: Int = 44100,
    private val channelConfig: Int = AudioFormat.CHANNEL_IN_MONO,
    private val audioFormat: Int = AudioFormat.ENCODING_PCM_16BIT
) {
    private var audioRecord: AudioRecord? = null
    private var isRecordingActive = false
    private val bufferSize = AudioRecord.getMinBufferSize(sampleRate, channelConfig, audioFormat)
    
    @Volatile
    var onAudioData: ((ByteArray, Int) -> Unit)? = null
    
    fun start(): Boolean {
        if (isRecordingActive) return false
        
        try {
            audioRecord = AudioRecord(
                MediaRecorder.AudioSource.MIC,
                sampleRate,
                channelConfig,
                audioFormat,
                bufferSize * 2
            )
            
            if (audioRecord?.state != AudioRecord.STATE_INITIALIZED) {
                audioRecord?.release()
                audioRecord = null
                return false
            }
            
            audioRecord?.startRecording()
            isRecordingActive = true
            return true
        } catch (e: Exception) {
            e.printStackTrace()
            return false
        }
    }
    
    suspend fun readAudioData(): Pair<ByteArray, Int>? = withContext(Dispatchers.IO) {
        if (!isRecordingActive || audioRecord == null) return@withContext null
        
        val buffer = ByteArray(bufferSize)
        val readSize = audioRecord?.read(buffer, 0, bufferSize) ?: -1
        
        if (readSize > 0) {
            onAudioData?.invoke(buffer, readSize)
            Pair(buffer, readSize)
        } else {
            null
        }
    }
    
    fun stop() {
        isRecordingActive = false
        audioRecord?.apply {
            try {
                stop()
                release()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
        audioRecord = null
    }
    
    fun isRecording(): Boolean = isRecordingActive
    
    fun getBufferSizeInBytes(): Int = bufferSize
}
