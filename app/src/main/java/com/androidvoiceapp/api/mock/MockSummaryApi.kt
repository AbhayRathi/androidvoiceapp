package com.androidvoiceapp.api.mock

import android.util.Log
import com.androidvoiceapp.api.SummaryApi
import com.androidvoiceapp.api.SummaryUpdate
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class MockSummaryApi @Inject constructor() : SummaryApi {

    companion object {
        private const val TAG = "MockSummaryApi"
    }

    override fun generateSummary(transcript: String): Flow<SummaryUpdate> = flow {
        Log.d(TAG, "Starting summary generation (mock streaming)")

        val fullSummary = """
            Title: Meeting Discussion Summary
            Summary: This meeting covered important topics including project timeline, technical architecture, and implementation approach. The team discussed key milestones, quality assurance processes, and next steps for the project.
            Action Items:
            * Schedule follow-up meeting to address remaining concerns
            * Send action items to all participants
            * Complete integration testing by end of quarter
            Key Points:
            * Technical architecture review is the first priority
            * All team members need alignment on implementation
            * Quality assurance will be the focus of the next phase
        """.trimIndent()

        // Simulate streaming by emitting chunks of the summary
        fullSummary.chunked(30).forEach { chunk ->
            delay(200)
            emit(SummaryUpdate(text = chunk, isDone = false))
        }

        // Signal completion
        emit(SummaryUpdate(text = "", isDone = true))

        Log.d(TAG, "Summary generation complete")
    }
}
