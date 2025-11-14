package com.androidvoiceapp

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.work.Configuration
import androidx.work.testing.SynchronousExecutor
import androidx.work.testing.WorkManagerTestInitHelper
import com.androidvoiceapp.api.mock.MockSummaryApi
import com.androidvoiceapp.api.mock.MockTranscriptionApi
import com.androidvoiceapp.data.repository.*
import com.androidvoiceapp.data.room.*
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

/**
 * End-to-End Integration Test
 * Tests the complete data flow from meeting creation through transcription to summary generation
 */
@RunWith(AndroidJUnit4::class)
class EndToEndIntegrationTest {

    private lateinit var database: AppDatabase
    private lateinit var context: Context
    
    private lateinit var meetingRepository: MeetingRepository
    private lateinit var chunkRepository: ChunkRepository
    private lateinit var transcriptRepository: TranscriptRepository
    private lateinit var summaryRepository: SummaryRepository
    private lateinit var sessionStateRepository: SessionStateRepository
    
    private lateinit var mockTranscriptionApi: MockTranscriptionApi
    private lateinit var mockSummaryApi: MockSummaryApi

    @Before
    fun setup() {
        context = ApplicationProvider.getApplicationContext()
        
        // Create in-memory database for testing
        database = Room.inMemoryDatabaseBuilder(
            context,
            AppDatabase::class.java
        ).allowMainThreadQueries().build()
        
        // Initialize repositories
        meetingRepository = MeetingRepositoryImpl(database.meetingDao())
        chunkRepository = ChunkRepositoryImpl(database.chunkDao())
        transcriptRepository = TranscriptRepositoryImpl(database.transcriptSegmentDao())
        summaryRepository = SummaryRepositoryImpl(database.summaryDao())
        sessionStateRepository = SessionStateRepositoryImpl(database.sessionStateDao())
        
        // Initialize mock APIs
        mockTranscriptionApi = MockTranscriptionApi()
        mockSummaryApi = MockSummaryApi()
        
        // Initialize WorkManager for testing
        val config = Configuration.Builder()
            .setMinimumLoggingLevel(android.util.Log.DEBUG)
            .setExecutor(SynchronousExecutor())
            .build()
        WorkManagerTestInitHelper.initializeTestWorkManager(context, config)
    }

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun testCompleteMeetingFlow() = runTest {
        println("=== Starting End-to-End Integration Test ===")
        
        // Step 1: Create a meeting
        println("\n1. Creating meeting...")
        val meetingId = meetingRepository.createMeeting("Test Meeting E2E")
        assertNotNull(meetingId)
        assertTrue(meetingId > 0, "Meeting ID should be positive")
        println("✓ Meeting created with ID: $meetingId")
        
        // Verify meeting exists
        val meeting = meetingRepository.getMeetingById(meetingId).first()
        assertNotNull(meeting, "Meeting should exist")
        assertEquals("Test Meeting E2E", meeting.title)
        assertEquals("recording", meeting.status)
        println("✓ Meeting verified in database")
        
        // Step 2: Create audio chunks
        println("\n2. Creating audio chunks...")
        val tempDir = context.cacheDir
        val chunkFiles = mutableListOf<File>()
        val chunkIds = mutableListOf<Long>()
        
        for (i in 0 until 3) {
            val chunkFile = File(tempDir, "test_chunk_$i.wav")
            chunkFile.writeText("Mock audio data for chunk $i") // Mock WAV content
            chunkFiles.add(chunkFile)
            
            val chunkId = chunkRepository.createChunk(
                meetingId = meetingId,
                sequenceNumber = i,
                filePath = chunkFile.absolutePath,
                duration = 30000L, // 30 seconds
                startTime = i * 28000L // 28s intervals (30s - 2s overlap)
            )
            chunkIds.add(chunkId)
            
            chunkRepository.updateChunkStatus(chunkId, "finalized")
            println("✓ Chunk $i created and finalized (ID: $chunkId)")
        }
        
        // Verify chunks exist
        val chunks = chunkRepository.getChunksByMeetingId(meetingId).first()
        assertEquals(3, chunks.size, "Should have 3 chunks")
        println("✓ All chunks verified in database")
        
        // Step 3: Transcribe chunks using mock API
        println("\n3. Transcribing chunks...")
        for (i in 0 until 3) {
            val chunk = chunks[i]
            val chunkFile = File(chunk.filePath)
            
            val segments = mockTranscriptionApi.transcribe(
                chunkFile = chunkFile,
                meetingId = meetingId,
                chunkId = chunk.id,
                sequenceNumber = chunk.sequenceNumber,
                chunkStartTime = chunk.startTime
            )
            
            transcriptRepository.insertSegments(segments)
            chunkRepository.updateChunkStatus(chunk.id, "transcribed")
            
            println("✓ Chunk $i transcribed (${segments.size} segments)")
        }
        
        // Verify transcripts
        val transcriptSegments = transcriptRepository.getSegmentsByMeetingId(meetingId).first()
        assertTrue(transcriptSegments.isNotEmpty(), "Should have transcript segments")
        println("✓ Total transcript segments: ${transcriptSegments.size}")
        
        // Verify ordering
        val sortedSegments = transcriptSegments.sortedBy { it.startTime }
        assertEquals(transcriptSegments, sortedSegments, "Segments should be in chronological order")
        println("✓ Transcript segments are properly ordered")
        
        // Step 4: Generate summary using mock API
        println("\n4. Generating summary...")
        
        // Create initial summary entry
        val summaryEntity = SummaryEntity(
            meetingId = meetingId,
            status = "generating"
        )
        summaryRepository.createOrUpdateSummary(summaryEntity)
        
        // Simulate streaming summary generation
        val transcriptText = transcriptSegments.joinToString(" ") { it.text }
        var updateCount = 0
        
        mockSummaryApi.generateSummaryStream(transcriptText).collect { update ->
            updateCount++
            
            val actionItemsJson = kotlinx.serialization.json.Json.encodeToString(
                kotlinx.serialization.builtins.ListSerializer(kotlinx.serialization.builtins.serializer()),
                update.actionItems
            )
            val keyPointsJson = kotlinx.serialization.json.Json.encodeToString(
                kotlinx.serialization.builtins.ListSerializer(kotlinx.serialization.builtins.serializer()),
                update.keyPoints
            )
            
            summaryRepository.updateSummaryContent(
                meetingId = meetingId,
                title = update.title,
                summary = update.summary,
                actionItems = actionItemsJson,
                keyPoints = keyPointsJson,
                progress = update.progress,
                status = if (update.isComplete) "completed" else "generating"
            )
            
            println("✓ Summary update ${updateCount}: ${(update.progress * 100).toInt()}% complete")
        }
        
        // Verify summary
        val finalSummary = summaryRepository.getSummaryByMeetingId(meetingId).first()
        assertNotNull(finalSummary, "Summary should exist")
        assertEquals("completed", finalSummary.status)
        assertEquals(1.0f, finalSummary.progress)
        assertTrue(finalSummary.title.isNotEmpty(), "Summary should have title")
        assertTrue(finalSummary.summary.isNotEmpty(), "Summary should have content")
        println("✓ Summary generation complete")
        
        // Step 5: Update meeting status
        println("\n5. Finalizing meeting...")
        meetingRepository.updateMeetingStatus(meetingId, "completed")
        meetingRepository.endMeeting(meetingId)
        
        val finalMeeting = meetingRepository.getMeetingById(meetingId).first()
        assertEquals("completed", finalMeeting?.status)
        assertNotNull(finalMeeting?.endTime)
        println("✓ Meeting marked as completed")
        
        // Step 6: Verify complete data integrity
        println("\n6. Verifying data integrity...")
        
        // Check all chunks are transcribed
        val allChunks = chunkRepository.getChunksByMeetingId(meetingId).first()
        assertTrue(allChunks.all { it.status == "transcribed" }, "All chunks should be transcribed")
        
        // Check transcript segments count
        val segmentCount = transcriptRepository.getSegmentCount(meetingId)
        assertEquals(transcriptSegments.size, segmentCount)
        
        // Check summary exists and is complete
        val summary = summaryRepository.getSummaryByMeetingId(meetingId).first()
        assertNotNull(summary)
        assertEquals("completed", summary.status)
        
        println("✓ Data integrity verified")
        
        // Cleanup test files
        chunkFiles.forEach { it.delete() }
        
        println("\n=== End-to-End Integration Test PASSED ===")
        println("Summary:")
        println("  - Meeting ID: $meetingId")
        println("  - Chunks created: ${allChunks.size}")
        println("  - Transcript segments: $segmentCount")
        println("  - Summary status: ${summary.status}")
        println("  - Summary title: ${summary.title}")
    }

