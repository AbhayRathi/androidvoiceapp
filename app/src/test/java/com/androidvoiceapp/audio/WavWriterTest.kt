package com.androidvoiceapp.audio

import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.Rule
import org.junit.rules.TemporaryFolder
import java.io.File
import java.io.RandomAccessFile
import java.nio.ByteBuffer
import java.nio.ByteOrder

class WavWriterTest {
    
    @get:Rule
    val tempFolder = TemporaryFolder()
    
    private lateinit var testFile: File
    private lateinit var wavWriter: WavWriter
    
    @Before
    fun setup() {
        testFile = tempFolder.newFile("test_audio.wav")
        wavWriter = WavWriter(
            file = testFile,
            sampleRate = 16000,
            channels = 1,
            bitsPerSample = 16
        )
    }
    
    @Test
    fun `test WAV header is written correctly with empty file`() {
        // Open and immediately close (empty file)
        wavWriter.open()
        wavWriter.close()
        
        // Verify file exists and has correct header size
        assertTrue("WAV file should exist", testFile.exists())
        assertEquals("Empty WAV file should be 44 bytes (header only)", 44L, testFile.length())
        
        // Verify header structure
        val raf = RandomAccessFile(testFile, "r")
        val header = ByteArray(44)
        raf.read(header)
        raf.close()
        
        val buffer = ByteBuffer.wrap(header)
        buffer.order(ByteOrder.LITTLE_ENDIAN)
        
        // Check RIFF header
        val riffHeader = ByteArray(4)
        buffer.get(riffHeader)
        assertEquals("RIFF", String(riffHeader))
        
        // Check file size (should be 36 for empty file)
        val fileSize = buffer.int
        assertEquals(36, fileSize)
        
        // Check WAVE header
        val waveHeader = ByteArray(4)
        buffer.get(waveHeader)
        assertEquals("WAVE", String(waveHeader))
        
        // Check fmt chunk
        val fmtHeader = ByteArray(4)
        buffer.get(fmtHeader)
        assertEquals("fmt ", String(fmtHeader))
        
        // Check fmt chunk size
        val fmtSize = buffer.int
        assertEquals(16, fmtSize)
        
        // Check audio format (PCM = 1)
        val audioFormat = buffer.short
        assertEquals(1, audioFormat.toInt())
        
        // Check channels
        val channels = buffer.short
        assertEquals(1, channels.toInt())
        
        // Check sample rate
        val sampleRate = buffer.int
        assertEquals(16000, sampleRate)
        
        // Check byte rate (sampleRate * channels * bitsPerSample / 8)
        val byteRate = buffer.int
        assertEquals(32000, byteRate)
        
        // Check block align (channels * bitsPerSample / 8)
        val blockAlign = buffer.short
        assertEquals(2, blockAlign.toInt())
        
        // Check bits per sample
        val bitsPerSample = buffer.short
        assertEquals(16, bitsPerSample.toInt())
        
        // Check data chunk
        val dataHeader = ByteArray(4)
        buffer.get(dataHeader)
        assertEquals("data", String(dataHeader))
        
        // Check data size (should be 0 for empty file)
        val dataSize = buffer.int
        assertEquals(0, dataSize)
    }
    
    @Test
    fun `test WAV header is updated with correct data size after writing samples`() {
        val samples = ShortArray(1000) { (it % 100).toShort() }
        val expectedDataSize = samples.size * 2 // 2 bytes per sample
        
        wavWriter.open()
        wavWriter.write(samples, samples.size)
        wavWriter.close()
        
        // Verify file size (header + data)
        val expectedFileSize = 44L + expectedDataSize
        assertEquals("File size should be header + data", expectedFileSize, testFile.length())
        
        // Verify data size in header
        val raf = RandomAccessFile(testFile, "r")
        
        // Seek to file size field (offset 4)
        raf.seek(4)
        val fileSizeBytes = ByteArray(4)
        raf.read(fileSizeBytes)
        val fileSize = ByteBuffer.wrap(fileSizeBytes).order(ByteOrder.LITTLE_ENDIAN).int
        assertEquals("File size field should be 36 + data size", 36 + expectedDataSize, fileSize)
        
        // Seek to data size field (offset 40)
        raf.seek(40)
        val dataSizeBytes = ByteArray(4)
        raf.read(dataSizeBytes)
        val dataSize = ByteBuffer.wrap(dataSizeBytes).order(ByteOrder.LITTLE_ENDIAN).int
        assertEquals("Data size field should match written data", expectedDataSize, dataSize)
        
        raf.close()
    }
    
