package com.abhay.voiceapp.service

import android.Manifest
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
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
import androidx.core.app.ServiceCompat
import com.abhay.voiceapp.R
import com.abhay.voiceapp.audio.AudioRecorder
import com.abhay.voiceapp.audio.ChunkManager
import com.abhay.voiceapp.audio.SilenceDetector
import com.abhay.voiceapp.data.entity.MeetingStatus
import com.abhay.voiceapp.data.entity.SessionState
import com.abhay.voiceapp.data.repository.MeetingRepository
import com.abhay.voiceapp.data.repository.SessionStateRepository
import com.abhay.voiceapp.ui.MainActivity
import com.abhay.voiceapp.util.StorageManager
import com.abhay.voiceapp.util.TimeFormatter
import com.abhay.voiceapp.worker.FinalizeChunkWorker
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
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
    }
    
    @Inject lateinit var audioRecorder: AudioRecorder
    @Inject lateinit var chunkManager: ChunkManager
    @Inject lateinit var silenceDetector: SilenceDetector
    @Inject lateinit var meetingRepository: MeetingRepository
    @Inject lateinit var sessionStateRepository: SessionStateRepository
    @Inject lateinit var storageManager: StorageManager
    
    private val binder = LocalBinder()
    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    
    private var recordingJob: Job? = null
    private var currentMeetingId: Long = -1
    private var startTime: Long = 0
    private var pausedTime: Long = 0
    private var totalPausedDuration: Long = 0
    
    private val _recordingState = MutableStateFlow<RecordingState>(RecordingState.Stopped)
    val recordingState: StateFlow<RecordingState> = _recordingState.asStateFlow()
    
    private val _statusMessage = MutableStateFlow("")
    val statusMessage: StateFlow<String> = _statusMessage.asStateFlow()
    
    private var audioManager: AudioManager? = null
    private var telephonyManager: TelephonyManager? = null
    private val audioFocusChangeListener = AudioManager.OnAudioFocusChangeListener { focusChange ->
        handleAudioFocusChange(focusChange)
    }
    
    inner class LocalBinder : Binder() {
        fun getService(): RecordingService = this@RecordingService
    }
    
    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
        audioManager = getSystemService(Context.AUDIO_SERVICE) as AudioManager
        telephonyManager = getSystemService(Context.TELEPHONY_SERVICE) as TelephonyManager
        setupPhoneStateListener()
    }
    
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_START -> {
                val meetingId = intent.getLongExtra(EXTRA_MEETING_ID, -1)
                val meetingTitle = intent.getStringExtra(EXTRA_MEETING_TITLE) ?: "Recording"
                startRecording(meetingId, meetingTitle)
            }
            ACTION_PAUSE -> pauseRecording()
            ACTION_RESUME -> resumeRecording()
            ACTION_STOP -> stopRecording()
        }
        return START_STICKY
    }
    
    override fun onBind(intent: Intent?): IBinder = binder
    
    private fun startRecording(meetingId: Long, meetingTitle: String) {
        if (_recordingState.value is RecordingState.Recording) {
            Log.w(TAG, "Already recording")
            return
        }
        
        // Check storage
        if (!storageManager.hasEnoughStorage()) {
            _statusMessage.value = getString(R.string.recording_stopped_low_storage)
            stopSelf()
            return
        }
        
        currentMeetingId = meetingId
        startTime = System.currentTimeMillis()
        totalPausedDuration = 0
        
        // Start foreground service
        val notification = createNotification(meetingTitle, "00:00", getString(R.string.recording))
        ServiceCompat.startForeground(
            this,
            NOTIFICATION_ID,
            notification,
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                ServiceInfo.FOREGROUND_SERVICE_TYPE_MICROPHONE
            } else {
                0
            }
        )
        
        // Request audio focus
        val result = audioManager?.requestAudioFocus(
            audioFocusChangeListener,
            AudioManager.STREAM_MUSIC,
            AudioManager.AUDIOFOCUS_GAIN
        )
        
        if (result != AudioManager.AUDIOFOCUS_REQUEST_GRANTED) {
            Log.w(TAG, "Audio focus not granted")
        }
        
        // Start recording
        chunkManager.startNewRecording(meetingId)
        silenceDetector.reset()
        
        if (audioRecorder.startRecording()) {
            _recordingState.value = RecordingState.Recording(meetingId)
            _statusMessage.value = getString(R.string.recording)
            startRecordingLoop()
            
            // Save session state
            serviceScope.launch {
                sessionStateRepository.saveSessionState(
                    SessionState(
                        meetingId = meetingId,
                        isRecording = true,
                        isPaused = false,
                        currentChunkIndex = 0,
                        currentChunkStartTime = startTime
                    )
                )
            }
        } else {
            _statusMessage.value = "Failed to start recording"
            stopSelf()
        }
    }
    
    private fun pauseRecording(reason: String = getString(R.string.paused)) {
        if (_recordingState.value !is RecordingState.Recording) return
        
        pausedTime = System.currentTimeMillis()
        _recordingState.value = RecordingState.Paused(currentMeetingId, reason)
        _statusMessage.value = reason
        
        recordingJob?.cancel()
        
        updateNotification("Paused", reason)
        
        serviceScope.launch {
            meetingRepository.updateMeetingStatus(currentMeetingId, MeetingStatus.PAUSED)
            sessionStateRepository.updateSessionState(
                SessionState(
                    meetingId = currentMeetingId,
                    isRecording = true,
                    isPaused = true,
                    currentChunkIndex = 0,
                    currentChunkStartTime = startTime,
                    pauseReason = reason
                )
            )
        }
    }
    
    private fun resumeRecording() {
        if (_recordingState.value !is RecordingState.Paused) return
        
        val pauseDuration = System.currentTimeMillis() - pausedTime
        totalPausedDuration += pauseDuration
        
        _recordingState.value = RecordingState.Recording(currentMeetingId)
        _statusMessage.value = getString(R.string.recording)
        
        startRecordingLoop()
        
        serviceScope.launch {
            meetingRepository.updateMeetingStatus(currentMeetingId, MeetingStatus.RECORDING)
            sessionStateRepository.updateSessionState(
                SessionState(
                    meetingId = currentMeetingId,
                    isRecording = true,
                    isPaused = false,
                    currentChunkIndex = 0,
                    currentChunkStartTime = startTime
                )
            )
        }
    }
    
    private fun stopRecording() {
        recordingJob?.cancel()
        audioRecorder.stopRecording()
        
        // Finalize any remaining chunk
        serviceScope.launch {
            chunkManager.finalizeFinalChunk()?.let { chunk ->
                FinalizeChunkWorker.enqueue(applicationContext, chunk.id)
            }
            
            val endTime = System.currentTimeMillis()
            meetingRepository.endMeeting(currentMeetingId, endTime)
            meetingRepository.updateMeetingStatus(currentMeetingId, MeetingStatus.STOPPED)
            sessionStateRepository.deleteSessionState(currentMeetingId)
        }
        
        audioManager?.abandonAudioFocus(audioFocusChangeListener)
        
        _recordingState.value = RecordingState.Stopped
        _statusMessage.value = getString(R.string.stopped)
        
        chunkManager.reset()
        silenceDetector.reset()
        
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
    }
    
    private fun startRecordingLoop() {
        recordingJob = serviceScope.launch {
            val buffer = ByteArray(audioRecorder.getBufferSize())
            var lastNotificationUpdate = System.currentTimeMillis()
            
            while (isActive && audioRecorder.isRecordingActive()) {
                val bytesRead = audioRecorder.readAudioData(buffer)
                
                if (bytesRead > 0) {
                    // Check for silence
                    if (silenceDetector.detectSilence(buffer, bytesRead)) {
                        _statusMessage.value = getString(R.string.no_audio_detected)
                        updateNotification(getElapsedTime(), getString(R.string.no_audio_detected))
                    }
                    
                    // Process audio data and create chunks
                    val chunk = chunkManager.processAudioData(buffer, bytesRead)
                    chunk?.let {
                        // Enqueue finalization worker
                        FinalizeChunkWorker.enqueue(applicationContext, it.id)
                    }
                    
                    // Update notification every second
                    val now = System.currentTimeMillis()
                    if (now - lastNotificationUpdate >= 1000) {
                        updateNotification(getElapsedTime(), _statusMessage.value)
                        lastNotificationUpdate = now
                    }
                    
                    // Check storage periodically
                    if (!storageManager.hasEnoughStorage()) {
                        _statusMessage.value = getString(R.string.recording_stopped_low_storage)
                        stopRecording()
                        break
                    }
                }
                
                delay(10) // Small delay to prevent tight loop
            }
        }
    }
    
    private fun getElapsedTime(): String {
        val elapsed = System.currentTimeMillis() - startTime - totalPausedDuration
        return TimeFormatter.formatDuration(elapsed)
    }
    
    private fun handleAudioFocusChange(focusChange: Int) {
        when (focusChange) {
            AudioManager.AUDIOFOCUS_LOSS,
            AudioManager.AUDIOFOCUS_LOSS_TRANSIENT -> {
                if (_recordingState.value is RecordingState.Recording) {
                    pauseRecording(getString(R.string.paused_audio_focus))
                }
            }
            AudioManager.AUDIOFOCUS_GAIN -> {
                if (_recordingState.value is RecordingState.Paused) {
                    // Auto-resume could be optional based on user preference
                    // For now, we keep it paused and let user resume manually
                }
            }
        }
    }
    
    private fun setupPhoneStateListener() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            // Use TelephonyCallback for Android 12+
            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.READ_PHONE_STATE) 
                == PackageManager.PERMISSION_GRANTED) {
                telephonyManager?.registerTelephonyCallback(
                    mainExecutor,
                    object : TelephonyCallback(), TelephonyCallback.CallStateListener {
                        override fun onCallStateChanged(state: Int) {
                            handleCallStateChange(state)
                        }
                    }
                )
            }
        } else {
            @Suppress("DEPRECATION")
            telephonyManager?.listen(object : PhoneStateListener() {
                @Deprecated("Deprecated in Java")
                override fun onCallStateChanged(state: Int, phoneNumber: String?) {
                    handleCallStateChange(state)
                }
            }, PhoneStateListener.LISTEN_CALL_STATE)
        }
    }
    
    private fun handleCallStateChange(state: Int) {
        when (state) {
            TelephonyManager.CALL_STATE_RINGING,
            TelephonyManager.CALL_STATE_OFFHOOK -> {
                if (_recordingState.value is RecordingState.Recording) {
                    pauseRecording(getString(R.string.paused_phone_call))
                }
            }
            TelephonyManager.CALL_STATE_IDLE -> {
                // Call ended - user can manually resume
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
                setShowBadge(false)
                lockscreenVisibility = Notification.VISIBILITY_PUBLIC
            }
            
            val notificationManager = getSystemService(NotificationManager::class.java)
            notificationManager.createNotificationChannel(channel)
        }
    }
    
    private fun createNotification(title: String, time: String, status: String): Notification {
        val intent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP
        }
        val pendingIntent = PendingIntent.getActivity(
            this, 0, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        
        val pauseIntent = Intent(this, RecordingService::class.java).apply {
            action = ACTION_PAUSE
        }
        val pausePendingIntent = PendingIntent.getService(
            this, 1, pauseIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        
        val resumeIntent = Intent(this, RecordingService::class.java).apply {
            action = ACTION_RESUME
        }
        val resumePendingIntent = PendingIntent.getService(
            this, 2, resumeIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        
        val stopIntent = Intent(this, RecordingService::class.java).apply {
            action = ACTION_STOP
        }
        val stopPendingIntent = PendingIntent.getService(
            this, 3, stopIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle(title)
            .setContentText("$time - $status")
            .setSmallIcon(android.R.drawable.ic_btn_speak_now)
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .setShowWhen(true)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .apply {
                when (_recordingState.value) {
                    is RecordingState.Recording -> {
                        addAction(android.R.drawable.ic_media_pause, getString(R.string.pause), pausePendingIntent)
                    }
                    is RecordingState.Paused -> {
                        addAction(android.R.drawable.ic_media_play, getString(R.string.resume), resumePendingIntent)
                    }
                    else -> {}
                }
                addAction(android.R.drawable.ic_delete, getString(R.string.stop), stopPendingIntent)
            }
            .build()
    }
    
    private fun updateNotification(time: String, status: String) {
        val notification = createNotification("Recording", time, status)
        val notificationManager = getSystemService(NotificationManager::class.java)
        notificationManager.notify(NOTIFICATION_ID, notification)
    }
    
    override fun onDestroy() {
        super.onDestroy()
        serviceScope.cancel()
        audioManager?.abandonAudioFocus(audioFocusChangeListener)
    }
    
    fun getElapsedTimeMs(): Long {
        return if (startTime > 0) {
            System.currentTimeMillis() - startTime - totalPausedDuration
        } else {
            0
        }
    }
}

sealed class RecordingState {
    object Stopped : RecordingState()
    data class Recording(val meetingId: Long) : RecordingState()
    data class Paused(val meetingId: Long, val reason: String) : RecordingState()
}
