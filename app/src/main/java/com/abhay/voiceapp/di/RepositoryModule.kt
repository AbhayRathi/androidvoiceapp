package com.abhay.voiceapp.di

import com.abhay.voiceapp.data.repository.*
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
        meetingRepositoryImpl: MeetingRepositoryImpl
    ): MeetingRepository
    
    @Binds
    @Singleton
    abstract fun bindChunkRepository(
        chunkRepositoryImpl: ChunkRepositoryImpl
    ): ChunkRepository
    
    @Binds
    @Singleton
    abstract fun bindTranscriptRepository(
        transcriptRepositoryImpl: TranscriptRepositoryImpl
    ): TranscriptRepository
    
    @Binds
    @Singleton
    abstract fun bindSummaryRepository(
        summaryRepositoryImpl: SummaryRepositoryImpl
    ): SummaryRepository
    
    @Binds
    @Singleton
    abstract fun bindSessionStateRepository(
        sessionStateRepositoryImpl: SessionStateRepositoryImpl
    ): SessionStateRepository
}