    @Test
    fun `test multiple writes accumulate data size correctly`() {
        val samples1 = ShortArray(500) { 100 }
        val samples2 = ShortArray(300) { 200 }
        val samples3 = ShortArray(200) { 300 }
        
        val totalSamples = samples1.size + samples2.size + samples3.size
        val expectedDataSize = totalSamples * 2
        
        wavWriter.open()
        wavWriter.write(samples1, samples1.size)
        wavWriter.write(samples2, samples2.size)
        wavWriter.write(samples3, samples3.size)
        wavWriter.close()
        
        // Verify total file size
        val expectedFileSize = 44L + expectedDataSize
        assertEquals("File size should be header + all data", expectedFileSize, testFile.length())
        
        // Verify data size in header
        val raf = RandomAccessFile(testFile, "r")
        raf.seek(40)
        val dataSizeBytes = ByteArray(4)
        raf.read(dataSizeBytes)
        val dataSize = ByteBuffer.wrap(dataSizeBytes).order(ByteOrder.LITTLE_ENDIAN).int
        assertEquals("Data size should match all written samples", expectedDataSize, dataSize)
        raf.close()
    }
    
    @Test
    fun `test partial write length is respected`() {
        val samples = ShortArray(1000) { (it % 100).toShort() }
        val writeLength = 600
        val expectedDataSize = writeLength * 2
        
        wavWriter.open()
        wavWriter.write(samples, writeLength) // Only write first 600 samples
        wavWriter.close()
        
        // Verify file size
        val expectedFileSize = 44L + expectedDataSize
        assertEquals("File size should reflect partial write", expectedFileSize, testFile.length())
        
        // Verify data size in header
        val raf = RandomAccessFile(testFile, "r")
        raf.seek(40)
        val dataSizeBytes = ByteArray(4)
        raf.read(dataSizeBytes)
        val dataSize = ByteBuffer.wrap(dataSizeBytes).order(ByteOrder.LITTLE_ENDIAN).int
        assertEquals("Data size should reflect partial write", expectedDataSize, dataSize)
        raf.close()
    }
    
    @Test
    fun `test WAV file can be created in nested directory`() {
        val nestedDir = File(tempFolder.root, "audio/chunks/meeting_1")
        val nestedFile = File(nestedDir, "chunk_1.wav")
        
        val nestedWavWriter = WavWriter(
            file = nestedFile,
            sampleRate = 16000,
            channels = 1,
            bitsPerSample = 16
        )
        
        // Should create directories automatically
        nestedWavWriter.open()
        nestedWavWriter.close()
        
        assertTrue("Nested directory should be created", nestedDir.exists())
        assertTrue("WAV file should exist in nested directory", nestedFile.exists())
        assertEquals("File should have correct header", 44L, nestedFile.length())
    }
    
    @Test
    fun `test different sample rates produce correct byte rate`() {
        val sampleRates = listOf(8000, 16000, 44100, 48000)
        
        for (sampleRate in sampleRates) {
            val file = tempFolder.newFile("test_${sampleRate}.wav")
            val writer = WavWriter(
                file = file,
                sampleRate = sampleRate,
                channels = 1,
                bitsPerSample = 16
            )
            
            writer.open()
            writer.close()
            
            // Verify byte rate in header
            val raf = RandomAccessFile(file, "r")
            raf.seek(28) // Byte rate offset
            val byteRateBytes = ByteArray(4)
            raf.read(byteRateBytes)
            val byteRate = ByteBuffer.wrap(byteRateBytes).order(ByteOrder.LITTLE_ENDIAN).int
            raf.close()
            
            val expectedByteRate = sampleRate * 1 * 16 / 8
            assertEquals("Byte rate should be correct for $sampleRate Hz", expectedByteRate, byteRate)
        }
    }
    
    @Test
    fun `test data is written in little-endian format`() {
        val samples = shortArrayOf(0x0100, 0x0201, 0x0302) // Values that differ in byte order
        
        wavWriter.open()
        wavWriter.write(samples, samples.size)
        wavWriter.close()
        
        // Read the data portion
        val raf = RandomAccessFile(testFile, "r")
        raf.seek(44) // Skip header
        val dataBytes = ByteArray(samples.size * 2)
        raf.read(dataBytes)
        raf.close()
        
        val buffer = ByteBuffer.wrap(dataBytes)
        buffer.order(ByteOrder.LITTLE_ENDIAN)
        
        // Verify each sample
        for (i in samples.indices) {
            val readValue = buffer.short
            assertEquals("Sample $i should be in little-endian", samples[i], readValue)
        }
    }
}
