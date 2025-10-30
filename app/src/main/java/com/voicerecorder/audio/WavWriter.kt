package com.voicerecorder.audio

import android.util.Log
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.io.RandomAccessFile
import java.nio.ByteBuffer
import java.nio.ByteOrder

class WavWriter(
    private val file: File,
    private val sampleRate: Int,
    private val channelCount: Int,
    private val bitsPerSample: Int
) {
    companion object {
        private const val TAG = "WavWriter"
    }

    private var outputStream: FileOutputStream? = null
    private var totalAudioLen: Long = 0
    private var isHeaderWritten = false

    fun start() {
        try {
            outputStream = FileOutputStream(file)
            // Write placeholder header (will be updated on finalize)
            writeWavHeader(0)
            isHeaderWritten = true
            totalAudioLen = 0
        } catch (e: IOException) {
            Log.e(TAG, "Failed to start WAV writer", e)
            throw e
        }
    }

    fun write(audioData: ShortArray, length: Int) {
        try {
            val byteBuffer = ByteBuffer.allocate(length * 2)
            byteBuffer.order(ByteOrder.LITTLE_ENDIAN)
            for (i in 0 until length) {
                byteBuffer.putShort(audioData[i])
            }
            outputStream?.write(byteBuffer.array())
            totalAudioLen += length * 2L
        } catch (e: IOException) {
            Log.e(TAG, "Failed to write audio data", e)
        }
    }

    fun finalize() {
        try {
            outputStream?.close()
            outputStream = null
            
            // Update WAV header with correct sizes
            updateWavHeader()
        } catch (e: IOException) {
            Log.e(TAG, "Failed to finalize WAV file", e)
        }
    }

    private fun writeWavHeader(audioLength: Long) {
        val header = ByteArray(44)
        val totalDataLen = audioLength + 36
        val byteRate = (sampleRate * channelCount * bitsPerSample / 8).toLong()

        // RIFF header
        header[0] = 'R'.code.toByte()
        header[1] = 'I'.code.toByte()
        header[2] = 'F'.code.toByte()
        header[3] = 'F'.code.toByte()
        
        // File size
        header[4] = (totalDataLen and 0xff).toByte()
        header[5] = ((totalDataLen shr 8) and 0xff).toByte()
        header[6] = ((totalDataLen shr 16) and 0xff).toByte()
        header[7] = ((totalDataLen shr 24) and 0xff).toByte()
        
        // WAVE header
        header[8] = 'W'.code.toByte()
        header[9] = 'A'.code.toByte()
        header[10] = 'V'.code.toByte()
        header[11] = 'E'.code.toByte()
        
        // fmt subchunk
        header[12] = 'f'.code.toByte()
        header[13] = 'm'.code.toByte()
        header[14] = 't'.code.toByte()
        header[15] = ' '.code.toByte()
        header[16] = 16 // Subchunk size
        header[17] = 0
        header[18] = 0
        header[19] = 0
        header[20] = 1 // Audio format (PCM)
        header[21] = 0
        header[22] = channelCount.toByte()
        header[23] = 0
        
        // Sample rate
        header[24] = (sampleRate and 0xff).toByte()
        header[25] = ((sampleRate shr 8) and 0xff).toByte()
        header[26] = ((sampleRate shr 16) and 0xff).toByte()
        header[27] = ((sampleRate shr 24) and 0xff).toByte()
        
        // Byte rate
        header[28] = (byteRate and 0xff).toByte()
        header[29] = ((byteRate shr 8) and 0xff).toByte()
        header[30] = ((byteRate shr 16) and 0xff).toByte()
        header[31] = ((byteRate shr 24) and 0xff).toByte()
        
        // Block align
        val blockAlign = (channelCount * bitsPerSample / 8).toShort()
        header[32] = (blockAlign.toInt() and 0xff).toByte()
        header[33] = ((blockAlign.toInt() shr 8) and 0xff).toByte()
        
        // Bits per sample
        header[34] = bitsPerSample.toByte()
        header[35] = 0
        
        // data subchunk
        header[36] = 'd'.code.toByte()
        header[37] = 'a'.code.toByte()
        header[38] = 't'.code.toByte()
        header[39] = 'a'.code.toByte()
        header[40] = (audioLength and 0xff).toByte()
        header[41] = ((audioLength shr 8) and 0xff).toByte()
        header[42] = ((audioLength shr 16) and 0xff).toByte()
        header[43] = ((audioLength shr 24) and 0xff).toByte()

        outputStream?.write(header)
    }

    private fun updateWavHeader() {
        try {
            RandomAccessFile(file, "rw").use { raf ->
                // Update file size
                raf.seek(4)
                val totalDataLen = totalAudioLen + 36
                raf.write((totalDataLen and 0xff).toInt())
                raf.write(((totalDataLen shr 8) and 0xff).toInt())
                raf.write(((totalDataLen shr 16) and 0xff).toInt())
                raf.write(((totalDataLen shr 24) and 0xff).toInt())
                
                // Update data size
                raf.seek(40)
                raf.write((totalAudioLen and 0xff).toInt())
                raf.write(((totalAudioLen shr 8) and 0xff).toInt())
                raf.write(((totalAudioLen shr 16) and 0xff).toInt())
                raf.write(((totalAudioLen shr 24) and 0xff).toInt())
            }
        } catch (e: IOException) {
            Log.e(TAG, "Failed to update WAV header", e)
        }
    }
}
