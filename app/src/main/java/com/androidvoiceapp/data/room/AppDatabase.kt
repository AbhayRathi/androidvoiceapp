package com.androidvoiceapp.data.room

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(
    entities = [
        MeetingEntity::class,
        ChunkEntity::class,
        TranscriptSegmentEntity::class,
        SummaryEntity::class,
        SessionStateEntity::class
    ],
    version = 1,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun meetingDao(): MeetingDao
    abstract fun chunkDao(): ChunkDao
    abstract fun transcriptSegmentDao(): TranscriptSegmentDao
    abstract fun summaryDao(): SummaryDao
    abstract fun sessionStateDao(): SessionStateDao
}
