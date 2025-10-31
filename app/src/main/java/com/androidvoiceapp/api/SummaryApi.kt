package com.androidvoiceapp.api

import kotlinx.coroutines.flow.Flow

/**
 * Interface for summary API implementations
 */
interface SummaryApi {
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
    fun generateSummaryStream(transcript: String): Flow<SummaryUpdate>
    
    /**
     * Check if summary API is available
     */
    fun isAvailable(): Boolean
}
