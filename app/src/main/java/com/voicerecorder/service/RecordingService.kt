package com.voicerecorder.service

import android.Manifest
import android.app.*
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.content.pm.ServiceInfo
import android.media.AudioManager
import android.os.Binder
import android.os.Build
import android.os.IBinder
import android.telephony.PhoneStateListener
import android.telephony.TelephonyCallback
import android.telephony.TelephonyManager
import android.util.Log
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.work.ExistingWorkPolicy
import androidx.work.WorkManager
import com.voicerecorder.R
import com.voicerecorder.audio.AudioRecordWrapper
import com.voicerecorder.audio.WavWriter
import com.voicerecorder.data.repository.*
import com.voicerecorder.data.room.*
import com.voicerecorder.util.AudioFocusHelper
import com.voicerecorder.util.SilenceDetector
import com.voicerecorder.util.StorageChecker
import com.voicerecorder.workers.FinalizeChunkWorker
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.first
import java.io.File
import javax.inject.Inject

@AndroidEntryPoint
class RecordingService : Service() {

    companion object {
        private const val TAG = "RecordingService"
        private const val NOTIFICATION_ID = 1001
        private const val CHANNEL_ID = "recording_channel"
        private const val CHUNK_DURATION_MS = 30000L // 30 seconds
        private const val OVERLAP_DURATION_MS = 2000L // 2 seconds
        private const val BUFFER_SIZE = 1024

        const val ACTION_START = "com.voicerecorder.action.START"
        const val ACTION_PAUSE = "com.voicerecorder.action.PAUSE"
        const val ACTION_RESUME = "com.voicerecorder.action.RESUME"
        const val ACTION_STOP = "com.voicerecorder.action.STOP"
        
        const val EXTRA_MEETING_ID = "meeting_id"
    }

    @Inject lateinit var meetingRepository: MeetingRepository
    @Inject lateinit var chunkRepository: ChunkRepository
    @Inject lateinit var sessionStateRepository: SessionStateRepository
    @Inject lateinit var storageChecker: StorageChecker

    private val binder = LocalBinder()
    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    private var audioRecord: AudioRecordWrapper? = null
    private var wavWriter: WavWriter? = null
    private var silenceDetector: SilenceDetector? = null
    private var audioFocusHelper: AudioFocusHelper? = null
    
    private var currentMeetingId: Long? = null
    private var currentChunkNumber = 0
    private var recordingJob: Job? = null
    private var timerJob: Job? = null
    
    private var isRecording = false
    private var isPaused = false
    private var recordingStartTime = 0L
    private var chunkStartTime = 0L
    private var totalPausedDuration = 0L
    private var pauseStartTime = 0L
    
    private var statusMessage = "Recording"
    private var currentChunkFile: File? = null
    
    private var telephonyManager: TelephonyManager? = null
    private var phoneStateListener: Any? = null
    private var headsetReceiver: BroadcastReceiver? = null

    inner class LocalBinder : Binder() {
        fun getService(): RecordingService = this@RecordingService
    }

    override fun onBind(intent: Intent?): IBinder = binder

