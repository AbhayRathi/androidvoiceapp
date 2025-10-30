package com.voicerecorder.di

import android.content.Context
import androidx.room.Room
import androidx.work.WorkerFactory
import com.google.gson.Gson
import com.voicerecorder.api.SummaryApi
import com.voicerecorder.api.TranscriptionApi
import com.voicerecorder.api.mock.MockSummaryApi
import com.voicerecorder.api.mock.MockTranscriptionApi
import com.voicerecorder.data.repository.*
import com.voicerecorder.data.room.*
import com.voicerecorder.util.StorageChecker
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideAppDatabase(@ApplicationContext context: Context): AppDatabase {
        return Room.databaseBuilder(
            context,
            AppDatabase::class.java,
            "voice_recorder_db"
        )
            .fallbackToDestructiveMigration()
            .build()
    }

    @Provides
    @Singleton
    fun provideMeetingDao(database: AppDatabase): MeetingDao {
        return database.meetingDao()
    }

    @Provides
    @Singleton
    fun provideChunkDao(database: AppDatabase): ChunkDao {
        return database.chunkDao()
    }

    @Provides
    @Singleton
    fun provideTranscriptSegmentDao(database: AppDatabase): TranscriptSegmentDao {
        return database.transcriptSegmentDao()
    }

    @Provides
    @Singleton
    fun provideSummaryDao(database: AppDatabase): SummaryDao {
        return database.summaryDao()
    }

    @Provides
    @Singleton
    fun provideSessionStateDao(database: AppDatabase): SessionStateDao {
        return database.sessionStateDao()
    }
}

@Module
@InstallIn(SingletonComponent::class)
object RepositoryModule {

    @Provides
    @Singleton
    fun provideMeetingRepository(meetingDao: MeetingDao): MeetingRepository {
        return MeetingRepositoryImpl(meetingDao)
    }

    @Provides
    @Singleton
    fun provideChunkRepository(chunkDao: ChunkDao): ChunkRepository {
        return ChunkRepositoryImpl(chunkDao)
    }

    @Provides
    @Singleton
    fun provideTranscriptRepository(transcriptDao: TranscriptSegmentDao): TranscriptRepository {
        return TranscriptRepositoryImpl(transcriptDao)
    }

    @Provides
    @Singleton
    fun provideSummaryRepository(summaryDao: SummaryDao): SummaryRepository {
        return SummaryRepositoryImpl(summaryDao)
    }

    @Provides
    @Singleton
    fun provideSessionStateRepository(sessionStateDao: SessionStateDao): SessionStateRepository {
        return SessionStateRepositoryImpl(sessionStateDao)
    }
}

@Module
@InstallIn(SingletonComponent::class)
object ApiModule {

    @Provides
    @Singleton
    fun provideTranscriptionApi(): TranscriptionApi {
        // TODO: Add logic to switch between mock and real API based on settings
        return MockTranscriptionApi()
    }

    @Provides
    @Singleton
    fun provideSummaryApi(): SummaryApi {
        // TODO: Add logic to switch between mock and real API based on settings
        return MockSummaryApi()
    }

    @Provides
    @Singleton
    fun provideGson(): Gson {
        return Gson()
    }
}

@Module
@InstallIn(SingletonComponent::class)
object UtilModule {

    @Provides
    @Singleton
    fun provideStorageChecker(@ApplicationContext context: Context): StorageChecker {
        return StorageChecker(context)
    }
}
