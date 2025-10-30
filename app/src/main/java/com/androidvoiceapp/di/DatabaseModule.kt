package com.androidvoiceapp.di

import android.content.Context
import androidx.room.Room
import com.androidvoiceapp.data.room.*
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
        ).build()
    }
    
    @Provides
    fun provideMeetingDao(database: AppDatabase): MeetingDao =
        database.meetingDao()
    
    @Provides
    fun provideChunkDao(database: AppDatabase): ChunkDao =
        database.chunkDao()
    
    @Provides
    fun provideTranscriptSegmentDao(database: AppDatabase): TranscriptSegmentDao =
        database.transcriptSegmentDao()
    
    @Provides
    fun provideSummaryDao(database: AppDatabase): SummaryDao =
        database.summaryDao()
    
    @Provides
    fun provideSessionStateDao(database: AppDatabase): SessionStateDao =
        database.sessionStateDao()
}
