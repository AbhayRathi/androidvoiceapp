package com.androidvoiceapp.api.mock

import android.util.Log
import com.androidvoiceapp.data.room.TranscriptSegmentEntity
import kotlinx.coroutines.delay
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Mock transcription API for testing
 * Returns deterministic transcripts based on chunk sequence
 */
@Singleton
class MockTranscriptionApi @Inject constructor() {
    
    companion object {
        private const val TAG = "MockTranscriptionApi"
        
        private val SAMPLE_TEXTS = listOf(
            "Welcome to the meeting. Today we will discuss the project timeline and deliverables.",
            "The first item on our agenda is the technical architecture review.",
            "We need to ensure all team members are aligned on the implementation approach.",
            "Let's review the key milestones and deadlines for this quarter.",
            "The next phase will focus on integration testing and quality assurance.",
            "We should schedule a follow-up meeting to address any remaining concerns.",
            "Action items will be sent to all participants after this meeting.",
            "Thank you all for your valuable input and contributions today."
        )
    }
    
    /**
     * Transcribe audio chunk (mock implementation)
     * @param chunkFile The WAV file to transcribe
     * @param meetingId The meeting ID
     * @param chunkId The chunk ID
     * @param sequenceNumber The sequence number of the chunk
     * @param chunkStartTime The start time of this chunk in the meeting (ms)
     * @return List of transcript segments
     */
    suspend fun transcribe(
        chunkFile: File,
        meetingId: Long,
        chunkId: Long,
        sequenceNumber: Int,
        chunkStartTime: Long
    ): List<TranscriptSegmentEntity> {
        Log.d(TAG, "Transcribing chunk $sequenceNumber for meeting $meetingId")
        
        // Simulate API call delay (2-5 seconds)
        delay((2000L..5000L).random())
        
        // Generate deterministic transcript based on sequence number
        val textIndex = sequenceNumber % SAMPLE_TEXTS.size
        val text = SAMPLE_TEXTS[textIndex]
        
        // Create a single segment for simplicity
        // In real implementation, this would return multiple segments with timestamps
        val segment = TranscriptSegmentEntity(
            meetingId = meetingId,
            chunkId = chunkId,
            text = text,
            startTime = chunkStartTime,
            endTime = chunkStartTime + 30000, // 30 seconds
            confidence = 0.95f
        )
        
        Log.d(TAG, "Transcription complete for chunk $sequenceNumber")
        return listOf(segment)
    }
    
    /**
     * Check if transcription is available (always true for mock)
     */
    fun isAvailable(): Boolean = true
}
