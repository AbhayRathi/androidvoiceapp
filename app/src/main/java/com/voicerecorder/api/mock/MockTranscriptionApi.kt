package com.voicerecorder.api.mock

import com.voicerecorder.api.*
import kotlinx.coroutines.delay
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class MockTranscriptionApi @Inject constructor() : TranscriptionApi {
    
    private val sampleTexts = listOf(
        "Hello and welcome to our meeting today.",
        "Let's discuss the project roadmap and key milestones.",
        "We need to focus on the critical deliverables for this quarter.",
        "The team has been working hard on implementing the new features.",
        "I'd like to hear everyone's thoughts on the proposed timeline.",
        "We should consider the risks and potential blockers.",
        "Let's move forward with the action items we've identified.",
        "Thank you all for your contributions to this discussion."
    )

    override suspend fun transcribe(audioFile: File, chunkNumber: Int): TranscriptResult {
        // Simulate API call delay
        delay(2000)

        // Generate deterministic transcript based on chunk number
        val textIndex = chunkNumber % sampleTexts.size
        val text = sampleTexts[textIndex]
        
        val duration = 30000L // 30 seconds
        val startTime = chunkNumber * duration
        
        val segments = listOf(
            TranscriptSegmentData(
                text = text,
                startTime = startTime,
                endTime = startTime + duration,
                confidence = 0.95f
            )
        )

        return TranscriptResult(
            segments = segments,
            duration = duration
        )
    }
}
