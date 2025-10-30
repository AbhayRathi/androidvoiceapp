package com.voicerecorder.util

import android.content.Context
import android.os.Environment
import android.os.StatFs
import android.util.Log
import java.io.File

class StorageChecker(private val context: Context) {
    companion object {
        private const val TAG = "StorageChecker"
        private const val MIN_STORAGE_MB = 100 // Minimum 100MB free
        private const val MB = 1024 * 1024L
    }

    fun hasEnoughStorage(): Boolean {
        val availableMb = getAvailableStorageMb()
        Log.d(TAG, "Available storage: $availableMb MB")
        return availableMb >= MIN_STORAGE_MB
    }

    fun getAvailableStorageMb(): Long {
        return try {
            val storageDir = getStorageDirectory()
            val stat = StatFs(storageDir.path)
            val availableBytes = stat.availableBlocksLong * stat.blockSizeLong
            availableBytes / MB
        } catch (e: Exception) {
            Log.e(TAG, "Failed to get available storage", e)
            0
        }
    }

    private fun getStorageDirectory(): File {
        return context.getExternalFilesDir(null) ?: context.filesDir
    }
}
