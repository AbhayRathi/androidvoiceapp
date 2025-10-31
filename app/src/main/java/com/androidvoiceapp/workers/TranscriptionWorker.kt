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
import kotlinx.coroutines.flow.first
import java.io.File

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

        if (meetingId == -1L || chunkId == -1L) {
            return Result.failure()
        }

        return try {
            val chunk = chunkRepository.getChunkById(chunkId) ?: return Result.failure()

            chunkRepository.updateChunkStatus(chunkId, "transcribing")

            val audioFile = File(chunk.filePath)
            if (!audioFile.exists()) {
                chunkRepository.updateChunkStatus(chunkId, "failed")
                return Result.failure()
            }

            val segments = transcriptionApi.transcribe(
                chunkFile = audioFile,
                meetingId = meetingId,
                chunkId = chunkId,
                sequenceNumber = chunk.sequenceNumber,
                chunkStartTime = chunk.startTime
            )

            transcriptRepository.insertSegments(segments)
            chunkRepository.updateChunkStatus(chunkId, "transcribed")

            checkAndTriggerSummary(meetingId)

            Result.success()
        } catch (e: Exception) {
            if (runAttemptCount >= MAX_RETRIES - 1) {
                chunkRepository.updateChunkStatus(chunkId, "failed")
                handleTranscriptionFailure(meetingId)
                return Result.failure()
            }
            Result.retry()
        }
    }

    private suspend fun checkAndTriggerSummary(meetingId: Long) {
        val meeting = meetingRepository.getMeetingByIdOnce(meetingId)

        // Only proceed if the meeting has finished recording.
        if (meeting?.status == "stopped" || meeting?.status == "completed") {
            val allChunks = chunkRepository.getChunksByMeetingId(meetingId).first()

            // Check if every single chunk has been successfully transcribed.
            val allTranscribed = allChunks.isNotEmpty() && allChunks.all { it.status == "transcribed" }

            if (allTranscribed) {
                Log.d(TAG, "All chunks for meeting $meetingId are transcribed. Triggering summary.")
                val summaryWorkRequest = OneTimeWorkRequestBuilder<SummaryWorker>()
                    .setInputData(workDataOf("meeting_id" to meetingId))
                    .setExpedited(OutOfQuotaPolicy.RUN_AS_NON_EXPEDITED_WORK_REQUEST)
                    .build()

                WorkManager.getInstance(applicationContext)
                    .enqueueUniqueWork(
                        "summary_meeting_${meetingId}",
                        ExistingWorkPolicy.KEEP,
                        summaryWorkRequest
                    )
            } else {
                val transcribedCount = allChunks.count { it.status == "transcribed" }
                Log.d(TAG, "Summary not triggered for meeting $meetingId: Meeting stopped, but only $transcribedCount/${allChunks.size} chunks are transcribed.")
            }
        }
    }

    private suspend fun handleTranscriptionFailure(meetingId: Long) {
        Log.e(TAG, "Max retries reached for a chunk in meeting $meetingId. Notifying meeting.")
        meetingRepository.updateMeetingStatus(meetingId, "failed")
    }
}
