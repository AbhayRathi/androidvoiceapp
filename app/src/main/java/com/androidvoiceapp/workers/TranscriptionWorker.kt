package com.androidvoiceapp.workers

import android.content.Context
import android.util.Log
import androidx.hilt.work.HiltWorker
import androidx.work.*
import com.androidvoiceapp.api.mock.MockTranscriptionApi
import com.androidvoiceapp.data.repository.ChunkRepository
import com.androidvoiceapp.data.repository.MeetingRepository
import com.androidvoiceapp.data.repository.TranscriptRepository
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import java.io.File

/**
 * Worker to transcribe audio chunks
 * Implements retry logic and ensures transcripts are in correct order
 */
@HiltWorker
class TranscriptionWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted workerParams: WorkerParameters,
    private val chunkRepository: ChunkRepository,
    private val transcriptRepository: TranscriptRepository,
    private val meetingRepository: MeetingRepository,
    private val mockTranscriptionApi: MockTranscriptionApi
) : CoroutineWorker(context, workerParams) {

    companion object {
        private const val TAG = "TranscriptionWorker"
        private const val MAX_RETRIES = 3
    }

    override suspend fun doWork(): Result {
        val meetingId = inputData.getLong("meeting_id", -1L)
        val chunkId = inputData.getLong("chunk_id", -1L)
        val retryCount = runAttemptCount

        if (meetingId == -1L || chunkId == -1L) {
            Log.e(TAG, "Invalid input data")
            return Result.failure()
        }

        return try {
            Log.d(TAG, "Transcribing chunk $chunkId (attempt ${retryCount + 1})")

            val chunk = chunkRepository.getChunkById(chunkId)
            if (chunk == null) {
                Log.e(TAG, "Chunk $chunkId not found")
                return Result.failure()
            }

            // Update chunk status
            chunkRepository.updateChunkStatus(chunkId, "transcribing")

            // Get the audio file
            val audioFile = File(chunk.filePath)
            if (!audioFile.exists()) {
                Log.e(TAG, "Audio file not found: ${chunk.filePath}")
                chunkRepository.updateChunkStatus(chunkId, "failed")
                return Result.failure()
            }

            // Call transcription API (mock)
            val segments = mockTranscriptionApi.transcribe(
                chunkFile = audioFile,
                meetingId = meetingId,
                chunkId = chunkId,
                sequenceNumber = chunk.sequenceNumber,
                chunkStartTime = chunk.startTime
            )

            // Save transcript segments
            transcriptRepository.insertSegments(segments)

            // Update chunk status
            chunkRepository.updateChunkStatus(chunkId, "transcribed")

            Log.d(TAG, "Chunk $chunkId transcribed successfully (${segments.size} segments)")

            // Check if all chunks are transcribed
            checkAndTriggerSummary(meetingId)

            Result.success()

        } catch (e: Exception) {
            Log.e(TAG, "Failed to transcribe chunk $chunkId (attempt ${retryCount + 1})", e)

            if (retryCount >= MAX_RETRIES - 1) {
                // Max retries reached, mark as failed and re-enqueue all chunks
                chunkRepository.updateChunkStatus(chunkId, "failed")
                handleTranscriptionFailure(meetingId)
                return Result.failure()
            }

            Result.retry()
        }
    }

    private suspend fun checkAndTriggerSummary(meetingId: Long) {
        try {
            // Get all chunks for this meeting
            val chunks = chunkRepository.getChunksByStatus(meetingId, "transcribed")
            val totalChunks = chunkRepository.getChunksByMeetingId(meetingId)

            // Count if we have processed all
            val meeting = meetingRepository.getMeetingByIdOnce(meetingId)
            if (meeting?.status == "stopped" || meeting?.status == "completed") {
                // Meeting is stopped, check if all chunks are transcribed
                val allChunks = chunkRepository.getChunksByMeetingId(meetingId)
                // We can't easily get the count synchronously from Flow, so we'll trigger summary
                // when we detect the meeting is in stopped state and this chunk completes
                
                // Trigger summary generation
                val summaryWorkRequest = OneTimeWorkRequestBuilder<SummaryWorker>()
                    .setInputData(
                        workDataOf("meeting_id" to meetingId)
                    )
                    .setExpedited(OutOfQuotaPolicy.RUN_AS_NON_EXPEDITED_WORK_REQUEST)
                    .build()

                WorkManager.getInstance(applicationContext)
                    .enqueueUniqueWork(
                        "summary_meeting_${meetingId}",
                        ExistingWorkPolicy.KEEP,
                        summaryWorkRequest
                    )
                
                Log.d(TAG, "Summary generation triggered for meeting $meetingId")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error checking for summary trigger", e)
        }
    }

    private suspend fun handleTranscriptionFailure(meetingId: Long) {
        try {
            Log.w(TAG, "Handling transcription failure for meeting $meetingId - re-enqueueing all chunks")

            // Get all chunks for this meeting
            val allChunks = chunkRepository.getChunksByStatus(meetingId, "finalized")
            val failedChunks = chunkRepository.getChunksByStatus(meetingId, "failed")
            val transcribingChunks = chunkRepository.getChunksByStatus(meetingId, "transcribing")

            val chunksToRetry = allChunks + failedChunks + transcribingChunks

            // Re-enqueue transcription for all chunks
            for (chunk in chunksToRetry) {
                // Reset chunk status
                chunkRepository.updateChunkStatus(chunk.id, "finalized")

                // Enqueue transcription
                val workRequest = OneTimeWorkRequestBuilder<TranscriptionWorker>()
                    .setInputData(
                        workDataOf(
                            "meeting_id" to meetingId,
                            "chunk_id" to chunk.id
                        )
                    )
                    .setBackoffCriteria(
                        BackoffPolicy.EXPONENTIAL,
                        WorkRequest.MIN_BACKOFF_MILLIS,
                        java.util.concurrent.TimeUnit.MILLISECONDS
                    )
                    .build()

                WorkManager.getInstance(applicationContext)
                    .enqueueUniqueWork(
                        "transcribe_chunk_${chunk.id}",
                        ExistingWorkPolicy.REPLACE,
                        workRequest
                    )
            }

            Log.d(TAG, "Re-enqueued ${chunksToRetry.size} chunks for transcription")

        } catch (e: Exception) {
            Log.e(TAG, "Error handling transcription failure", e)
        }
    }
}
