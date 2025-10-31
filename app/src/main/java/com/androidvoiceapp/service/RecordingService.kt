package com.androidvoiceapp.service

import android.Manifest
import android.app.*
import android.bluetooth.BluetoothHeadset
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.content.pm.ServiceInfo
import android.media.AudioManager
import android.os.Build
import android.os.IBinder
import android.telephony.PhoneStateListener
import android.telephony.TelephonyCallback
import android.telephony.TelephonyManager
import android.util.Log
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.work.*
import com.androidvoiceapp.R
import com.androidvoiceapp.audio.AudioRecordWrapper
import com.androidvoiceapp.audio.WavWriter
import com.androidvoiceapp.data.repository.*
import com.androidvoiceapp.data.room.SessionStateEntity
import com.androidvoiceapp.util.AudioFocusHelper
import com.androidvoiceapp.util.StorageChecker
import com.androidvoiceapp.workers.FinalizeChunkWorker
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.*
import java.io.File
import javax.inject.Inject

@AndroidEntryPoint
class RecordingService : Service() {

    companion object {
        private const val TAG = "RecordingService"
        private const val NOTIFICATION_ID = 1001
        private const val CHANNEL_ID = "recording_channel"

        const val ACTION_START = "action_start"
        const val ACTION_PAUSE = "action_pause"
        const val ACTION_RESUME = "action_resume"
        const val ACTION_STOP = "action_stop"

        const val EXTRA_MEETING_ID = "meeting_id"
        const val EXTRA_MEETING_TITLE = "meeting_title"

        private const val CHUNK_DURATION_MS = 30000L // 30 seconds
        private const val OVERLAP_DURATION_MS = 2000L // 2 seconds
        private const val SAMPLE_RATE = 16000

        private const val REQUEST_CODE_PAUSE = 1001
        private const val REQUEST_CODE_RESUME = 1002
        private const val REQUEST_CODE_STOP = 1003
    }

    @Inject
    lateinit var meetingRepository: MeetingRepository

    @Inject
    lateinit var chunkRepository: ChunkRepository

    @Inject
    lateinit var sessionStateRepository: SessionStateRepository

    @Inject
    lateinit var storageChecker: StorageChecker

    @Inject
    lateinit var workManager: WorkManager

    private val serviceScope = CoroutineScope(Dispatchers.Default + SupervisorJob())

    private var audioRecorder: AudioRecordWrapper? = null
    private var wavWriter: WavWriter? = null
    private var audioFocusHelper: AudioFocusHelper? = null

    private var meetingId: Long = -1
    private var currentChunkSequence = 0
    private var isRecording = false
    private var isPaused = false
    private var recordingStartTime = 0L
    private var pausedDuration = 0L
    private var lastPauseTime = 0L

    private var currentStatus = "Recording..."
    private var currentChunkFile: File? = null
    private var currentChunkStartTime = 0L

