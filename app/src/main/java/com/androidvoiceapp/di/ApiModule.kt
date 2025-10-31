package com.androidvoiceapp.di

import com.androidvoiceapp.api.GeminiSummaryApi
import com.androidvoiceapp.api.SummaryApi
import com.androidvoiceapp.api.TranscriptionApi
import com.androidvoiceapp.api.GeminiTranscriptionApi
import com.androidvoiceapp.api.mock.MockTranscriptionApi
import com.androidvoiceapp.data.repository.SettingsRepository
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object ApiModule {

    @Provides
    @Singleton
    fun provideTranscriptionApi(
        settingsRepository: SettingsRepository,
        geminiApi: GeminiTranscriptionApi,
        mockApi: MockTranscriptionApi
    ): TranscriptionApi {
        val provider = runBlocking { settingsRepository.getProvider().first() }
        return if (provider == "Gemini" && geminiApi.isAvailable()) {
            geminiApi
        } else {
            mockApi
        }
    }

    @Provides
    @Singleton
    fun provideSummaryApi(
        settingsRepository: SettingsRepository,
        geminiSummaryApi: GeminiSummaryApi
    ): SummaryApi {
        val provider = runBlocking { settingsRepository.getProvider().first() }
        return if (provider == "Gemini") {
            geminiSummaryApi
        } else {
            // You can create a mock summary API for testing
            geminiSummaryApi // Fallback to Gemini for now
        }
    }
}
