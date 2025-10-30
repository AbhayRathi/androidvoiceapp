package com.androidvoiceapp.workers

import android.content.Context
import android.util.Log
import androidx.hilt.work.HiltWorker
import androidx.work.*
import com.androidvoiceapp.data.repository.ChunkRepository
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject

/**
 * Worker to finalize audio chunks and enqueue transcription
 */
@HiltWorker
class FinalizeChunkWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted workerParams: WorkerParameters,
    private val chunkRepository: ChunkRepository
) : CoroutineWorker(context, workerParams) {

    companion object {
        private const val TAG = "FinalizeChunkWorker"
    }

    override suspend fun doWork(): Result {
        val meetingId = inputData.getLong("meeting_id", -1L)
        val chunkId = inputData.getLong("chunk_id", -1L)

        if (meetingId == -1L || chunkId == -1L) {
            Log.e(TAG, "Invalid input data")
            return Result.failure()
        }

        return try {
            Log.d(TAG, "Finalizing chunk $chunkId for meeting $meetingId")

            // Update chunk status to finalized
            chunkRepository.updateChunkStatus(chunkId, "finalized")

            // Enqueue transcription worker
            val transcriptionWorkRequest = OneTimeWorkRequestBuilder<TranscriptionWorker>()
                .setInputData(
                    workDataOf(
                        "meeting_id" to meetingId,
                        "chunk_id" to chunkId
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
                    "transcribe_chunk_${chunkId}",
                    ExistingWorkPolicy.KEEP,
                    transcriptionWorkRequest
                )

            Log.d(TAG, "Chunk $chunkId finalized and transcription enqueued")
            Result.success()

        } catch (e: Exception) {
            Log.e(TAG, "Failed to finalize chunk $chunkId", e)
            Result.retry()
        }
    }
}
