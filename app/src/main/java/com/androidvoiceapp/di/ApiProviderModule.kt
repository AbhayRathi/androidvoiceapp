package com.androidvoiceapp.di

import com.androidvoiceapp.api.SummaryApi
import com.androidvoiceapp.api.TranscriptionApi
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * Module that provides the actual API implementations to use
 * Currently defaults to Mock implementations
 * Can be extended to conditionally provide Production APIs based on settings
 */
@Module
@InstallIn(SingletonComponent::class)
object ApiProviderModule {
    
    /**
     * Provides the TranscriptionApi to be used throughout the app
     * Currently returns Mock implementation
     * 
     * To use real APIs, update this to:
     * 1. Check encrypted preferences for API key and provider selection
     * 2. Return real implementation if key exists, otherwise Mock
     * 
     * Example when real API is ready:
     * @Provides
     * @Singleton
     * fun provideTranscriptionApi(
     *     @MockApi mockApi: TranscriptionApi,
     *     @ProductionApi realApi: TranscriptionApi?
     * ): TranscriptionApi = realApi ?: mockApi
     */
    @Provides
    @Singleton
    fun provideTranscriptionApi(
        @MockApi mockApi: TranscriptionApi
    ): TranscriptionApi {
        // For now, always return Mock
        // TODO: Add logic to switch between Mock and Production based on settings
        return mockApi
    }
    
    /**
     * Provides the SummaryApi to be used throughout the app
     * Currently returns Mock implementation
     * 
     * To use real APIs, update this to:
     * 1. Check encrypted preferences for API key and provider selection
     * 2. Return real implementation if key exists, otherwise Mock
     * 
     * Example when real API is ready:
     * @Provides
     * @Singleton
     * fun provideSummaryApi(
     *     @MockApi mockApi: SummaryApi,
     *     @ProductionApi realApi: SummaryApi?
     * ): SummaryApi = realApi ?: mockApi
     */
    @Provides
    @Singleton
    fun provideSummaryApi(
        @MockApi mockApi: SummaryApi
    ): SummaryApi {
        // For now, always return Mock
        // TODO: Add logic to switch between Mock and Production based on settings
        return mockApi
    }
}
