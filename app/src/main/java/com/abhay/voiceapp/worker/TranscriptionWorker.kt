package com.abhay.voiceapp.worker

import android.content.Context
import android.util.Log
import androidx.hilt.work.HiltWorker
import androidx.work.*
import com.abhay.voiceapp.api.TranscriptionApi
import com.abhay.voiceapp.data.entity.ChunkStatus
import com.abhay.voiceapp.data.entity.MeetingStatus
import com.abhay.voiceapp.data.entity.TranscriptSegment
import com.abhay.voiceapp.data.repository.ChunkRepository
import com.abhay.voiceapp.data.repository.MeetingRepository
import com.abhay.voiceapp.data.repository.TranscriptRepository
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import java.io.File
import java.util.concurrent.TimeUnit

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
        private const val KEY_CHUNK_ID = "chunk_id"
        
        fun enqueue(context: Context, chunkId: Long) {
            val workRequest = OneTimeWorkRequestBuilder<TranscriptionWorker>()
                .setInputData(workDataOf(KEY_CHUNK_ID to chunkId))
                .setBackoffCriteria(
                    BackoffPolicy.EXPONENTIAL,
                    WorkRequest.MIN_BACKOFF_MILLIS,
                    TimeUnit.MILLISECONDS
                )
                .setConstraints(
                    Constraints.Builder()
                        .setRequiredNetworkType(NetworkType.CONNECTED)
                        .build()
                )
                .addTag("transcribe_chunk_$chunkId")
                .build()
            
            WorkManager.getInstance(context).enqueue(workRequest)
            Log.d(TAG, "Enqueued transcription work for chunk: $chunkId")
        }
    }
    
    override suspend fun doWork(): Result {
        val chunkId = inputData.getLong(KEY_CHUNK_ID, -1)
        if (chunkId == -1L) {
            Log.e(TAG, "Invalid chunk ID")
            return Result.failure()
        }
        
        return try {
            Log.d(TAG, "Transcribing chunk: $chunkId")
            
            val chunk = chunkRepository.getChunkById(chunkId)
            if (chunk == null) {
                Log.e(TAG, "Chunk not found: $chunkId")
                return Result.failure()
            }
            
            // Update status
            chunkRepository.updateChunkStatus(chunkId, ChunkStatus.TRANSCRIBING)
            meetingRepository.updateMeetingStatus(chunk.meetingId, MeetingStatus.PROCESSING)
            
            // Transcribe audio file
            val audioFile = File(chunk.filePath)
            if (!audioFile.exists()) {
                Log.e(TAG, "Audio file not found: ${chunk.filePath}")
                chunkRepository.updateChunkStatus(chunkId, ChunkStatus.FAILED)
                return Result.failure()
            }
            
            val result = transcriptionApi.transcribe(audioFile, chunk.chunkIndex)
            
            // Save transcript segments to database
            val segments = result.segments.map { segment ->
                TranscriptSegment(
                    meetingId = chunk.meetingId,
                    chunkId = chunkId,
                    text = segment.text,
                    startTime = chunk.startTime + segment.startTime,
                    endTime = chunk.startTime + segment.endTime,
                    confidence = segment.confidence
                )
            }
            
            transcriptRepository.saveTranscriptSegments(segments)
            
            // Update chunk status
            chunkRepository.updateChunkStatus(chunkId, ChunkStatus.TRANSCRIBED)
            
            Log.d(TAG, "Chunk transcribed successfully: $chunkId with ${segments.size} segments")
            
            // Check if all chunks are transcribed, then trigger summary generation
            checkAndTriggerSummary(chunk.meetingId)
            
            Result.success()
        } catch (e: Exception) {
            Log.e(TAG, "Error transcribing chunk: $chunkId", e)
            chunkRepository.updateChunkStatus(chunkId, ChunkStatus.FAILED)
            Result.retry()
        }
    }
    
    private suspend fun checkAndTriggerSummary(meetingId: Long) {
        val chunks = chunkRepository.getChunksByMeetingIdSync(meetingId)
        val allTranscribed = chunks.all { it.status == ChunkStatus.TRANSCRIBED }
        
        if (allTranscribed && chunks.isNotEmpty()) {
            Log.d(TAG, "All chunks transcribed for meeting: $meetingId, triggering summary")
            SummaryWorker.enqueue(applicationContext, meetingId)
        }
    }
}
