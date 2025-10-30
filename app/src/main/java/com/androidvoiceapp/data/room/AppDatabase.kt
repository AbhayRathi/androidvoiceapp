package com.androidvoiceapp.data.room

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters

@Database(
    entities = [
        Meeting::class,
        Chunk::class,
        TranscriptSegment::class,
        Summary::class,
        SessionState::class
    ],
    version = 1,
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun meetingDao(): MeetingDao
    abstract fun chunkDao(): ChunkDao
    abstract fun transcriptSegmentDao(): TranscriptSegmentDao
    abstract fun summaryDao(): SummaryDao
    abstract fun sessionStateDao(): SessionStateDao
}
