package com.voicerecorder.workers

import android.content.Context
import android.util.Log
import androidx.hilt.work.HiltWorker
import androidx.work.*
import com.voicerecorder.api.TranscriptionApi
import com.voicerecorder.data.repository.ChunkRepository
import com.voicerecorder.data.repository.TranscriptRepository
import com.voicerecorder.data.room.TranscriptSegment
import com.voicerecorder.data.room.TranscriptionStatus
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import java.io.File
import java.util.concurrent.TimeUnit

@HiltWorker
class TranscriptionWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted workerParams: WorkerParameters,
    private val transcriptionApi: TranscriptionApi,
    private val chunkRepository: ChunkRepository,
    private val transcriptRepository: TranscriptRepository
) : CoroutineWorker(context, workerParams) {

    companion object {
        private const val TAG = "TranscriptionWorker"
        const val KEY_CHUNK_ID = "chunk_id"
        const val KEY_MEETING_ID = "meeting_id"
        private const val MAX_RETRIES = 3
        
        fun createWorkRequest(chunkId: Long, meetingId: Long): OneTimeWorkRequest {
            val inputData = workDataOf(
                KEY_CHUNK_ID to chunkId,
                KEY_MEETING_ID to meetingId
            )
            
            return OneTimeWorkRequestBuilder<TranscriptionWorker>()
                .setInputData(inputData)
                .setConstraints(
                    Constraints.Builder()
                        .setRequiredNetworkType(NetworkType.CONNECTED)
                        .build()
                )
                .setBackoffCriteria(
                    BackoffPolicy.EXPONENTIAL,
                    30,
                    TimeUnit.SECONDS
                )
                .build()
        }
    }

    override suspend fun doWork(): Result {
        val chunkId = inputData.getLong(KEY_CHUNK_ID, -1)
        val meetingId = inputData.getLong(KEY_MEETING_ID, -1)

        if (chunkId == -1L || meetingId == -1L) {
            Log.e(TAG, "Invalid chunk ID or meeting ID")
            return Result.failure()
        }

        return try {
            Log.d(TAG, "Transcribing chunk $chunkId")

            val chunk = chunkRepository.getChunkById(chunkId)
            if (chunk == null) {
                Log.e(TAG, "Chunk not found: $chunkId")
                return Result.failure()
            }

            // Update status to in progress
            chunkRepository.updateTranscriptionStatus(chunkId, TranscriptionStatus.IN_PROGRESS, chunk.retryCount)

            // Transcribe audio file
            val audioFile = File(chunk.filePath)
            if (!audioFile.exists()) {
                Log.e(TAG, "Audio file not found: ${chunk.filePath}")
                return Result.failure()
            }

            val transcriptResult = transcriptionApi.transcribe(audioFile, chunk.chunkNumber)

            // Save transcript segments to database
            val segments = transcriptResult.segments.map { segmentData ->
                TranscriptSegment(
                    meetingId = meetingId,
                    chunkId = chunkId,
                    text = segmentData.text,
                    timestamp = segmentData.startTime,
                    startTime = segmentData.startTime,
                    endTime = segmentData.endTime,
                    confidence = segmentData.confidence
                )
            }
            transcriptRepository.insertSegments(segments)

            // Update status to completed
            chunkRepository.updateTranscriptionStatus(chunkId, TranscriptionStatus.COMPLETED, chunk.retryCount)

            Log.d(TAG, "Chunk $chunkId transcribed successfully")

            // Check if all chunks are transcribed, then trigger summary
            checkAndTriggerSummary(meetingId)

            Result.success()
        } catch (e: Exception) {
            Log.e(TAG, "Failed to transcribe chunk $chunkId", e)
            
            val chunk = chunkRepository.getChunkById(chunkId)
            if (chunk != null && chunk.retryCount >= MAX_RETRIES) {
                // Max retries reached, trigger retry-all-on-failure
                Log.e(TAG, "Max retries reached for chunk $chunkId, resetting all chunks")
                chunkRepository.updateAllTranscriptionStatus(meetingId, TranscriptionStatus.PENDING)
                
                // Re-enqueue all chunks
                val allChunks = chunkRepository.getChunksByTranscriptionStatus(meetingId, TranscriptionStatus.PENDING)
                allChunks.forEach { pendingChunk ->
                    val work = createWorkRequest(pendingChunk.id, meetingId)
                    WorkManager.getInstance(applicationContext)
                        .enqueueUniqueWork(
                            "transcription_${pendingChunk.id}",
                            ExistingWorkPolicy.REPLACE,
                            work
                        )
                }
                
                Result.failure()
            } else {
                // Increment retry count
                chunkRepository.updateTranscriptionStatus(
                    chunkId,
                    TranscriptionStatus.FAILED,
                    (chunk?.retryCount ?: 0) + 1
                )
                Result.retry()
            }
        }
    }

    private suspend fun checkAndTriggerSummary(meetingId: Long) {
        val pendingChunks = chunkRepository.getChunksByTranscriptionStatus(meetingId, TranscriptionStatus.PENDING)
        val inProgressChunks = chunkRepository.getChunksByTranscriptionStatus(meetingId, TranscriptionStatus.IN_PROGRESS)
        
        if (pendingChunks.isEmpty() && inProgressChunks.isEmpty()) {
            // All chunks transcribed, trigger summary generation
            Log.d(TAG, "All chunks transcribed, triggering summary for meeting $meetingId")
            val summaryWork = SummaryWorker.createWorkRequest(meetingId)
            WorkManager.getInstance(applicationContext)
                .enqueueUniqueWork(
                    "summary_$meetingId",
                    ExistingWorkPolicy.KEEP,
                    summaryWork
                )
        }
    }
}
