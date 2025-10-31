package com.androidvoiceapp.di

import com.androidvoiceapp.api.SummaryApi
import com.androidvoiceapp.api.TranscriptionApi
import com.androidvoiceapp.api.mock.MockSummaryApi
import com.androidvoiceapp.api.mock.MockTranscriptionApi
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * Debug/Mock module that provides Mock API implementations by default
 * This ensures mock providers are always available for testing
 */
@Module
@InstallIn(SingletonComponent::class)
abstract class DebugApiModule {
    
    @Binds
    @Singleton
    @MockApi
    abstract fun bindMockTranscriptionApi(impl: MockTranscriptionApi): TranscriptionApi
    
    @Binds
    @Singleton
    @MockApi
    abstract fun bindMockSummaryApi(impl: MockSummaryApi): SummaryApi
}

