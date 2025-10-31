package com.androidvoiceapp.data.repository

import kotlinx.coroutines.flow.Flow

interface SettingsRepository {
    fun getProvider(): Flow<String>
    suspend fun setProvider(provider: String)
}
