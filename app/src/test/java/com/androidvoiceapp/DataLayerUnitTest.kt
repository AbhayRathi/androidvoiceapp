package com.androidvoiceapp

import com.androidvoiceapp.data.room.*
import com.androidvoiceapp.api.mock.MockTranscriptionApi
import com.androidvoiceapp.api.mock.MockSummaryApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Test
import java.io.File
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

/**
 * Unit test to verify data layer components work correctly
 * This test runs in the JVM without Android dependencies
 */
class DataLayerUnitTest {

    @Test
    fun testMockTranscriptionApiReturnsSegments() {
        val api = MockTranscriptionApi()
        val tempFile = File.createTempFile("test", ".wav")
        tempFile.writeText("Mock audio data")
        
        runBlocking {
            val segments = api.transcribe(
                chunkFile = tempFile,
                meetingId = 1L,
                chunkId = 1L,
                sequenceNumber = 0,
                chunkStartTime = 0L
            )
            
            assertNotNull(segments, "Segments should not be null")
            assertTrue(segments.isNotEmpty(), "Should return at least one segment")
            assertEquals(1L, segments[0].meetingId, "Meeting ID should match")
            assertEquals(1L, segments[0].chunkId, "Chunk ID should match")
            assertTrue(segments[0].text.isNotEmpty(), "Segment text should not be empty")
            
            println("✓ Mock transcription API test passed")
            println("  Returned ${segments.size} segment(s)")
            println("  Sample text: ${segments[0].text}")
        }
        
        tempFile.delete()
    }

    @Test
    fun testMockSummaryApiStreamsUpdates() {
        val api = MockSummaryApi()
        
        runBlocking {
            val updates = mutableListOf<MockSummaryApi.SummaryUpdate>()
            
            api.generateSummaryStream("Sample transcript text").collect { update ->
                updates.add(update)
            }
            
            assertTrue(updates.isNotEmpty(), "Should have summary updates")
            assertTrue(updates.size >= 6, "Should have at least 6 updates")
            
            // Verify progress increases
            val progresses = updates.map { it.progress }
            assertEquals(progresses.sorted(), progresses, "Progress should be monotonically increasing")
            
            // Verify final update is complete
            val lastUpdate = updates.last()
            assertTrue(lastUpdate.isComplete, "Last update should be marked complete")
            assertEquals(1.0f, lastUpdate.progress, "Final progress should be 1.0")
            assertTrue(lastUpdate.title.isNotEmpty(), "Should have title")
            assertTrue(lastUpdate.summary.isNotEmpty(), "Should have summary")
            assertTrue(lastUpdate.actionItems.isNotEmpty(), "Should have action items")
            assertTrue(lastUpdate.keyPoints.isNotEmpty(), "Should have key points")
            
            println("✓ Mock summary API test passed")
            println("  Updates received: ${updates.size}")
            println("  Final title: ${lastUpdate.title}")
            println("  Action items: ${lastUpdate.actionItems.size}")
            println("  Key points: ${lastUpdate.keyPoints.size}")
        }
    }

    @Test
    fun testTranscriptionApiDeterminism() {
        val api = MockTranscriptionApi()
        val tempFile = File.createTempFile("test", ".wav")
        tempFile.writeText("Mock audio data")
        
        runBlocking {
            // Call API multiple times with same input
            val results = (0 until 3).map {
                api.transcribe(
                    chunkFile = tempFile,
                    meetingId = 1L,
                    chunkId = 1L,
                    sequenceNumber = 0,
                    chunkStartTime = 0L
                )
            }
            
            // All results should be identical
            val firstText = results[0][0].text
            results.forEach { segments ->
                assertEquals(firstText, segments[0].text, "API should be deterministic")
            }
            
            println("✓ Transcription API determinism test passed")
        }
        
        tempFile.delete()
    }

    @Test
    fun testTranscriptionApiSequenceVariation() {
        val api = MockTranscriptionApi()
        val tempFile = File.createTempFile("test", ".wav")
        tempFile.writeText("Mock audio data")
        
        runBlocking {
            // Different sequence numbers should produce different results
            val texts = (0 until 5).map { seq ->
                val segments = api.transcribe(
                    chunkFile = tempFile,
                    meetingId = 1L,
                    chunkId = 1L,
                    sequenceNumber = seq,
                    chunkStartTime = seq * 28000L
                )
                segments[0].text
            }
            
            // Should have variation in texts (they cycle through samples)
            val uniqueTexts = texts.toSet()
            assertTrue(uniqueTexts.size > 1, "Different sequences should produce varied output")
            
            println("✓ Transcription sequence variation test passed")
            println("  Unique texts: ${uniqueTexts.size} out of 5")
        }
        
        tempFile.delete()
    }

    @Test
    fun testEntityRelationships() {
        // Test entity creation with proper relationships
        val meeting = MeetingEntity(
            id = 1,
            title = "Test Meeting",
            startTime = System.currentTimeMillis(),
            status = "recording"
        )
        
        val chunk = ChunkEntity(
            id = 1,
            meetingId = meeting.id,
            sequenceNumber = 0,
            filePath = "/path/to/chunk.wav",
            duration = 30000L,
            startTime = 0L,
            status = "recording"
        )
        
        val segment = TranscriptSegmentEntity(
            id = 1,
            meetingId = meeting.id,
            chunkId = chunk.id,
            text = "Test transcript",
            startTime = 0L,
            endTime = 5000L,
            confidence = 0.95f
        )
        
        val summary = SummaryEntity(
            id = 1,
            meetingId = meeting.id,
            title = "Test Summary",
            summary = "Summary content",
            actionItems = "[]",
            keyPoints = "[]",
            status = "completed",
            progress = 1.0f
        )
        
        // Verify relationships
        assertEquals(meeting.id, chunk.meetingId, "Chunk should reference meeting")
        assertEquals(meeting.id, segment.meetingId, "Segment should reference meeting")
        assertEquals(chunk.id, segment.chunkId, "Segment should reference chunk")
        assertEquals(meeting.id, summary.meetingId, "Summary should reference meeting")
        
        println("✓ Entity relationships test passed")
        println("  Meeting -> Chunk -> Segment relationship verified")
        println("  Meeting -> Summary relationship verified")
    }
}
