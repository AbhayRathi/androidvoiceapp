package com.abhay.voiceapp.worker

import android.content.Context
import android.util.Log
import androidx.hilt.work.HiltWorker
import androidx.work.*
import com.abhay.voiceapp.audio.WavFileWriter
import com.abhay.voiceapp.data.entity.ChunkStatus
import com.abhay.voiceapp.data.repository.ChunkRepository
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import java.io.File
import java.util.concurrent.TimeUnit

@HiltWorker
class FinalizeChunkWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted workerParams: WorkerParameters,
    private val chunkRepository: ChunkRepository,
    private val wavFileWriter: WavFileWriter
) : CoroutineWorker(context, workerParams) {
    
    companion object {
        private const val TAG = "FinalizeChunkWorker"
        private const val KEY_CHUNK_ID = "chunk_id"
        
        fun enqueue(context: Context, chunkId: Long) {
            val workRequest = OneTimeWorkRequestBuilder<FinalizeChunkWorker>()
                .setInputData(workDataOf(KEY_CHUNK_ID to chunkId))
                .setBackoffCriteria(
                    BackoffPolicy.EXPONENTIAL,
                    WorkRequest.MIN_BACKOFF_MILLIS,
                    TimeUnit.MILLISECONDS
                )
                .addTag("finalize_chunk_$chunkId")
                .build()
            
            WorkManager.getInstance(context).enqueue(workRequest)
            Log.d(TAG, "Enqueued finalize work for chunk: $chunkId")
        }
    }
    
    override suspend fun doWork(): Result {
        val chunkId = inputData.getLong(KEY_CHUNK_ID, -1)
        if (chunkId == -1L) {
            Log.e(TAG, "Invalid chunk ID")
            return Result.failure()
        }
        
        return try {
            Log.d(TAG, "Finalizing chunk: $chunkId")
            
            val chunk = chunkRepository.getChunkById(chunkId)
            if (chunk == null) {
                Log.e(TAG, "Chunk not found: $chunkId")
                return Result.failure()
            }
            
            // Update status to finalizing
            chunkRepository.updateChunkStatus(chunkId, ChunkStatus.FINALIZING)
            
            // Convert temp file to final WAV file
            val tempFile = File(chunk.tempFilePath ?: "")
            val finalFile = File(chunk.filePath)
            
            if (!tempFile.exists()) {
                Log.e(TAG, "Temp file does not exist: ${tempFile.absolutePath}")
                chunkRepository.updateChunkStatus(chunkId, ChunkStatus.FAILED)
                return Result.failure()
            }
            
            val success = wavFileWriter.finalizeWavFile(tempFile, finalFile)
            
            if (success) {
                // Update chunk status and file path
                chunkRepository.finalizeChunk(chunkId, finalFile.absolutePath)
                Log.d(TAG, "Chunk finalized successfully: $chunkId")
                
                // Enqueue transcription worker
                TranscriptionWorker.enqueue(applicationContext, chunkId)
                
                Result.success()
            } else {
                Log.e(TAG, "Failed to finalize chunk: $chunkId")
                chunkRepository.updateChunkStatus(chunkId, ChunkStatus.FAILED)
                Result.retry()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error finalizing chunk: $chunkId", e)
            Result.retry()
        }
    }
}
