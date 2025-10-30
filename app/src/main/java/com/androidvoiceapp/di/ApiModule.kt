package com.androidvoiceapp.di

import android.content.Context
import com.androidvoiceapp.api.mock.MockSummaryApi
import com.androidvoiceapp.api.mock.MockTranscriptionApi
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object ApiModule {
    
    @Provides
    @Singleton
    fun provideMockTranscriptionApi(@ApplicationContext context: Context): MockTranscriptionApi {
        return MockTranscriptionApi()
    }
    
    @Provides
    @Singleton
    fun provideMockSummaryApi(): MockSummaryApi {
        return MockSummaryApi()
    }
}
