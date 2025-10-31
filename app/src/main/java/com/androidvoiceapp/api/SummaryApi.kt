package com.androidvoiceapp.api

import kotlinx.coroutines.flow.Flow

/**
 * Represents a partial summary update during streaming.
 */
data class SummaryUpdate(val text: String, val isDone: Boolean)

/**
 * Interface for summary generation APIs.
 */
interface SummaryApi {
    /**
     * Generate a structured summary from a full transcript.
     *
     * @param transcript The complete transcript of the meeting.
     * @return A Flow that emits partial summary updates as they are generated.
     */
    fun generateSummary(transcript: String): Flow<SummaryUpdate>
}
