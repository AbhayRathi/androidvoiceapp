package com.androidvoiceapp.di

import com.androidvoiceapp.data.repository.*
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {
    
    @Binds
    @Singleton
    abstract fun bindMeetingRepository(
        impl: MeetingRepositoryImpl
    ): MeetingRepository
    
    @Binds
    @Singleton
    abstract fun bindChunkRepository(
        impl: ChunkRepositoryImpl
    ): ChunkRepository
    
    @Binds
    @Singleton
    abstract fun bindTranscriptRepository(
        impl: TranscriptRepositoryImpl
    ): TranscriptRepository
    
    @Binds
    @Singleton
    abstract fun bindSummaryRepository(
        impl: SummaryRepositoryImpl
    ): SummaryRepository
    
    @Binds
    @Singleton
    abstract fun bindSessionStateRepository(
        impl: SessionStateRepositoryImpl
    ): SessionStateRepository

    @Binds
    @Singleton
    abstract fun bindSettingsRepository(
        impl: SettingsRepositoryImpl
    ): SettingsRepository
}
