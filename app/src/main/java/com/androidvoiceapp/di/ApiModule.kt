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
 * Hilt module for providing the REAL API implementations for the release build.
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
