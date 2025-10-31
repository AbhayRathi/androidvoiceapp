package com.androidvoiceapp.workers

import android.content.Context
import android.util.Log
import androidx.hilt.work.HiltWorker
import androidx.work.*
import com.androidvoiceapp.api.SummaryApi
import com.androidvoiceapp.data.repository.MeetingRepository
import com.androidvoiceapp.data.repository.SummaryRepository
import com.androidvoiceapp.data.repository.TranscriptRepository
import com.androidvoiceapp.data.room.SummaryEntity
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.collect
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

/**
 * Worker to generate meeting summaries with streaming updates
 * Runs as foreground work to survive app kill
 */
@HiltWorker
class SummaryWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted workerParams: WorkerParameters,
    private val summaryRepository: SummaryRepository,
    private val transcriptRepository: TranscriptRepository,
    private val meetingRepository: MeetingRepository,
    private val summaryApi: SummaryApi
) : CoroutineWorker(context, workerParams) {

    companion object {
        private const val TAG = "SummaryWorker"
        private const val NOTIFICATION_ID = 2001
    }

    private val json = Json { 
        ignoreUnknownKeys = true
        encodeDefaults = true
    }

    override suspend fun doWork(): Result {
        val meetingId = inputData.getLong("meeting_id", -1L)

        if (meetingId == -1L) {
            Log.e(TAG, "Invalid meeting ID")
            return Result.failure()
        }

        return try {
            Log.d(TAG, "Starting summary generation: meetingId=$meetingId, workerId=$id")

            // Create or get existing summary
            var summary = summaryRepository.getSummaryByMeetingIdOnce(meetingId)
            if (summary == null) {
                Log.d(TAG, "Creating new summary entity: meetingId=$meetingId")
                summary = SummaryEntity(
                    meetingId = meetingId,
                    status = "generating"
                )
                summaryRepository.createOrUpdateSummary(summary)
            } else {
                Log.d(TAG, "Using existing summary: meetingId=$meetingId, status=${summary.status}")
                // Update status to generating
                summaryRepository.updateSummaryStatus(meetingId, "generating")
            }

            // Get all transcript segments
            val segmentCount = transcriptRepository.getSegmentCount(meetingId)
            Log.d(TAG, "Transcript segment count: meetingId=$meetingId, count=$segmentCount")
            
            if (segmentCount == 0) {
                Log.w(TAG, "No transcript segments found: meetingId=$meetingId")
                summaryRepository.updateSummaryStatus(
                    meetingId,
                    "failed",
                    "No transcript available"
                )
                return Result.failure()
            }

            // Build full transcript text (in real implementation, would stream from DB)
            // For now, use a placeholder since we're using mock API anyway
            val transcriptText = "Full meeting transcript text here"
            Log.d(TAG, "Starting streaming summary generation: meetingId=$meetingId")

            // Stream summary generation
            summaryApi.generateSummaryStream(transcriptText)
                .catch { e ->
                    Log.e(TAG, "Error in summary stream: meetingId=$meetingId, error=${e.message}", e)
                    summaryRepository.updateSummaryStatus(
                        meetingId,
                        "failed",
                        e.message
                    )
                }
                .collect { update ->
                    Log.d(TAG, "Summary update received: meetingId=$meetingId, progress=${(update.progress * 100).toInt()}%, isComplete=${update.isComplete}")
                    
                    // Convert lists to JSON strings
                    val actionItemsJson = json.encodeToString(update.actionItems)
                    val keyPointsJson = json.encodeToString(update.keyPoints)
                    
                    // Update database with streaming progress
                    summaryRepository.updateSummaryContent(
                        meetingId = meetingId,
                        title = update.title,
                        summary = update.summary,
                        actionItems = actionItemsJson,
                        keyPoints = keyPointsJson,
                        progress = update.progress,
                        status = if (update.isComplete) "completed" else "generating"
                    )

                    if (update.isComplete) {
                        // Update meeting status
                        meetingRepository.updateMeetingStatus(meetingId, "completed")
                        Log.d(TAG, "Summary generation completed: meetingId=$meetingId")
                    }
                }

            Log.d(TAG, "Summary worker completed successfully: meetingId=$meetingId")
            Result.success()

        } catch (e: Exception) {
            Log.e(TAG, "Failed to generate summary: meetingId=$meetingId, error=${e.message}", e)
            summaryRepository.updateSummaryStatus(
                meetingId,
                "failed",
                e.message
            )
            Result.failure()
        }
    }

    override suspend fun getForegroundInfo(): ForegroundInfo {
        return ForegroundInfo(
            NOTIFICATION_ID,
            createNotification()
        )
    }

    private fun createNotification(): android.app.Notification {
        val channelId = "summary_channel"
        
        // Create notification channel for API 26+
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            val channel = android.app.NotificationChannel(
                channelId,
                "Summary Generation",
                android.app.NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Generating meeting summaries"
                setSound(null, null)
            }
            val notificationManager = applicationContext.getSystemService(
                android.app.NotificationManager::class.java
            )
            notificationManager.createNotificationChannel(channel)
        }

        return androidx.core.app.NotificationCompat.Builder(applicationContext, channelId)
            .setContentTitle("Generating Summary")
            .setContentText("Processing meeting transcript...")
            .setSmallIcon(com.androidvoiceapp.R.drawable.ic_launcher_foreground)
            .setOngoing(true)
            .setPriority(androidx.core.app.NotificationCompat.PRIORITY_LOW)
            .build()
    }
}
