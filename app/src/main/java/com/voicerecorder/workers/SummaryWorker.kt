package com.voicerecorder.workers

import android.content.Context
import android.util.Log
import androidx.hilt.work.HiltWorker
import androidx.work.*
import com.google.gson.Gson
import com.voicerecorder.api.SummaryApi
import com.voicerecorder.api.SummaryProgress
import com.voicerecorder.data.repository.SummaryRepository
import com.voicerecorder.data.repository.TranscriptRepository
import com.voicerecorder.data.room.Summary
import com.voicerecorder.data.room.SummaryStatus
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.flow.first
import java.util.concurrent.TimeUnit

@HiltWorker
class SummaryWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted workerParams: WorkerParameters,
    private val summaryApi: SummaryApi,
    private val summaryRepository: SummaryRepository,
    private val transcriptRepository: TranscriptRepository,
    private val gson: Gson
) : CoroutineWorker(context, workerParams) {

    companion object {
        private const val TAG = "SummaryWorker"
        const val KEY_MEETING_ID = "meeting_id"
        
        fun createWorkRequest(meetingId: Long): OneTimeWorkRequest {
            val inputData = workDataOf(KEY_MEETING_ID to meetingId)
            
            return OneTimeWorkRequestBuilder<SummaryWorker>()
                .setInputData(inputData)
                .setConstraints(
                    Constraints.Builder()
                        .setRequiredNetworkType(NetworkType.CONNECTED)
                        .build()
                )
                .setExpedited(OutOfQuotaPolicy.RUN_AS_NON_EXPEDITED_WORK_REQUEST)
                .setBackoffCriteria(
                    BackoffPolicy.EXPONENTIAL,
                    30,
                    TimeUnit.SECONDS
                )
                .build()
        }
    }

    override suspend fun doWork(): Result {
        val meetingId = inputData.getLong(KEY_MEETING_ID, -1)

        if (meetingId == -1L) {
            Log.e(TAG, "Invalid meeting ID")
            return Result.failure()
        }

        return try {
            Log.d(TAG, "Generating summary for meeting $meetingId")

            // Create or get existing summary entry
            var summary = summaryRepository.getSummary(meetingId)
            if (summary == null) {
                val summaryId = summaryRepository.insertSummary(
                    Summary(
                        meetingId = meetingId,
                        status = SummaryStatus.IN_PROGRESS
                    )
                )
                summary = summaryRepository.getSummary(meetingId)
            } else {
                summaryRepository.updateStatus(meetingId, SummaryStatus.IN_PROGRESS)
            }

            // Get all transcript segments
            val segments = transcriptRepository.getSegmentsForMeeting(meetingId).first()
            if (segments.isEmpty()) {
                Log.e(TAG, "No transcript segments found for meeting $meetingId")
                summaryRepository.updateStatus(meetingId, SummaryStatus.ERROR, "No transcript available")
                return Result.failure()
            }

            // Combine transcript text
            val transcriptText = segments.joinToString(" ") { it.text }

            // Build summary progressively
            val titleBuilder = StringBuilder()
            val summaryTextBuilder = StringBuilder()
            val actionItemsList = mutableListOf<String>()
            val keyPointsList = mutableListOf<String>()

            // Generate summary with progress updates
            val result = summaryApi.generateSummary(transcriptText) { progress ->
                // Update database with streaming progress
                updateSummaryProgress(
                    meetingId,
                    progress,
                    titleBuilder,
                    summaryTextBuilder,
                    actionItemsList,
                    keyPointsList
                )
            }

            // Save final summary
            val finalSummary = Summary(
                id = summary?.id ?: 0,
                meetingId = meetingId,
                title = result.title,
                summary = result.summary,
                actionItems = gson.toJson(result.actionItems),
                keyPoints = gson.toJson(result.keyPoints),
                status = SummaryStatus.COMPLETED,
                progress = 100,
                updatedAt = System.currentTimeMillis()
            )
            summaryRepository.updateSummary(finalSummary)

            Log.d(TAG, "Summary generated successfully for meeting $meetingId")
            Result.success()
        } catch (e: Exception) {
            Log.e(TAG, "Failed to generate summary for meeting $meetingId", e)
            summaryRepository.updateStatus(
                meetingId,
                SummaryStatus.ERROR,
                e.message ?: "Unknown error"
            )
            
            if (runAttemptCount < 3) {
                Result.retry()
            } else {
                Result.failure()
            }
        }
    }

    private suspend fun updateSummaryProgress(
        meetingId: Long,
        progress: SummaryProgress,
        titleBuilder: StringBuilder,
        summaryTextBuilder: StringBuilder,
        actionItemsList: MutableList<String>,
        keyPointsList: MutableList<String>
    ) {
        when (progress.section) {
            "title" -> {
                titleBuilder.clear()
                titleBuilder.append(progress.content)
            }
            "summary" -> {
                summaryTextBuilder.clear()
                summaryTextBuilder.append(progress.content)
            }
            "actionItems" -> {
                if (!actionItemsList.contains(progress.content)) {
                    actionItemsList.add(progress.content)
                }
            }
            "keyPoints" -> {
                if (!keyPointsList.contains(progress.content)) {
                    keyPointsList.add(progress.content)
                }
            }
        }

        // Update summary in database
        val currentSummary = summaryRepository.getSummary(meetingId)
        if (currentSummary != null) {
            val updatedSummary = currentSummary.copy(
                title = titleBuilder.toString(),
                summary = summaryTextBuilder.toString(),
                actionItems = gson.toJson(actionItemsList),
                keyPoints = gson.toJson(keyPointsList),
                progress = progress.progress,
                status = SummaryStatus.IN_PROGRESS,
                updatedAt = System.currentTimeMillis()
            )
            summaryRepository.updateSummary(updatedSummary)
        }
    }
}
