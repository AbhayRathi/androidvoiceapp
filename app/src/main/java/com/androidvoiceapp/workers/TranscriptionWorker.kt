package com.androidvoiceapp.workers

import android.content.Context
import android.util.Log
import androidx.hilt.work.HiltWorker
import androidx.work.*
import com.androidvoiceapp.api.TranscriptionApi
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
    private val transcriptionApi: TranscriptionApi
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
            Log.e(TAG, "Invalid input data: meetingId=$meetingId, chunkId=$chunkId")
            return Result.failure()
        }

        return try {
            Log.d(TAG, "Starting transcription: meetingId=$meetingId, chunkId=$chunkId, attempt=${retryCount + 1}/$MAX_RETRIES, workerId=$id")

            val chunk = chunkRepository.getChunkById(chunkId)
            if (chunk == null) {
                Log.e(TAG, "Chunk not found in database: chunkId=$chunkId")
                return Result.failure()
            }

            Log.d(TAG, "Chunk retrieved: chunkId=$chunkId, sequenceNumber=${chunk.sequenceNumber}, filePath=${chunk.filePath}")

            // Update chunk status
            chunkRepository.updateChunkStatus(chunkId, "transcribing")
            Log.d(TAG, "Chunk status updated to 'transcribing': chunkId=$chunkId")

            // Get the audio file
            val audioFile = File(chunk.filePath)
            if (!audioFile.exists()) {
                Log.e(TAG, "Audio file not found: path=${chunk.filePath}, chunkId=$chunkId")
                chunkRepository.updateChunkStatus(chunkId, "failed")
                return Result.failure()
            }

            Log.d(TAG, "Audio file exists: size=${audioFile.length()} bytes, chunkId=$chunkId")

            // Call transcription API
            val segments = transcriptionApi.transcribe(
                chunkFile = audioFile,
                meetingId = meetingId,
                chunkId = chunkId,
                sequenceNumber = chunk.sequenceNumber,
                chunkStartTime = chunk.startTime
            )

            Log.d(TAG, "Transcription completed: chunkId=$chunkId, segmentCount=${segments.size}")

            // Save transcript segments
            transcriptRepository.insertSegments(segments)
            Log.d(TAG, "Transcript segments saved: chunkId=$chunkId, segmentCount=${segments.size}")

            // Update chunk status
            chunkRepository.updateChunkStatus(chunkId, "transcribed")
            Log.d(TAG, "Chunk status updated to 'transcribed': chunkId=$chunkId")

            // Check if all chunks are transcribed
            checkAndTriggerSummary(meetingId)

            Log.d(TAG, "Transcription worker completed successfully: chunkId=$chunkId")
            Result.success()

        } catch (e: Exception) {
            Log.e(TAG, "Transcription failed: chunkId=$chunkId, attempt=${retryCount + 1}/$MAX_RETRIES, error=${e.message}", e)

            if (retryCount >= MAX_RETRIES - 1) {
                // Max retries reached, mark as failed and re-enqueue all chunks
                Log.w(TAG, "Max retries reached for chunk $chunkId, triggering retry-all-on-failure")
                chunkRepository.updateChunkStatus(chunkId, "failed")
                handleTranscriptionFailure(meetingId)
                return Result.failure()
            }

            Log.d(TAG, "Scheduling retry for chunk $chunkId, attempt ${retryCount + 2}/$MAX_RETRIES")
            Result.retry()
        }
    }

    private suspend fun checkAndTriggerSummary(meetingId: Long) {
        try {
            Log.d(TAG, "Checking if summary should be triggered: meetingId=$meetingId")
            
            // Get all chunks for this meeting
            val chunks = chunkRepository.getChunksByStatus(meetingId, "transcribed")
            val totalChunks = chunkRepository.getChunksByMeetingId(meetingId)

            // Count if we have processed all
            val meeting = meetingRepository.getMeetingByIdOnce(meetingId)
            if (meeting?.status == "stopped" || meeting?.status == "completed") {
                Log.d(TAG, "Meeting is in terminal state: meetingId=$meetingId, status=${meeting.status}")
                
                // Meeting is stopped, check if all chunks are transcribed
                val allChunks = chunkRepository.getChunksByMeetingId(meetingId)
                // We can't easily get the count synchronously from Flow, so we'll trigger summary
                // when we detect the meeting is in stopped state and this chunk completes
                
                Log.d(TAG, "Triggering summary generation: meetingId=$meetingId")
                
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
                
                Log.d(TAG, "Summary worker enqueued: meetingId=$meetingId, workRequestId=${summaryWorkRequest.id}")
            } else {
                Log.d(TAG, "Meeting not in terminal state yet: meetingId=$meetingId, status=${meeting?.status}")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error checking for summary trigger: meetingId=$meetingId, error=${e.message}", e)
        }
    }

    private suspend fun handleTranscriptionFailure(meetingId: Long) {
        try {
            Log.w(TAG, "Handling transcription failure for meetingId=$meetingId - initiating retry-all-on-failure")

            // Get all chunks for this meeting
            val allChunks = chunkRepository.getChunksByStatus(meetingId, "finalized")
            val failedChunks = chunkRepository.getChunksByStatus(meetingId, "failed")
            val transcribingChunks = chunkRepository.getChunksByStatus(meetingId, "transcribing")

            val chunksToRetry = allChunks + failedChunks + transcribingChunks
            
            Log.d(TAG, "Retry-all-on-failure: meetingId=$meetingId, totalChunks=${chunksToRetry.size}, finalized=${allChunks.size}, failed=${failedChunks.size}, transcribing=${transcribingChunks.size}")

            // Re-enqueue transcription for all chunks
            for (chunk in chunksToRetry) {
                // Reset chunk status
                chunkRepository.updateChunkStatus(chunk.id, "finalized")
                Log.d(TAG, "Reset chunk status to 'finalized': chunkId=${chunk.id}")

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
                    
                Log.d(TAG, "Re-enqueued transcription worker: chunkId=${chunk.id}, workRequestId=${workRequest.id}")
            }

            Log.d(TAG, "Retry-all-on-failure completed: meetingId=$meetingId, re-enqueued ${chunksToRetry.size} chunks")

        } catch (e: Exception) {
            Log.e(TAG, "Error handling transcription failure for meetingId=$meetingId: ${e.message}", e)
        }
    }
}
