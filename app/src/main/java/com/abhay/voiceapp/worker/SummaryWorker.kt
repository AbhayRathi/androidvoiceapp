package com.abhay.voiceapp.worker

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.pm.ServiceInfo
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.hilt.work.HiltWorker
import androidx.work.*
import com.abhay.voiceapp.R
import com.abhay.voiceapp.api.SummaryApi
import com.abhay.voiceapp.api.SummaryUpdateType
import com.abhay.voiceapp.data.entity.MeetingStatus
import com.abhay.voiceapp.data.entity.Summary
import com.abhay.voiceapp.data.entity.SummaryStatus
import com.abhay.voiceapp.data.repository.MeetingRepository
import com.abhay.voiceapp.data.repository.SummaryRepository
import com.abhay.voiceapp.data.repository.TranscriptRepository
import com.google.gson.Gson
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.onEach
import java.util.concurrent.TimeUnit

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
        private const val KEY_MEETING_ID = "meeting_id"
        private const val NOTIFICATION_ID = 2001
        private const val CHANNEL_ID = "summary_channel"
        
        fun enqueue(context: Context, meetingId: Long) {
            val workRequest = OneTimeWorkRequestBuilder<SummaryWorker>()
                .setInputData(workDataOf(KEY_MEETING_ID to meetingId))
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
                .setExpedited(OutOfQuotaPolicy.RUN_AS_NON_EXPEDITED_WORK_REQUEST)
                .addTag("generate_summary_$meetingId")
                .build()
            
            WorkManager.getInstance(context).enqueue(workRequest)
            Log.d(TAG, "Enqueued summary work for meeting: $meetingId")
        }
    }
    
    override suspend fun doWork(): Result {
        val meetingId = inputData.getLong(KEY_MEETING_ID, -1)
        if (meetingId == -1L) {
            Log.e(TAG, "Invalid meeting ID")
            return Result.failure()
        }
        
        return try {
            // Set foreground for long-running operation
            setForeground(createForegroundInfo())
            
            Log.d(TAG, "Generating summary for meeting: $meetingId")
            
            // Get all transcript segments
            val transcriptSegments = transcriptRepository.getTranscriptByMeetingIdSync(meetingId)
            if (transcriptSegments.isEmpty()) {
                Log.w(TAG, "No transcript segments found for meeting: $meetingId")
                return Result.failure()
            }
            
            // Combine transcript
            val fullTranscript = transcriptSegments.joinToString(" ") { it.text }
            
            // Create or get summary entity
            var summary = summaryRepository.getSummaryByMeetingIdSync(meetingId)
            if (summary == null) {
                val summaryId = summaryRepository.createOrUpdateSummary(
                    Summary(
                        meetingId = meetingId,
                        status = SummaryStatus.GENERATING
                    )
                )
                summary = summaryRepository.getSummaryByMeetingIdSync(meetingId)
            } else {
                summaryRepository.updateSummaryStatus(meetingId, SummaryStatus.GENERATING)
            }
            
            // Generate summary with streaming updates
            val actionItems = mutableListOf<String>()
            val keyPoints = mutableListOf<String>()
            val summaryTextBuilder = StringBuilder()
            
            summaryApi.generateSummary(fullTranscript)
                .onEach { update ->
                    when (update.type) {
                        SummaryUpdateType.TITLE -> {
                            summaryRepository.updateSummaryTitle(meetingId, update.content)
                            Log.d(TAG, "Title updated: ${update.content}")
                        }
                        SummaryUpdateType.SUMMARY_TEXT -> {
                            summaryTextBuilder.append(update.content)
                            summaryRepository.updateSummaryText(meetingId, summaryTextBuilder.toString())
                            Log.d(TAG, "Summary text updated")
                        }
                        SummaryUpdateType.ACTION_ITEM -> {
                            actionItems.add(update.content)
                            summaryRepository.updateActionItems(
                                meetingId,
                                Gson().toJson(actionItems)
                            )
                            Log.d(TAG, "Action item added: ${update.content}")
                        }
                        SummaryUpdateType.KEY_POINT -> {
                            keyPoints.add(update.content)
                            summaryRepository.updateKeyPoints(
                                meetingId,
                                Gson().toJson(keyPoints)
                            )
                            Log.d(TAG, "Key point added: ${update.content}")
                        }
                        SummaryUpdateType.COMPLETE -> {
                            summaryRepository.updateSummaryStatus(meetingId, SummaryStatus.COMPLETED)
                            meetingRepository.updateMeetingStatus(meetingId, MeetingStatus.COMPLETED)
                            Log.d(TAG, "Summary generation completed")
                        }
                    }
                }
                .catch { e ->
                    Log.e(TAG, "Error generating summary", e)
                    summaryRepository.updateSummaryStatus(meetingId, SummaryStatus.FAILED)
                    throw e
                }
                .collect()
            
            Log.d(TAG, "Summary generated successfully for meeting: $meetingId")
            Result.success()
        } catch (e: Exception) {
            Log.e(TAG, "Error generating summary for meeting: $meetingId", e)
            summaryRepository.updateSummaryStatus(meetingId, SummaryStatus.FAILED)
            Result.retry()
        }
    }
    
    private fun createForegroundInfo(): ForegroundInfo {
        createNotificationChannel()
        
        val notification = NotificationCompat.Builder(applicationContext, CHANNEL_ID)
            .setContentTitle("Generating Summary")
            .setContentText("Processing meeting transcript...")
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setOngoing(true)
            .build()
        
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            ForegroundInfo(
                NOTIFICATION_ID,
                notification,
                ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC
            )
        } else {
            ForegroundInfo(NOTIFICATION_ID, notification)
        }
    }
    
    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Summary Generation",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Shows progress of summary generation"
            }
            
            val notificationManager = 
                applicationContext.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }
}
