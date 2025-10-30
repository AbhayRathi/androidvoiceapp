package com.abhay.voiceapp.util

import android.content.Context
import android.os.Environment
import android.os.StatFs
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class StorageManager @Inject constructor(
    @ApplicationContext private val context: Context
) {
    companion object {
        private const val MIN_STORAGE_BYTES = 100 * 1024 * 1024L // 100 MB
    }
    
    fun hasEnoughStorage(): Boolean {
        return getAvailableStorageBytes() > MIN_STORAGE_BYTES
    }
    
    fun getAvailableStorageBytes(): Long {
        val stat = StatFs(context.filesDir.path)
        return stat.availableBlocksLong * stat.blockSizeLong
    }
    
    fun getAvailableStorageMB(): Long {
        return getAvailableStorageBytes() / (1024 * 1024)
    }
}
