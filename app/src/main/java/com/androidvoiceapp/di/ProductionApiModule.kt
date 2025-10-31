package com.androidvoiceapp.di

import com.androidvoiceapp.api.SummaryApi
import com.androidvoiceapp.api.TranscriptionApi
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * Production module stub for real API providers
 * This will be populated when real API implementations are added
 * 
 * To integrate real APIs:
 * 1. Create real API implementation classes (e.g., OpenAITranscriptionApi)
 * 2. Implement the TranscriptionApi and SummaryApi interfaces
 * 3. Add conditional logic here to provide real APIs based on settings
 * 
 * Example:
 * @Provides
 * @Singleton
 * @ProductionApi
 * fun provideRealTranscriptionApi(
 *     encryptedPrefs: EncryptedSharedPreferences
 * ): TranscriptionApi? {
 *     val apiKey = encryptedPrefs.getString("api_key", "") ?: ""
 *     return if (apiKey.isNotEmpty()) {
 *         when (encryptedPrefs.getString("provider", "Mock")) {
 *             "OpenAI" -> OpenAITranscriptionApi(apiKey)
 *             "Gemini" -> GeminiTranscriptionApi(apiKey)
 *             else -> null
 *         }
 *     } else null
 * }
 */
@Module
@InstallIn(SingletonComponent::class)
object ProductionApiModule {
    // Stub - to be populated with real API providers
    // See class documentation above for integration guide
}
