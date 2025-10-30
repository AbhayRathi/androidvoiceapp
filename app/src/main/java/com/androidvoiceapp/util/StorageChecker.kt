package com.androidvoiceapp.util

import android.content.Context
import android.os.Environment
import android.os.StatFs
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class StorageChecker @Inject constructor(
    @ApplicationContext private val context: Context
) {
    
    companion object {
        private const val MIN_STORAGE_BYTES = 100 * 1024 * 1024L // 100 MB minimum
    }
    
    /**
     * Check if there is sufficient storage available
     */
    fun hasEnoughStorage(): Boolean {
        return try {
            val availableBytes = getAvailableStorageBytes()
            availableBytes >= MIN_STORAGE_BYTES
        } catch (e: Exception) {
            false
        }
    }
    
    /**
     * Get available storage in bytes
     */
    fun getAvailableStorageBytes(): Long {
        return try {
            val externalDir = context.getExternalFilesDir(null)
            val path = externalDir?.absolutePath ?: return 0L
            val stat = StatFs(path)
            stat.availableBlocksLong * stat.blockSizeLong
        } catch (e: Exception) {
            0L
        }
    }
    
    /**
     * Get human-readable storage string
     */
    fun getAvailableStorageString(): String {
        val bytes = getAvailableStorageBytes()
        return when {
            bytes >= 1024 * 1024 * 1024 -> "%.2f GB".format(bytes / (1024.0 * 1024.0 * 1024.0))
            bytes >= 1024 * 1024 -> "%.2f MB".format(bytes / (1024.0 * 1024.0))
            bytes >= 1024 -> "%.2f KB".format(bytes / 1024.0)
            else -> "$bytes B"
        }
    }
}