    private var telephonyManager: TelephonyManager? = null
    private var phoneStateListener: Any? = null
    private var headsetReceiver: BroadcastReceiver? = null
    private var isInPhoneCall = false

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
        setupPhoneStateListener()
        setupHeadsetListener()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_START -> handleStart(intent)
            ACTION_PAUSE -> handlePause()
            ACTION_RESUME -> handleResume()
            ACTION_STOP -> handleStop()
        }
        return START_STICKY
    }

    private fun handleStart(intent: Intent) {
        val title = intent.getStringExtra(EXTRA_MEETING_TITLE) ?: "Meeting"

        serviceScope.launch {
            if (!storageChecker.hasEnoughStorage()) {
                Log.e(TAG, "Not enough storage to start recording.")
                stopSelf()
                return@launch
            }

            try {
                val newMeetingId = meetingRepository.createMeeting(title)
                meetingId = newMeetingId
                Log.d(TAG, "Created meeting in database with ID: $meetingId")

                saveSessionState()
                startForeground()
                startRecording()

            } catch (e: Exception) {
                Log.e(TAG, "Failed to create meeting and start recording", e)
                stopSelf()
            }
        }
    }

    private fun handlePause() {
        if (isRecording && !isPaused) {
            pauseRecording()
        }
    }

    private fun handleResume() {
        if (isRecording && isPaused) {
            resumeRecording()
        }
    }

    private fun handleStop() {
        serviceScope.launch {
            stopRecording()
            stopSelf()
        }
    }

    private fun startForeground() {
        val notification = createNotification()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            startForeground(
                NOTIFICATION_ID,
                notification,
                ServiceInfo.FOREGROUND_SERVICE_TYPE_MICROPHONE
            )
        } else {
            startForeground(NOTIFICATION_ID, notification)
        }
    }

    private fun startRecording() {
        if (meetingId == -1L) {
            Log.e(TAG, "Cannot start recording, meetingId is invalid.")
            return
        }

        try {
            audioFocusHelper = AudioFocusHelper(this) { gained ->
                if (gained) {
                    if (isPaused && !isInPhoneCall) handleResume()
                } else {
                    currentStatus = getString(R.string.status_paused_audio_focus)
                    handlePause()
                }
            }

            if (!audioFocusHelper!!.requestAudioFocus()) {
                Log.e(TAG, "Failed to gain audio focus")
                return
            }

            audioRecorder = AudioRecordWrapper()
            if (!audioRecorder!!.start()) {
                Log.e(TAG, "Failed to start AudioRecord")
                return
            }

            isRecording = true
            isPaused = false
            recordingStartTime = System.currentTimeMillis()
            currentStatus = getString(R.string.status_recording)

            serviceScope.launch { recordingLoop() }
            serviceScope.launch { updateTimer() }

            updateNotification()

        } catch (e: Exception) {
            Log.e(TAG, "Exception in startRecording", e)
        }
    }

    private suspend fun recordingLoop() {
        while (isRecording) {
            if (isPaused) {
                delay(100)
                continue
            }

            try {
                if (!storageChecker.hasEnoughStorage()) {
                    currentStatus = getString(R.string.status_low_storage)
                    withContext(Dispatchers.Main) { updateNotification() }
                    stopRecording()
                    break
                }

                startNewChunk()

                val chunkStartTime = System.currentTimeMillis()
                val buffer = ShortArray(audioRecorder!!.getBufferSize())

                while (isRecording && !isPaused) {
                    val elapsed = System.currentTimeMillis() - chunkStartTime
                    if (elapsed >= CHUNK_DURATION_MS) break

                    val samplesRead = audioRecorder!!.read(buffer)
                    if (samplesRead > 0) {
                        wavWriter?.write(buffer, samplesRead)
                    }

                    delay(10) // Prevent busy loop
                }

                finalizeCurrentChunk()

                if (isRecording && !isPaused) {
                    delay(OVERLAP_DURATION_MS)
                }

            } catch (e: Exception) {
                Log.e(TAG, "Error in recording loop", e)
                break
            }
        }
    }

    private suspend fun startNewChunk() {
        val audioDir = File(getExternalFilesDir(null), "audio")
        audioDir.mkdirs()

        currentChunkFile = File(audioDir, "meeting_${meetingId}_chunk_${currentChunkSequence}.wav.tmp")
        currentChunkStartTime = System.currentTimeMillis() - recordingStartTime - pausedDuration

        wavWriter = WavWriter(currentChunkFile!!, SAMPLE_RATE)
        wavWriter?.open()
    }

    private suspend fun finalizeCurrentChunk() {
        if (wavWriter == null || currentChunkFile == null) return

        try {
            wavWriter?.close()
            wavWriter = null

            val finalFile = File(currentChunkFile!!.absolutePath.removeSuffix(".tmp"))
            currentChunkFile?.renameTo(finalFile)

            val chunkId = chunkRepository.createChunk(
                meetingId = meetingId,
                sequenceNumber = currentChunkSequence,
                filePath = finalFile.absolutePath,
                duration = CHUNK_DURATION_MS,
                startTime = currentChunkStartTime
            )

            // **CRITICAL FIX: Pass both meetingId and chunkId to the worker**
            val workRequest = OneTimeWorkRequestBuilder<FinalizeChunkWorker>()
                .setInputData(
                    workDataOf(
                        "meeting_id" to meetingId,
                        "chunk_id" to chunkId
                    )
                )
                .build()

            workManager.enqueueUniqueWork(
                "finalize_chunk_${chunkId}",
                ExistingWorkPolicy.KEEP,
                workRequest
            )

            currentChunkSequence++
            saveSessionState()

        } catch (e: Exception) {
            Log.e(TAG, "Failed to finalize chunk", e)
        }
    }

    private fun pauseRecording() {
        isPaused = true
        lastPauseTime = System.currentTimeMillis()
        serviceScope.launch { saveSessionState() }
        updateNotification()
    }

    private fun resumeRecording() {
        if (isPaused) {
            isPaused = false
            pausedDuration += System.currentTimeMillis() - lastPauseTime
            currentStatus = getString(R.string.status_recording)
            serviceScope.launch { saveSessionState() }
            updateNotification()
        }
    }

    private suspend fun stopRecording() {
        isRecording = false
        isPaused = false

        try {
            finalizeCurrentChunk()

            audioRecorder?.stop()
            audioRecorder?.release()
            audioRecorder = null

            audioFocusHelper?.abandonAudioFocus()
            audioFocusHelper = null

            meetingRepository.endMeeting(meetingId)
            sessionStateRepository.deleteSessionState(meetingId)

            currentStatus = getString(R.string.status_stopped)

        } catch (e: Exception) {
            Log.e(TAG, "Error stopping recording", e)
        }
    }

    private suspend fun saveSessionState() {
        if (meetingId == -1L) return
        try {
            val state = SessionStateEntity(
                meetingId = meetingId,
                isRecording = isRecording,
                isPaused = isPaused,
                currentChunkSequence = currentChunkSequence,
                currentChunkPath = currentChunkFile?.absolutePath,
                recordingStartTime = recordingStartTime,
                pausedTime = if (isPaused) lastPauseTime else null,
                status = currentStatus
            )
            sessionStateRepository.saveSessionState(state)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to save session state", e)
        }
    }

    private suspend fun updateTimer() {
        while (isRecording) {
            delay(1000)
            if (!isPaused) {
                updateNotification()
            }
        }
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                getString(R.string.notification_channel_name),
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = getString(R.string.notification_channel_description)
                setSound(null, null)
            }
            getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
        }
    }

    private fun createNotification(): Notification {
        val elapsedTime = if (isRecording) (System.currentTimeMillis() - recordingStartTime - pausedDuration) / 1000 else 0
        val minutes = elapsedTime / 60
        val seconds = elapsedTime % 60
        val timerText = String.format("%02d:%02d", minutes, seconds)

        val builder = NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle(getString(R.string.notification_title_recording))
            .setContentText("$currentStatus - $timerText")
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)

        if (isPaused) {
            val resumeIntent = Intent(this, RecordingService::class.java).apply { action = ACTION_RESUME }
            val resumePendingIntent = PendingIntent.getService(this, REQUEST_CODE_RESUME, resumeIntent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
            builder.addAction(0, getString(R.string.notification_action_resume), resumePendingIntent)
        } else if (isRecording) {
            val pauseIntent = Intent(this, RecordingService::class.java).apply { action = ACTION_PAUSE }
            val pausePendingIntent = PendingIntent.getService(this, REQUEST_CODE_PAUSE, pauseIntent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
            builder.addAction(0, getString(R.string.notification_action_pause), pausePendingIntent)
        }

        val stopIntent = Intent(this, RecordingService::class.java).apply { action = ACTION_STOP }
        val stopPendingIntent = PendingIntent.getService(this, REQUEST_CODE_STOP, stopIntent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
        builder.addAction(0, getString(R.string.notification_action_stop), stopPendingIntent)

        return builder.build()
    }

    private fun updateNotification() {
        getSystemService(NotificationManager::class.java).notify(NOTIFICATION_ID, createNotification())
    }

    private fun setupPhoneStateListener() {
        telephonyManager = getSystemService(Context.TELEPHONY_SERVICE) as TelephonyManager
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.READ_PHONE_STATE) != PackageManager.PERMISSION_GRANTED) return

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val callback = object : TelephonyCallback(), TelephonyCallback.CallStateListener {
                override fun onCallStateChanged(state: Int) = handlePhoneStateChange(state)
            }
            phoneStateListener = callback
            telephonyManager?.registerTelephonyCallback(mainExecutor, callback)
        } else {
            @Suppress("DEPRECATION")
            val listener = object : PhoneStateListener() {
                @Deprecated("Deprecated in Java")
                override fun onCallStateChanged(state: Int, phoneNumber: String?) = handlePhoneStateChange(state)
            }
            phoneStateListener = listener
            telephonyManager?.listen(listener, PhoneStateListener.LISTEN_CALL_STATE)
        }
    }

    private fun handlePhoneStateChange(state: Int) {
        when (state) {
            TelephonyManager.CALL_STATE_RINGING, TelephonyManager.CALL_STATE_OFFHOOK -> {
                if (isRecording && !isPaused) {
                    isInPhoneCall = true
                    currentStatus = getString(R.string.status_paused_phone_call)
                    handlePause()
                }
            }
            TelephonyManager.CALL_STATE_IDLE -> {
                if (isInPhoneCall) {
                    isInPhoneCall = false
                    if (isPaused) handleResume()
                }
            }
        }
    }

    private fun setupHeadsetListener() {
        val filter = IntentFilter().apply {
            addAction(AudioManager.ACTION_HEADSET_PLUG)
            addAction(AudioManager.ACTION_SCO_AUDIO_STATE_UPDATED)
            addAction(BluetoothHeadset.ACTION_CONNECTION_STATE_CHANGED)
        }

        headsetReceiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context?, intent: Intent?) {
                Log.d(TAG, "Headset change detected: ${intent?.action}")
                showHeadsetChangeNotification()
            }
        }

        registerReceiver(headsetReceiver, filter)
    }

    private fun showHeadsetChangeNotification() {
        Log.d(TAG, getString(R.string.notification_headset_changed))
    }

    override fun onDestroy() {
        super.onDestroy()

        serviceScope.launch {
            if (isRecording && currentChunkFile != null) {
                finalizeCurrentChunk()
            }
        }

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                (phoneStateListener as? TelephonyCallback)?.let { telephonyManager?.unregisterTelephonyCallback(it) }
            } else {
                @Suppress("DEPRECATION")
                (phoneStateListener as? PhoneStateListener)?.let { telephonyManager?.listen(it, PhoneStateListener.LISTEN_NONE) }
            }
            headsetReceiver?.let { unregisterReceiver(it) }
        } catch (e: Exception) {
            Log.e(TAG, "Error during cleanup", e)
        }

        serviceScope.cancel()
    }
}
