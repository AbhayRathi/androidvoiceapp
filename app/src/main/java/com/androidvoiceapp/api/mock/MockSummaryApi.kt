package com.androidvoiceapp.api.mock

import android.util.Log
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Mock summary API that streams summary generation
 */
@Singleton
class MockSummaryApi @Inject constructor() {
    
    companion object {
        private const val TAG = "MockSummaryApi"
    }
    
    data class SummaryUpdate(
        val title: String = "",
        val summary: String = "",
        val actionItems: List<String> = emptyList(),
        val keyPoints: List<String> = emptyList(),
        val progress: Float = 0f,
        val isComplete: Boolean = false
    )
    
    /**
     * Generate summary with streaming updates
     * @param transcript The full transcript text
     * @return Flow of incremental summary updates
     */
    fun generateSummaryStream(transcript: String): Flow<SummaryUpdate> = flow {
        Log.d(TAG, "Starting summary generation (mock streaming)")
        
        // Simulate streaming by emitting progressive updates
        
        // Step 1: Title (10% progress)
        delay(1000)
        emit(SummaryUpdate(
            title = "Meeting Discussion Summary",
            progress = 0.1f
        ))
        
        // Step 2: Summary part 1 (30% progress)
        delay(1500)
        emit(SummaryUpdate(
            title = "Meeting Discussion Summary",
            summary = "This meeting covered important topics including project timeline, technical architecture, and implementation approach.",
            progress = 0.3f
        ))
        
        // Step 3: Summary complete (50% progress)
        delay(1500)
        emit(SummaryUpdate(
            title = "Meeting Discussion Summary",
            summary = "This meeting covered important topics including project timeline, technical architecture, and implementation approach. The team discussed key milestones, quality assurance processes, and next steps for the project.",
            progress = 0.5f
        ))
        
        // Step 4: Action items (70% progress)
        delay(1500)
        emit(SummaryUpdate(
            title = "Meeting Discussion Summary",
            summary = "This meeting covered important topics including project timeline, technical architecture, and implementation approach. The team discussed key milestones, quality assurance processes, and next steps for the project.",
            actionItems = listOf(
                "Schedule follow-up meeting to address remaining concerns",
                "Send action items to all participants",
                "Complete integration testing by end of quarter"
            ),
            progress = 0.7f
        ))
        
        // Step 5: Key points (90% progress)
        delay(1500)
        emit(SummaryUpdate(
            title = "Meeting Discussion Summary",
            summary = "This meeting covered important topics including project timeline, technical architecture, and implementation approach. The team discussed key milestones, quality assurance processes, and next steps for the project.",
            actionItems = listOf(
                "Schedule follow-up meeting to address remaining concerns",
                "Send action items to all participants",
                "Complete integration testing by end of quarter"
            ),
            keyPoints = listOf(
                "Technical architecture review is the first priority",
                "All team members need alignment on implementation",
                "Quality assurance will be the focus of the next phase",
                "Regular communication is essential for project success"
            ),
            progress = 0.9f
        ))
        
        // Step 6: Complete (100% progress)
        delay(1000)
        emit(SummaryUpdate(
            title = "Meeting Discussion Summary",
            summary = "This meeting covered important topics including project timeline, technical architecture, and implementation approach. The team discussed key milestones, quality assurance processes, and next steps for the project.",
            actionItems = listOf(
                "Schedule follow-up meeting to address remaining concerns",
                "Send action items to all participants",
                "Complete integration testing by end of quarter"
            ),
            keyPoints = listOf(
                "Technical architecture review is the first priority",
                "All team members need alignment on implementation",
                "Quality assurance will be the focus of the next phase",
                "Regular communication is essential for project success"
            ),
            progress = 1.0f,
            isComplete = true
        ))
        
        Log.d(TAG, "Summary generation complete")
    }
    
    /**
     * Check if summary API is available (always true for mock)
     */
    fun isAvailable(): Boolean = true
}