    override fun onCreate() {
        super.onCreate()
        Log.d(TAG, "Service created")
        createNotificationChannel()
        setupAudioFocusHelper()
        setupPhoneStateListener()
        setupHeadsetReceiver()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_START -> {
                val meetingId = intent.getLongExtra(EXTRA_MEETING_ID, -1L)
                if (meetingId != -1L) {
                    startRecording(meetingId)
                }
            }
            ACTION_PAUSE -> pauseRecording()
            ACTION_RESUME -> resumeRecording()
            ACTION_STOP -> stopRecording()
        }
        return START_STICKY
    }

    private fun startRecording(meetingId: Long) {
        if (isRecording) {
            Log.w(TAG, "Already recording")
            return
        }

        // Check storage
        if (!storageChecker.hasEnoughStorage()) {
            statusMessage = getString(R.string.recording_stopped_low_storage)
            showNotification()
            stopSelf()
            return
        }

        // Check permissions
        if (ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.RECORD_AUDIO
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            Log.e(TAG, "Missing RECORD_AUDIO permission")
            stopSelf()
            return
        }

        currentMeetingId = meetingId
        currentChunkNumber = 0
        recordingStartTime = System.currentTimeMillis()
        totalPausedDuration = 0
        statusMessage = getString(R.string.recording)
        
        // Request audio focus
        if (audioFocusHelper?.requestAudioFocus() != true) {
            Log.e(TAG, "Failed to get audio focus")
            return
        }

        // Start foreground service
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            startForeground(
                NOTIFICATION_ID,
                createNotification(),
                ServiceInfo.FOREGROUND_SERVICE_TYPE_MICROPHONE
            )
        } else {
            startForeground(NOTIFICATION_ID, createNotification())
        }

        isRecording = true
        isPaused = false

        // Save session state
        saveSessionState()

        // Start recording job
        startRecordingJob()
        startTimerJob()
    }

    private fun startRecordingJob() {
        recordingJob?.cancel()
        recordingJob = serviceScope.launch {
            try {
                startNewChunk()
            } catch (e: Exception) {
                Log.e(TAG, "Recording error", e)
                stopRecording()
            }
        }
    }

    private suspend fun startNewChunk() {
        if (!isRecording || isPaused) return

        chunkStartTime = System.currentTimeMillis()
        
        // Create chunk file
        val chunksDir = File(getExternalFilesDir(null), "chunks")
        if (!chunksDir.exists()) {
            chunksDir.mkdirs()
        }
        
        currentChunkFile = File(chunksDir, "meeting_${currentMeetingId}_chunk_${currentChunkNumber}.wav")
        
        // Initialize audio recording
        audioRecord = AudioRecordWrapper()
        wavWriter = WavWriter(
            currentChunkFile!!,
            audioRecord!!.getSampleRate(),
            audioRecord!!.getChannelCount(),
            audioRecord!!.getBitsPerSample()
        )
        silenceDetector = SilenceDetector()
        
        if (!audioRecord!!.startRecording()) {
            Log.e(TAG, "Failed to start audio recording")
            stopRecording()
            return
        }
        
        wavWriter!!.start()
        
        // Save chunk to database
        val chunk = Chunk(
            meetingId = currentMeetingId!!,
            chunkNumber = currentChunkNumber,
            filePath = currentChunkFile!!.absolutePath,
            startTime = chunkStartTime,
            endTime = 0,
            duration = 0,
            status = ChunkStatus.RECORDING
        )
        val chunkId = chunkRepository.createChunk(chunk)
        
        // Record audio
        val buffer = ShortArray(BUFFER_SIZE)
        val chunkDurationMs = CHUNK_DURATION_MS
        val overlapMs = OVERLAP_DURATION_MS
        var lastChunkData: ShortArray? = null
        
        while (isRecording && !isPaused) {
            val elapsed = System.currentTimeMillis() - chunkStartTime
            
            if (elapsed >= chunkDurationMs) {
                // Save overlap data for next chunk
                lastChunkData = buffer.copyOf()
                
                // Finalize current chunk
                finalizeChunk(chunkId)
                
                // Start next chunk
                currentChunkNumber++
                startNewChunk()
                
                // Write overlap from previous chunk
                if (lastChunkData != null) {
                    wavWriter?.write(lastChunkData, lastChunkData.size)
                }
                
                break
            }
            
            val read = audioRecord?.read(buffer) ?: -1
            if (read > 0) {
                wavWriter?.write(buffer, read)
                
                // Check for silence
                if (silenceDetector?.detectSilence(buffer, audioRecord!!.getSampleRate()) == true) {
                    statusMessage = getString(R.string.no_audio_detected)
                    showNotification()
                }
                
                // Check storage periodically
                if (elapsed % 5000 < 100) { // Check every 5 seconds
                    if (!storageChecker.hasEnoughStorage()) {
                        statusMessage = getString(R.string.recording_stopped_low_storage)
                        showNotification()
                        stopRecording()
                        break
                    }
                }
            }
        }
    }

    private suspend fun finalizeChunk(chunkId: Long) {
        wavWriter?.finalize()
        audioRecord?.stopRecording()
        
        val endTime = System.currentTimeMillis()
        val duration = endTime - chunkStartTime
        
        // Update chunk in database
        val chunk = chunkRepository.getChunkById(chunkId)
        if (chunk != null) {
            chunkRepository.updateChunk(
                chunk.copy(
                    endTime = endTime,
                    duration = duration,
                    status = ChunkStatus.FINALIZING
                )
            )
        }
        
        // Enqueue finalization worker
        val work = FinalizeChunkWorker.createWorkRequest(chunkId, currentMeetingId!!)
        WorkManager.getInstance(applicationContext)
            .enqueueUniqueWork(
                "finalize_$chunkId",
                ExistingWorkPolicy.KEEP,
                work
            )
    }

    private fun pauseRecording() {
        if (!isRecording || isPaused) return
        
        isPaused = true
        pauseStartTime = System.currentTimeMillis()
        
        serviceScope.launch {
            // Finalize current chunk
            if (currentChunkNumber >= 0) {
                val chunks = chunkRepository.getChunksForMeeting(currentMeetingId!!).first()
                val currentChunk = chunks.lastOrNull()
                if (currentChunk != null && currentChunk.status == ChunkStatus.RECORDING) {
                    finalizeChunk(currentChunk.id)
                }
            }
        }
        
        audioRecord?.stopRecording()
        wavWriter?.finalize()
        
        saveSessionState()
        showNotification()
    }

    private fun resumeRecording() {
        if (!isRecording || !isPaused) return
        
        isPaused = false
        totalPausedDuration += System.currentTimeMillis() - pauseStartTime
        statusMessage = getString(R.string.recording)
        
        silenceDetector?.reset()
        saveSessionState()
        showNotification()
        
        // Restart recording
        currentChunkNumber++
        startRecordingJob()
    }

    private fun stopRecording() {
        if (!isRecording) return
        
        isRecording = false
        isPaused = false
        
        recordingJob?.cancel()
        timerJob?.cancel()
        
        serviceScope.launch {
            // Finalize current chunk
            if (currentMeetingId != null && currentChunkNumber >= 0) {
                val chunks = chunkRepository.getChunksForMeeting(currentMeetingId!!).first()
                val currentChunk = chunks.lastOrNull()
                if (currentChunk != null && currentChunk.status == ChunkStatus.RECORDING) {
                    finalizeChunk(currentChunk.id)
                }
            }
            
            // Update meeting
            if (currentMeetingId != null) {
                val endTime = System.currentTimeMillis()
                val duration = endTime - recordingStartTime - totalPausedDuration
                meetingRepository.updateEndTime(currentMeetingId!!, endTime, duration)
                meetingRepository.updateStatus(currentMeetingId!!, MeetingStatus.PROCESSING)
            }
            
            // Clear session state
            sessionStateRepository.clearSessionState()
        }
        
        audioRecord?.stopRecording()
        wavWriter?.finalize()
        audioFocusHelper?.abandonAudioFocus()
        
        statusMessage = getString(R.string.stopped)
        showNotification()
        
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    private fun startTimerJob() {
        timerJob?.cancel()
        timerJob = serviceScope.launch {
            while (isRecording) {
                delay(1000)
                if (!isPaused) {
                    showNotification()
                }
            }
        }
    }

    private fun saveSessionState() {
        serviceScope.launch {
            val state = SessionState(
                meetingId = currentMeetingId,
                isRecording = isRecording,
                isPaused = isPaused,
                currentChunkNumber = currentChunkNumber,
                recordingStartTime = recordingStartTime,
                pauseTime = if (isPaused) pauseStartTime else 0,
                totalPausedDuration = totalPausedDuration,
                lastChunkPath = currentChunkFile?.absolutePath,
                statusMessage = statusMessage,
                needsRecovery = true
            )
            sessionStateRepository.saveSessionState(state)
        }
    }

    private fun setupAudioFocusHelper() {
        audioFocusHelper = AudioFocusHelper(this) { focusChange ->
            when (focusChange) {
                AudioManager.AUDIOFOCUS_LOSS, AudioManager.AUDIOFOCUS_LOSS_TRANSIENT -> {
                    if (isRecording && !isPaused) {
                        statusMessage = getString(R.string.paused_audio_focus)
                        pauseRecording()
                    }
                }
                AudioManager.AUDIOFOCUS_GAIN -> {
                    // User can manually resume
                }
            }
        }
    }

    private fun setupPhoneStateListener() {
        telephonyManager = getSystemService(Context.TELEPHONY_SERVICE) as TelephonyManager
        
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            if (ActivityCompat.checkSelfPermission(
                    this,
                    Manifest.permission.READ_PHONE_STATE
                ) == PackageManager.PERMISSION_GRANTED
            ) {
                val callback = object : TelephonyCallback(), TelephonyCallback.CallStateListener {
                    override fun onCallStateChanged(state: Int) {
                        handleCallStateChange(state)
                    }
                }
                phoneStateListener = callback
                telephonyManager?.registerTelephonyCallback(mainExecutor, callback)
            }
        } else {
            @Suppress("DEPRECATION")
            val listener = object : PhoneStateListener() {
                @Deprecated("Deprecated in API 31")
                override fun onCallStateChanged(state: Int, phoneNumber: String?) {
                    handleCallStateChange(state)
                }
            }
            phoneStateListener = listener
            @Suppress("DEPRECATION")
            telephonyManager?.listen(listener, PhoneStateListener.LISTEN_CALL_STATE)
        }
    }

    private fun handleCallStateChange(state: Int) {
        when (state) {
            TelephonyManager.CALL_STATE_RINGING, TelephonyManager.CALL_STATE_OFFHOOK -> {
                if (isRecording && !isPaused) {
                    statusMessage = getString(R.string.paused_phone_call)
                    pauseRecording()
                }
            }
            TelephonyManager.CALL_STATE_IDLE -> {
                // User can manually resume
            }
        }
    }

    private fun setupHeadsetReceiver() {
        headsetReceiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context?, intent: Intent?) {
                when (intent?.action) {
                    Intent.ACTION_HEADSET_PLUG,
                    AudioManager.ACTION_AUDIO_BECOMING_NOISY -> {
                        // Show notification about headset change
                        val notification = NotificationCompat.Builder(this@RecordingService, CHANNEL_ID)
                            .setContentTitle(getString(R.string.app_name))
                            .setContentText(getString(R.string.headset_changed))
                            .setSmallIcon(android.R.drawable.ic_dialog_info)
                            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                            .build()
                        
                        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
                        notificationManager.notify(1002, notification)
                    }
                }
            }
        }
        
        val filter = IntentFilter().apply {
            addAction(Intent.ACTION_HEADSET_PLUG)
            addAction(AudioManager.ACTION_AUDIO_BECOMING_NOISY)
        }
        registerReceiver(headsetReceiver, filter)
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                getString(R.string.recording_notification_channel_name),
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = getString(R.string.recording_notification_channel_desc)
                setShowBadge(false)
                lockscreenVisibility = Notification.VISIBILITY_PUBLIC
            }
            
            val notificationManager = getSystemService(NotificationManager::class.java)
            notificationManager?.createNotificationChannel(channel)
        }
    }

    private fun createNotification(): Notification {
        val elapsed = if (isRecording && !isPaused) {
            System.currentTimeMillis() - recordingStartTime - totalPausedDuration
        } else {
            0
        }
        
        val timeText = formatElapsedTime(elapsed)
        
        // Create pending intents for actions
        val pauseIntent = Intent(this, RecordingService::class.java).apply {
            action = ACTION_PAUSE
        }
        val pausePendingIntent = PendingIntent.getService(
            this, 0, pauseIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        
        val resumeIntent = Intent(this, RecordingService::class.java).apply {
            action = ACTION_RESUME
        }
        val resumePendingIntent = PendingIntent.getService(
            this, 0, resumeIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        
        val stopIntent = Intent(this, RecordingService::class.java).apply {
            action = ACTION_STOP
        }
        val stopPendingIntent = PendingIntent.getService(
            this, 0, stopIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        
        val builder = NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle(getString(R.string.recording_notification_title))
            .setContentText("$statusMessage - $timeText")
            .setSmallIcon(android.R.drawable.ic_btn_speak_now)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setOngoing(true)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
        
        if (isPaused) {
            builder.addAction(
                android.R.drawable.ic_media_play,
                getString(R.string.action_resume),
                resumePendingIntent
            )
        } else if (isRecording) {
            builder.addAction(
                android.R.drawable.ic_media_pause,
                getString(R.string.action_pause),
                pausePendingIntent
            )
        }
        
        builder.addAction(
            android.R.drawable.ic_delete,
            getString(R.string.action_stop),
            stopPendingIntent
        )
        
        return builder.build()
    }

    private fun showNotification() {
        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.notify(NOTIFICATION_ID, createNotification())
    }

    private fun formatElapsedTime(millis: Long): String {
        val seconds = (millis / 1000) % 60
        val minutes = (millis / (1000 * 60)) % 60
        val hours = millis / (1000 * 60 * 60)
        
        return if (hours > 0) {
            String.format("%02d:%02d:%02d", hours, minutes, seconds)
        } else {
            String.format("%02d:%02d", minutes, seconds)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        Log.d(TAG, "Service destroyed")
        
        recordingJob?.cancel()
        timerJob?.cancel()
        serviceScope.cancel()
        
        audioRecord?.stopRecording()
        wavWriter?.finalize()
        audioFocusHelper?.abandonAudioFocus()
        
        // Unregister receivers
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                (phoneStateListener as? TelephonyCallback)?.let {
                    telephonyManager?.unregisterTelephonyCallback(it)
                }
            } else {
                @Suppress("DEPRECATION")
                (phoneStateListener as? PhoneStateListener)?.let {
                    telephonyManager?.listen(it, PhoneStateListener.LISTEN_NONE)
                }
            }
            headsetReceiver?.let { unregisterReceiver(it) }
        } catch (e: Exception) {
            Log.e(TAG, "Error unregistering receivers", e)
        }
    }
}
