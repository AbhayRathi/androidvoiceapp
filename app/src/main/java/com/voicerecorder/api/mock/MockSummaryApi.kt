package com.voicerecorder.api.mock

import com.voicerecorder.api.*
import kotlinx.coroutines.delay
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class MockSummaryApi @Inject constructor() : SummaryApi {

    override suspend fun generateSummary(
        transcript: String,
        onProgress: (SummaryProgress) -> Unit
    ): SummaryResult {
        // Simulate streaming response with progressive updates
        
        // Title (10%)
        delay(500)
        val title = "Team Meeting Summary"
        onProgress(SummaryProgress("title", title, 10))
        
        delay(500)
        
        // Summary - stream word by word (10-50%)
        val summaryText = "The team discussed the project roadmap, key deliverables, and identified potential risks. " +
                "Progress was reviewed on the current sprint and adjustments were made to the timeline based on team feedback."
        
        val summaryWords = summaryText.split(" ")
        val summaryBuilder = StringBuilder()
        summaryWords.forEachIndexed { index, word ->
            summaryBuilder.append(word).append(" ")
            val progress = 10 + ((index + 1) * 40 / summaryWords.size)
            onProgress(SummaryProgress("summary", summaryBuilder.toString().trim(), progress))
            delay(100)
        }
        
        delay(500)
        
        // Action Items (50-75%)
        val actionItems = listOf(
            "Review and finalize project timeline by end of week",
            "Schedule follow-up meeting with stakeholders",
            "Update documentation with new requirements"
        )
        
        actionItems.forEachIndexed { index, item ->
            val progress = 50 + ((index + 1) * 25 / actionItems.size)
            onProgress(SummaryProgress("actionItems", item, progress))
            delay(300)
        }
        
        delay(500)
        
        // Key Points (75-100%)
        val keyPoints = listOf(
            "Project is on track for Q2 delivery",
            "Team identified three critical blockers to address",
            "New feature implementations are progressing well"
        )
        
        keyPoints.forEachIndexed { index, item ->
            val progress = 75 + ((index + 1) * 25 / keyPoints.size)
            onProgress(SummaryProgress("keyPoints", item, progress))
            delay(300)
        }

        return SummaryResult(
            title = title,
            summary = summaryText,
            actionItems = actionItems,
            keyPoints = keyPoints
        )
    }
}
