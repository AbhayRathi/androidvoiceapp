package com.abhay.voiceapp.data.database

import androidx.room.TypeConverter
import com.abhay.voiceapp.data.entity.*

class Converters {
    
    @TypeConverter
    fun fromMeetingStatus(value: MeetingStatus): String {
        return value.name
    }
    
    @TypeConverter
    fun toMeetingStatus(value: String): MeetingStatus {
        return MeetingStatus.valueOf(value)
    }
    
    @TypeConverter
    fun fromChunkStatus(value: ChunkStatus): String {
        return value.name
    }
    
    @TypeConverter
    fun toChunkStatus(value: String): ChunkStatus {
        return ChunkStatus.valueOf(value)
    }
    
    @TypeConverter
    fun fromSummaryStatus(value: SummaryStatus): String {
        return value.name
    }
    
    @TypeConverter
    fun toSummaryStatus(value: String): SummaryStatus {
        return SummaryStatus.valueOf(value)
    }
}