    @Test
    fun testSessionStatePersistence() = runTest {
        println("=== Testing Session State Persistence ===")
        
        // Create meeting
        val meetingId = meetingRepository.createMeeting("Session State Test")
        
        // Create session state
        val sessionState = SessionStateEntity(
            meetingId = meetingId,
            isRecording = true,
            isPaused = false,
            currentChunkSequence = 2,
            currentChunkPath = "/path/to/chunk_2.wav",
            recordingStartTime = System.currentTimeMillis() - 60000,
            status = "Recording..."
        )
        
        sessionStateRepository.saveSessionState(sessionState)
        println("✓ Session state saved")
        
        // Retrieve session state
        val retrievedState = sessionStateRepository.getSessionStateOnce(meetingId)
        assertNotNull(retrievedState)
        assertEquals(true, retrievedState.isRecording)
        assertEquals(2, retrievedState.currentChunkSequence)
        println("✓ Session state retrieved and verified")
        
        // Update session state (simulate pause)
        val updatedState = sessionState.copy(
            isPaused = true,
            pausedTime = System.currentTimeMillis(),
            status = "Paused - Phone call"
        )
        sessionStateRepository.updateSessionState(updatedState)
        println("✓ Session state updated (paused)")
        
        // Verify update
        val pausedState = sessionStateRepository.getSessionStateOnce(meetingId)
        assertEquals(true, pausedState?.isPaused)
        assertEquals("Paused - Phone call", pausedState?.status)
        println("✓ Paused state verified")
        
        // Clean up
        sessionStateRepository.deleteSessionState(meetingId)
        val deletedState = sessionStateRepository.getSessionStateOnce(meetingId)
        assertEquals(null, deletedState)
        println("✓ Session state cleanup verified")
        
        println("=== Session State Persistence Test PASSED ===")
    }

