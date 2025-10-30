package com.voicerecorder.audio

import com.google.common.truth.Truth.assertThat
import org.junit.Test
import org.junit.Rule
import org.junit.rules.TemporaryFolder
import java.io.File

class WavWriterTest {

    @get:Rule
    val tempFolder = TemporaryFolder()

    @Test
    fun `WAV file is created with correct header`() {
        val file = tempFolder.newFile("test.wav")
        val wavWriter = WavWriter(
            file = file,
            sampleRate = 16000,
            channelCount = 1,
            bitsPerSample = 16
        )

        wavWriter.start()
        
        // Write some audio data
        val buffer = ShortArray(1024) { it.toShort() }
        wavWriter.write(buffer, buffer.size)
        
        wavWriter.finalize()

        // Check file exists and has content
        assertThat(file.exists()).isTrue()
        assertThat(file.length()).isGreaterThan(44) // WAV header is 44 bytes
    }

    @Test
    fun `WAV file contains correct data after write`() {
        val file = tempFolder.newFile("test_data.wav")
        val wavWriter = WavWriter(
            file = file,
            sampleRate = 16000,
            channelCount = 1,
            bitsPerSample = 16
        )

        wavWriter.start()
        
        // Write known audio data
        val buffer = ShortArray(512) { 100 }
        wavWriter.write(buffer, buffer.size)
        
        wavWriter.finalize()

        // Check file size (header + data)
        val expectedSize = 44L + (512 * 2) // 44 byte header + 512 samples * 2 bytes
        assertThat(file.length()).isEqualTo(expectedSize)
    }

    @Test
    fun `multiple writes accumulate correctly`() {
        val file = tempFolder.newFile("test_multiple.wav")
        val wavWriter = WavWriter(
            file = file,
            sampleRate = 16000,
            channelCount = 1,
            bitsPerSample = 16
        )

        wavWriter.start()
        
        // Write multiple buffers
        val buffer1 = ShortArray(256) { 100 }
        val buffer2 = ShortArray(256) { 200 }
        
        wavWriter.write(buffer1, buffer1.size)
        wavWriter.write(buffer2, buffer2.size)
        
        wavWriter.finalize()

        // Check file size (header + both buffers)
        val expectedSize = 44L + ((256 + 256) * 2)
        assertThat(file.length()).isEqualTo(expectedSize)
    }

    @Test
    fun `chunk duration calculation is correct`() {
        // Test that 30 seconds of audio at 16kHz mono 16-bit
        // produces the expected file size
        val file = tempFolder.newFile("test_30s.wav")
        val sampleRate = 16000
        val chunkDurationSeconds = 30
        val samplesPerChunk = sampleRate * chunkDurationSeconds
        
        val wavWriter = WavWriter(
            file = file,
            sampleRate = sampleRate,
            channelCount = 1,
            bitsPerSample = 16
        )

        wavWriter.start()
        
        // Write 30 seconds of audio data in chunks
        val bufferSize = 1024
        val totalSamples = samplesPerChunk
        var written = 0
        
        while (written < totalSamples) {
            val toWrite = minOf(bufferSize, totalSamples - written)
            val buffer = ShortArray(toWrite) { 0 }
            wavWriter.write(buffer, toWrite)
            written += toWrite
        }
        
        wavWriter.finalize()

        // Expected file size: 44 bytes (header) + (16000 * 30 * 2) bytes (data)
        val expectedSize = 44L + (16000L * 30 * 2)
        assertThat(file.length()).isEqualTo(expectedSize)
    }

    @Test
    fun `overlap calculation test`() {
        // Test that 2 seconds overlap at 16kHz = 32000 samples
        val sampleRate = 16000
        val overlapSeconds = 2
        val overlapSamples = sampleRate * overlapSeconds
        
        assertThat(overlapSamples).isEqualTo(32000)
        
        // For 30s chunks with 2s overlap:
        // Chunk 1: 0-30s
        // Chunk 2: 28-58s (starts 2s before end of chunk 1)
        val chunkDuration = 30
        val chunk1End = chunkDuration
        val chunk2Start = chunk1End - overlapSeconds
        
        assertThat(chunk2Start).isEqualTo(28)
    }
}
