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
import kotlinx.coroutines.flow.first
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.json.Json

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
                summaryRepository.updateSummaryStatus(meetingId, "generating")
            }

            // Get all transcript segments
            val segments = transcriptRepository.getSegmentsByMeetingId(meetingId).first()
            if (segments.isEmpty()) {
                Log.w(TAG, "No transcript segments found: meetingId=$meetingId")
                summaryRepository.updateSummaryStatus(
                    meetingId,
                    "failed",
                    "No transcript available"
                )
                return Result.failure()
            }

            // Build full transcript text
            val transcriptText = segments.joinToString("\n") { it.text }
            Log.d(TAG, "Starting streaming summary generation: meetingId=$meetingId")

            // Stream summary generation
            var fullSummary = ""
            summaryApi.generateSummary(transcriptText).collect {
                fullSummary += it.text
                // For now, just save the full text as it comes in.
                // A more advanced implementation would parse the structured data.
                summaryRepository.updateSummaryContent(
                    meetingId = meetingId,
                    title = "", // Extracted from fullSummary later
                    summary = fullSummary,
                    actionItems = "", // Extracted from fullSummary later
                    keyPoints = "", // Extracted from fullSummary later
                    progress = 0.5f, // Placeholder progress
                    status = "generating"
                )
            }

            // Once the flow is complete, parse the full summary
            val parsedSummary = parseSummary(fullSummary)
            summaryRepository.updateSummaryContent(
                meetingId = meetingId,
                title = parsedSummary.title,
                summary = parsedSummary.summary,
                actionItems = json.encodeToString(ListSerializer(String.serializer()), parsedSummary.actionItems),
                keyPoints = json.encodeToString(ListSerializer(String.serializer()), parsedSummary.keyPoints),
                progress = 1.0f,
                status = "completed"
            )
            meetingRepository.updateMeetingStatus(meetingId, "completed")

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

    private fun parseSummary(fullText: String): ParsedSummary {
        val title = fullText.substringAfter("Title:", "").substringBefore("\n").trim()
        val summary = fullText.substringAfter("Summary:", "").substringBefore("Action Items:").trim()
        val actionItemsText = fullText.substringAfter("Action Items:", "").substringBefore("Key Points:").trim()
        val keyPointsText = fullText.substringAfter("Key Points:", "").trim()

        val actionItems = actionItemsText.lines().filter { it.isNotBlank() }.map { it.trim().removePrefix("*").trim() }
        val keyPoints = keyPointsText.lines().filter { it.isNotBlank() }.map { it.trim().removePrefix("*").trim() }

        return ParsedSummary(title, summary, actionItems, keyPoints)
    }

    data class ParsedSummary(val title: String, val summary: String, val actionItems: List<String>, val keyPoints: List<String>)

    override suspend fun getForegroundInfo(): ForegroundInfo {
        return ForegroundInfo(
            NOTIFICATION_ID,
            createNotification()
        )
    }

    private fun createNotification(): android.app.Notification {
        val channelId = "summary_channel"
        
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
