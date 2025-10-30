package com.abhay.voiceapp.di

import com.abhay.voiceapp.api.MockSummaryProvider
import com.abhay.voiceapp.api.MockTranscriptionProvider
import com.abhay.voiceapp.api.SummaryApi
import com.abhay.voiceapp.api.TranscriptionApi
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class ApiModule {
    
    @Binds
    @Singleton
    abstract fun bindTranscriptionApi(
        mockTranscriptionProvider: MockTranscriptionProvider
    ): TranscriptionApi
    
    @Binds
    @Singleton
    abstract fun bindSummaryApi(
        mockSummaryProvider: MockSummaryProvider
    ): SummaryApi
}
