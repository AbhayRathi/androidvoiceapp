package com.voicerecorder.api

import java.io.File

data class TranscriptResult(
    val segments: List<TranscriptSegmentData>,
    val duration: Long
)

data class TranscriptSegmentData(
    val text: String,
    val startTime: Long,
    val endTime: Long,
    val confidence: Float = 1.0f
)

data class SummaryResult(
    val title: String,
    val summary: String,
    val actionItems: List<String>,
    val keyPoints: List<String>
)

interface TranscriptionApi {
    suspend fun transcribe(audioFile: File, chunkNumber: Int): TranscriptResult
}

interface SummaryApi {
    suspend fun generateSummary(transcript: String, onProgress: (SummaryProgress) -> Unit): SummaryResult
}

data class SummaryProgress(
    val section: String, // "title", "summary", "actionItems", "keyPoints"
    val content: String,
    val progress: Int // 0-100
)
