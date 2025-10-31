package com.androidvoiceapp.di

import com.androidvoiceapp.api.GeminiSummaryApi
import com.androidvoiceapp.api.GeminiTranscriptionApi
import com.androidvoiceapp.api.SummaryApi
import com.androidvoiceapp.api.TranscriptionApi
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * Hilt module for providing the REAL API implementations for the DEBUG build.
 * This forces the app to use the real Gemini APIs during development and testing.
 */
@Module
@InstallIn(SingletonComponent::class)
abstract class ApiModule {

    @Binds
    @Singleton
    abstract fun bindTranscriptionApi(impl: GeminiTranscriptionApi): TranscriptionApi

    @Binds
    @Singleton
    abstract fun bindSummaryApi(impl: GeminiSummaryApi): SummaryApi
}
