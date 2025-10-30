package com.androidvoiceapp.audio

import java.io.File
import java.io.RandomAccessFile
import java.nio.ByteBuffer
import java.nio.ByteOrder

/**
 * Writes PCM audio data to WAV file format
 */
class WavWriter(
    private val file: File,
    private val sampleRate: Int = 16000,
    private val channels: Int = 1,
    private val bitsPerSample: Int = 16
) {
    
    private var raf: RandomAccessFile? = null
    private var dataSize = 0
    
    fun open() {
        file.parentFile?.mkdirs()
        raf = RandomAccessFile(file, "rw")
        // Write empty header (will update later with actual sizes)
        writeWavHeader(0)
    }
    
    fun write(samples: ShortArray, length: Int) {
        raf?.let { raf ->
            val buffer = ByteBuffer.allocate(length * 2)
            buffer.order(ByteOrder.LITTLE_ENDIAN)
            for (i in 0 until length) {
                buffer.putShort(samples[i])
            }
            raf.write(buffer.array())
            dataSize += length * 2
        }
    }
    
    fun close() {
        raf?.let { raf ->
            // Update header with actual data size
            raf.seek(0)
            writeWavHeader(dataSize)
            raf.close()
        }
        raf = null
    }
    
    private fun writeWavHeader(dataSize: Int) {
        raf?.let { raf ->
            val header = ByteBuffer.allocate(44)
            header.order(ByteOrder.LITTLE_ENDIAN)
            
            // RIFF chunk
            header.put("RIFF".toByteArray())
            header.putInt(36 + dataSize) // File size - 8
            header.put("WAVE".toByteArray())
            
            // fmt chunk
            header.put("fmt ".toByteArray())
            header.putInt(16) // Chunk size
            header.putShort(1.toShort()) // Audio format (PCM)
            header.putShort(channels.toShort())
            header.putInt(sampleRate)
            header.putInt(sampleRate * channels * bitsPerSample / 8) // Byte rate
            header.putShort((channels * bitsPerSample / 8).toShort()) // Block align
            header.putShort(bitsPerSample.toShort())
            
            // data chunk
            header.put("data".toByteArray())
            header.putInt(dataSize)
            
            raf.write(header.array())
        }
    }
}
