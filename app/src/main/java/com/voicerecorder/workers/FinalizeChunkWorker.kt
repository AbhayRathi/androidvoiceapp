package com.voicerecorder.workers

import android.content.Context
import android.util.Log
import androidx.hilt.work.HiltWorker
import androidx.work.*
import com.voicerecorder.data.repository.ChunkRepository
import com.voicerecorder.data.room.ChunkStatus
import com.voicerecorder.audio.WavWriter
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import java.io.File

@HiltWorker
class FinalizeChunkWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted workerParams: WorkerParameters,
    private val chunkRepository: ChunkRepository
) : CoroutineWorker(context, workerParams) {

    companion object {
        private const val TAG = "FinalizeChunkWorker"
        const val KEY_CHUNK_ID = "chunk_id"
        const val KEY_MEETING_ID = "meeting_id"
        
        fun createWorkRequest(chunkId: Long, meetingId: Long): OneTimeWorkRequest {
            val inputData = workDataOf(
                KEY_CHUNK_ID to chunkId,
                KEY_MEETING_ID to meetingId
            )
            
            return OneTimeWorkRequestBuilder<FinalizeChunkWorker>()
                .setInputData(inputData)
                .setConstraints(
                    Constraints.Builder()
                        .setRequiredNetworkType(NetworkType.NOT_REQUIRED)
                        .build()
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
            Log.d(TAG, "Finalizing chunk $chunkId")

            val chunk = chunkRepository.getChunkById(chunkId)
            if (chunk == null) {
                Log.e(TAG, "Chunk not found: $chunkId")
                return Result.failure()
            }

            // Update chunk status to finalized
            chunkRepository.updateStatus(chunkId, ChunkStatus.FINALIZED)

            // Enqueue transcription worker
            val transcriptionWork = TranscriptionWorker.createWorkRequest(chunkId, meetingId)
            WorkManager.getInstance(applicationContext)
                .enqueueUniqueWork(
                    "transcription_$chunkId",
                    ExistingWorkPolicy.KEEP,
                    transcriptionWork
                )

            Log.d(TAG, "Chunk $chunkId finalized successfully")
            Result.success()
        } catch (e: Exception) {
            Log.e(TAG, "Failed to finalize chunk $chunkId", e)
            Result.retry()
        }
    }
}
