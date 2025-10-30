package com.androidvoiceapp.util

import android.content.Context
import android.os.StatFs
import java.io.File

/**
 * Checks available storage space.
 */
class StorageChecker(private val context: Context) {
    
    companion object {
        private const val MIN_STORAGE_BYTES = 100 * 1024 * 1024L // 100 MB minimum
        private const val CHUNK_SIZE_ESTIMATE = 5 * 1024 * 1024L // ~5MB per chunk estimate
    }
    
    /**
     * Check if there's enough storage to start recording.
     */
    fun hasEnoughStorage(): Boolean {
        val availableBytes = getAvailableStorageBytes()
        return availableBytes > MIN_STORAGE_BYTES
    }
    
    /**
     * Check if there's enough storage for at least one more chunk.
     */
    fun canRecordMoreChunks(): Boolean {
        val availableBytes = getAvailableStorageBytes()
        return availableBytes > (MIN_STORAGE_BYTES + CHUNK_SIZE_ESTIMATE)
    }
    
    /**
     * Get available storage in bytes.
     */
    fun getAvailableStorageBytes(): Long {
        return try {
            val storageDir = getStorageDirectory()
            val stat = StatFs(storageDir.path)
            stat.availableBlocksLong * stat.blockSizeLong
        } catch (e: Exception) {
            e.printStackTrace()
            0L
        }
    }
    
    /**
     * Get available storage in MB.
     */
    fun getAvailableStorageMB(): Long {
        return getAvailableStorageBytes() / (1024 * 1024)
    }
    
    /**
     * Get the directory where audio files are stored.
     */
    fun getStorageDirectory(): File {
        // Use app-specific external files directory (doesn't require WRITE_EXTERNAL_STORAGE on API 19+)
        val audioDir = File(context.getExternalFilesDir(null), "audio")
        if (!audioDir.exists()) {
            audioDir.mkdirs()
        }
        return audioDir
    }
    
    /**
     * Get temp directory for recording chunks.
     */
    fun getTempDirectory(): File {
        val tempDir = File(getStorageDirectory(), "temp")
        if (!tempDir.exists()) {
            tempDir.mkdirs()
        }
        return tempDir
    }
}
