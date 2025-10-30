package com.abhay.voiceapp.api

import kotlinx.coroutines.delay
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.random.Random

@Singleton
class MockTranscriptionProvider @Inject constructor() : TranscriptionApi {
    
    companion object {
        private val SAMPLE_TEXTS = listOf(
            "Hello, this is a sample transcription of the audio recording.",
            "We need to discuss the project timeline and deliverables for the next sprint.",
            "The team has made significant progress on the new features.",
            "Let's schedule a follow-up meeting to review the action items.",
            "Could you please share the updated documentation with everyone?",
            "We should prioritize the bug fixes before adding new functionality.",
            "The client feedback has been mostly positive so far.",
            "I'll send out the meeting notes by end of day today.",
            "Does anyone have any questions or concerns to raise?",
            "Thanks everyone for your contributions to this project."
        )
    }
    
    override suspend fun transcribe(audioFile: File, chunkIndex: Int): TranscriptionResult {
        // Simulate API delay
        delay(2000 + Random.nextLong(1000, 3000))
        
        // Generate deterministic mock transcript based on chunk index
        val textIndex = chunkIndex % SAMPLE_TEXTS.size
        val text = SAMPLE_TEXTS[textIndex]
        
        // Create mock segments (split by sentences or phrases)
        val segments = mutableListOf<TranscriptionSegment>()
        val words = text.split(" ")
        var currentTime = 0L
        
        words.chunked(5).forEach { wordGroup ->
            val segmentText = wordGroup.joinToString(" ")
            val duration = 2000L + Random.nextLong(1000) // 2-3 seconds per segment
            segments.add(
                TranscriptionSegment(
                    text = segmentText,
                    startTime = currentTime,
                    endTime = currentTime + duration,
                    confidence = 0.85f + Random.nextFloat() * 0.14f // 0.85 - 0.99
                )
            )
            currentTime += duration
        }
        
        return TranscriptionResult(
            segments = segments,
            fullText = text
        )
    }
}
