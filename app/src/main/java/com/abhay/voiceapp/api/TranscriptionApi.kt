package com.abhay.voiceapp.api

import java.io.File

data class TranscriptionSegment(
    val text: String,
    val startTime: Long,
    val endTime: Long,
    val confidence: Float
)

data class TranscriptionResult(
    val segments: List<TranscriptionSegment>,
    val fullText: String
)

interface TranscriptionApi {
    suspend fun transcribe(audioFile: File, chunkIndex: Int): TranscriptionResult
}
