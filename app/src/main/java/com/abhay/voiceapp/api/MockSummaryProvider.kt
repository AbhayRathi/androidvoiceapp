package com.abhay.voiceapp.api

import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class MockSummaryProvider @Inject constructor() : SummaryApi {
    
    override fun generateSummary(transcript: String): Flow<SummaryUpdate> = flow {
        // Simulate streaming response with delays
        
        // 1. Generate title
        delay(500)
        emit(SummaryUpdate(
            type = SummaryUpdateType.TITLE,
            content = "Project Discussion Meeting"
        ))
        
        // 2. Generate summary (streaming in chunks)
        delay(1000)
        val summaryPart1 = "The team discussed the current project status and upcoming deliverables. "
        emit(SummaryUpdate(
            type = SummaryUpdateType.SUMMARY_TEXT,
            content = summaryPart1
        ))
        
        delay(1500)
        val summaryPart2 = "Key progress was made on new features, and client feedback has been positive. "
        emit(SummaryUpdate(
            type = SummaryUpdateType.SUMMARY_TEXT,
            content = summaryPart2
        ))
        
        delay(1000)
        val summaryPart3 = "The team agreed to prioritize bug fixes before adding additional functionality."
        emit(SummaryUpdate(
            type = SummaryUpdateType.SUMMARY_TEXT,
            content = summaryPart3
        ))
        
        // 3. Generate action items
        delay(800)
        emit(SummaryUpdate(
            type = SummaryUpdateType.ACTION_ITEM,
            content = "Schedule follow-up meeting to review action items"
        ))
        
        delay(700)
        emit(SummaryUpdate(
            type = SummaryUpdateType.ACTION_ITEM,
            content = "Share updated documentation with the team"
        ))
        
        delay(600)
        emit(SummaryUpdate(
            type = SummaryUpdateType.ACTION_ITEM,
            content = "Send out meeting notes by end of day"
        ))
        
        // 4. Generate key points
        delay(800)
        emit(SummaryUpdate(
            type = SummaryUpdateType.KEY_POINT,
            content = "Significant progress on new features"
        ))
        
        delay(700)
        emit(SummaryUpdate(
            type = SummaryUpdateType.KEY_POINT,
            content = "Positive client feedback received"
        ))
        
        delay(600)
        emit(SummaryUpdate(
            type = SummaryUpdateType.KEY_POINT,
            content = "Bug fixes prioritized over new features"
        ))
        
        // 5. Complete
        delay(500)
        emit(SummaryUpdate(
            type = SummaryUpdateType.COMPLETE,
            content = ""
        ))
    }
}
