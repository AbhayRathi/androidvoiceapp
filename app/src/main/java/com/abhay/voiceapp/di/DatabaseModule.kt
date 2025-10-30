package com.abhay.voiceapp.di

import android.content.Context
import androidx.room.Room
import com.abhay.voiceapp.data.dao.*
import com.abhay.voiceapp.data.database.AppDatabase
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
            "voice_app_database"
        )
            .fallbackToDestructiveMigration()
            .build()
    }
    
    @Provides
    fun provideMeetingDao(database: AppDatabase): MeetingDao {
        return database.meetingDao()
    }
    
    @Provides
    fun provideChunkDao(database: AppDatabase): ChunkDao {
        return database.chunkDao()
    }
    
    @Provides
    fun provideTranscriptSegmentDao(database: AppDatabase): TranscriptSegmentDao {
        return database.transcriptSegmentDao()
    }
    
    @Provides
    fun provideSummaryDao(database: AppDatabase): SummaryDao {
        return database.summaryDao()
    }
    
    @Provides
    fun provideSessionStateDao(database: AppDatabase): SessionStateDao {
        return database.sessionStateDao()
    }
}
