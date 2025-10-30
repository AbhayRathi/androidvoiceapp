package com.androidvoiceapp.audio

import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.nio.ByteBuffer
import java.nio.ByteOrder

/**
 * Writes PCM audio data as WAV file format.
 * Format: 16-bit PCM, mono, 44.1kHz
 */
class WavWriter(
    private val outputFile: File,
    private val sampleRate: Int = 44100,
    private val channels: Int = 1,
    private val bitsPerSample: Int = 16
) {
    private var outputStream: FileOutputStream? = null
    private var dataSize = 0L
    
    fun start() {
        outputStream = FileOutputStream(outputFile)
        // Write WAV header with placeholder for data size
        writeWavHeader(0)
    }
    
    fun write(buffer: ByteArray, size: Int) {
        outputStream?.write(buffer, 0, size)
        dataSize += size
    }
    
    fun stop() {
        outputStream?.apply {
            // Update header with actual data size
            flush()
            close()
        }
        
        // Update WAV header with actual size
        updateWavHeader()
        outputStream = null
    }
    
    private fun writeWavHeader(dataSize: Long) {
        val header = ByteBuffer.allocate(44).apply {
            order(ByteOrder.LITTLE_ENDIAN)
            
            // RIFF header
            put("RIFF".toByteArray())
            putInt((36 + dataSize).toInt()) // File size - 8
            put("WAVE".toByteArray())
            
            // fmt subchunk
            put("fmt ".toByteArray())
            putInt(16) // Subchunk size
            putShort(1) // Audio format (PCM)
            putShort(channels.toShort())
            putInt(sampleRate)
            putInt(sampleRate * channels * bitsPerSample / 8) // Byte rate
            putShort((channels * bitsPerSample / 8).toShort()) // Block align
            putShort(bitsPerSample.toShort())
            
            // data subchunk
            put("data".toByteArray())
            putInt(dataSize.toInt())
        }
        
        outputStream?.write(header.array())
    }
    
    private fun updateWavHeader() {
        try {
            java.io.RandomAccessFile(outputFile, "rw").use { raf ->
                raf.seek(4)
                raf.write(intToBytes((36 + dataSize).toInt()))
                raf.seek(40)
                raf.write(intToBytes(dataSize.toInt()))
            }
        } catch (e: IOException) {
            e.printStackTrace()
        }
    }
    
    private fun intToBytes(value: Int): ByteArray {
        return ByteBuffer.allocate(4).apply {
            order(ByteOrder.LITTLE_ENDIAN)
            putInt(value)
        }.array()
    }
}