    @Test
    fun testChunkTranscriptionOrdering() = runTest {
        println("=== Testing Chunk Transcription Ordering ===")
        
        val meetingId = meetingRepository.createMeeting("Ordering Test")
        val tempDir = context.cacheDir
        
        // Create chunks out of order
        val chunkSequences = listOf(2, 0, 1, 3)
        for (seq in chunkSequences) {
            val chunkFile = File(tempDir, "test_order_chunk_$seq.wav")
            chunkFile.writeText("Mock data $seq")
            
            val chunkId = chunkRepository.createChunk(
                meetingId = meetingId,
                sequenceNumber = seq,
                filePath = chunkFile.absolutePath,
                duration = 30000L,
                startTime = seq * 28000L
            )
            
            // Transcribe immediately
            val segments = mockTranscriptionApi.transcribe(
                chunkFile = chunkFile,
                meetingId = meetingId,
                chunkId = chunkId,
                sequenceNumber = seq,
                chunkStartTime = seq * 28000L
            )
            transcriptRepository.insertSegments(segments)
            
            chunkFile.delete()
        }
        
        // Verify segments are ordered by startTime
        val segments = transcriptRepository.getSegmentsByMeetingId(meetingId).first()
        val startTimes = segments.map { it.startTime }
        val sortedStartTimes = startTimes.sorted()
        
        assertEquals(sortedStartTimes, startTimes, "Segments should be ordered by startTime")
        println("✓ Transcription ordering verified (${segments.size} segments in correct order)")
        
        println("=== Chunk Transcription Ordering Test PASSED ===")
    }

    @Test
    fun testMockApiDeterminism() = runTest {
        println("=== Testing Mock API Determinism ===")
        
        val tempFile = File(context.cacheDir, "test_determinism.wav")
        tempFile.writeText("Mock data")
        
        // Transcribe same chunk multiple times
        val results = mutableListOf<List<TranscriptSegmentEntity>>()
        for (i in 0 until 3) {
            val segments = mockTranscriptionApi.transcribe(
                chunkFile = tempFile,
                meetingId = 1L,
                chunkId = 1L,
                sequenceNumber = 0,
                chunkStartTime = 0L
            )
            results.add(segments)
        }
        
        // Verify all results are identical
        val firstResult = results[0]
        for (i in 1 until results.size) {
            val currentResult = results[i]
            assertEquals(firstResult.size, currentResult.size, "Result $i should have same size")
            for (j in firstResult.indices) {
                assertEquals(firstResult[j].text, currentResult[j].text, "Text should match")
            }
        }
        
        println("✓ Mock transcription API is deterministic")
        
        tempFile.delete()
        println("=== Mock API Determinism Test PASSED ===")
    }
}
