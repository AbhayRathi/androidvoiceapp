package com.abhay.voiceapp.audio

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
class WavFileWriter @Inject constructor() {
    
    companion object {
        private const val TAG = "WavFileWriter"
        private const val WAV_HEADER_SIZE = 44
    }
    
    suspend fun writeWavFile(
        outputFile: File,
        audioData: ByteArray,
        sampleRate: Int = AudioRecorder.SAMPLE_RATE,
        channels: Int = AudioRecorder.CHANNELS,
        bitsPerSample: Int = AudioRecorder.BITS_PER_SAMPLE
    ): Boolean = withContext(Dispatchers.IO) {
        try {
            FileOutputStream(outputFile).use { fos ->
                // Write WAV header
                val header = createWavHeader(
                    audioData.size,
                    sampleRate,
                    channels,
                    bitsPerSample
                )
                fos.write(header)
                fos.write(audioData)
            }
            Log.d(TAG, "WAV file written: ${outputFile.absolutePath}, size: ${outputFile.length()}")
            true
        } catch (e: Exception) {
            Log.e(TAG, "Failed to write WAV file", e)
            false
        }
    }
    
    suspend fun finalizeWavFile(
        tempFile: File,
        finalFile: File
    ): Boolean = withContext(Dispatchers.IO) {
        try {
            if (!tempFile.exists()) {
                Log.e(TAG, "Temp file does not exist: ${tempFile.absolutePath}")
                return@withContext false
            }
            
            // Read raw PCM data
            val rawData = tempFile.readBytes()
            
            // Write as proper WAV file
            writeWavFile(finalFile, rawData)
            
            // Delete temp file
            tempFile.delete()
            
            true
        } catch (e: Exception) {
            Log.e(TAG, "Failed to finalize WAV file", e)
            false
        }
    }
    
    private fun createWavHeader(
        dataSize: Int,
        sampleRate: Int,
        channels: Int,
        bitsPerSample: Int
    ): ByteArray {
        val byteRate = sampleRate * channels * bitsPerSample / 8
        val blockAlign = channels * bitsPerSample / 8
        val totalSize = dataSize + 36
        
        return ByteBuffer.allocate(WAV_HEADER_SIZE).apply {
            order(ByteOrder.LITTLE_ENDIAN)
            
            // RIFF chunk descriptor
            put("RIFF".toByteArray())
            putInt(totalSize)
            put("WAVE".toByteArray())
            
            // fmt sub-chunk
            put("fmt ".toByteArray())
            putInt(16) // Subchunk1Size (16 for PCM)
            putShort(1) // AudioFormat (1 for PCM)
            putShort(channels.toShort())
            putInt(sampleRate)
            putInt(byteRate)
            putShort(blockAlign.toShort())
            putShort(bitsPerSample.toShort())
            
            // data sub-chunk
            put("data".toByteArray())
            putInt(dataSize)
        }.array()
    }
    
    suspend fun appendToRawFile(file: File, data: ByteArray): Boolean = withContext(Dispatchers.IO) {
        try {
            FileOutputStream(file, true).use { fos ->
                fos.write(data)
            }
            true
        } catch (e: Exception) {
            Log.e(TAG, "Failed to append to raw file", e)
            false
        }
    }
}
