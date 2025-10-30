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
import android.media.AudioDeviceInfo
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
import com.androidvoiceapp.util.SilenceDetector
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
    private var silenceDetector: SilenceDetector? = null
    
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
        Log.d(TAG, "Service created")
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
            try {
                // Check storage
                if (!storageChecker.hasEnoughStorage()) {
                    currentStatus = getString(R.string.status_low_storage)
                    stopSelf()
                    return@launch
                }
                
                // Create meeting
                meetingId = meetingRepository.createMeeting(title)
                Log.d(TAG, "Created meeting: $meetingId")
                
                // Save session state
                saveSessionState()
                
                // Start foreground
                startForeground()
                
                // Start recording
                startRecording()
                
            } catch (e: Exception) {
                Log.e(TAG, "Failed to start recording", e)
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
        try {
            // Initialize audio focus
            audioFocusHelper = AudioFocusHelper(this) { gained ->
                if (gained) {
                    if (isPaused && !isInPhoneCall) {
                        handleResume()
                    }
                } else {
                    currentStatus = getString(R.string.status_paused_audio_focus)
                    handlePause()
                }
            }
            
            if (!audioFocusHelper!!.requestAudioFocus()) {
                Log.e(TAG, "Failed to gain audio focus")
                return
            }
            
            // Initialize components
            silenceDetector = SilenceDetector()
            audioRecorder = AudioRecordWrapper()
            
            if (!audioRecorder!!.start()) {
                Log.e(TAG, "Failed to start AudioRecord")
                return
            }
            
            isRecording = true
            isPaused = false
            recordingStartTime = System.currentTimeMillis()
            currentStatus = getString(R.string.status_recording)
            
            // Start recording loop
            serviceScope.launch {
                recordingLoop()
            }
            
            // Start timer update
            serviceScope.launch {
                updateTimer()
            }
            
            updateNotification()
            
        } catch (e: Exception) {
            Log.e(TAG, "Failed to start recording", e)
        }
    }

    private suspend fun recordingLoop() {
        while (isRecording) {
            if (isPaused) {
                delay(100)
                continue
            }
            
            try {
                // Check storage periodically
                if (!storageChecker.hasEnoughStorage()) {
                    currentStatus = getString(R.string.status_low_storage)
                    withContext(Dispatchers.Main) {
                        updateNotification()
                    }
                    stopRecording()
                    break
                }
                
                // Start new chunk
                startNewChunk()
                
                val chunkStartTime = System.currentTimeMillis()
                val buffer = ShortArray(audioRecorder!!.getBufferSize())
                
                // Record for CHUNK_DURATION_MS
                while (isRecording && !isPaused) {
                    val elapsed = System.currentTimeMillis() - chunkStartTime
                    if (elapsed >= CHUNK_DURATION_MS) {
                        break
                    }
                    
                    val samplesRead = audioRecorder!!.read(buffer)
                    if (samplesRead > 0) {
                        wavWriter?.write(buffer, samplesRead)
                        
                        // Check for silence
                        if (silenceDetector?.processSamples(buffer) == true) {
                            currentStatus = getString(R.string.status_no_audio)
                            withContext(Dispatchers.Main) {
                                updateNotification()
                            }
                        }
                    }
                    
                    delay(10) // Small delay to prevent busy loop
                }
                
                // Finalize chunk
                finalizeCurrentChunk()
                
                // Handle overlap: continue recording for OVERLAP_DURATION_MS
                // The next chunk will include this overlap
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
        
        Log.d(TAG, "Started chunk $currentChunkSequence")
    }

    private suspend fun finalizeCurrentChunk() {
        try {
            wavWriter?.close()
            wavWriter = null
            
            val finalFile = File(currentChunkFile!!.absolutePath.removeSuffix(".tmp"))
            currentChunkFile?.renameTo(finalFile)
            
            val duration = CHUNK_DURATION_MS
            val chunkId = chunkRepository.createChunk(
                meetingId = meetingId,
                sequenceNumber = currentChunkSequence,
                filePath = finalFile.absolutePath,
                duration = duration,
                startTime = currentChunkStartTime
            )
            
            Log.d(TAG, "Finalized chunk $currentChunkSequence with ID $chunkId")
            
            // Enqueue finalize worker
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
        serviceScope.launch {
            saveSessionState()
        }
        updateNotification()
    }

    private fun resumeRecording() {
        if (isPaused) {
            isPaused = false
            pausedDuration += System.currentTimeMillis() - lastPauseTime
            currentStatus = getString(R.string.status_recording)
            silenceDetector?.reset()
            serviceScope.launch {
                saveSessionState()
            }
            updateNotification()
        }
    }

    private suspend fun stopRecording() {
        isRecording = false
        isPaused = false
        
        try {
            // Finalize any current chunk
            if (currentChunkFile != null && currentChunkFile!!.exists()) {
                finalizeCurrentChunk()
            }
            
            // Stop audio
            audioRecorder?.stop()
            audioRecorder?.release()
            audioRecorder = null
            
            // Release audio focus
            audioFocusHelper?.abandonAudioFocus()
            audioFocusHelper = null
            
            // Update meeting
            meetingRepository.endMeeting(meetingId)
            
            // Clear session state
            sessionStateRepository.deleteSessionState(meetingId)
            
            currentStatus = getString(R.string.status_stopped)
            
        } catch (e: Exception) {
            Log.e(TAG, "Error stopping recording", e)
        }
    }

    private suspend fun saveSessionState() {
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
            val notificationManager = getSystemService(NotificationManager::class.java)
            notificationManager.createNotificationChannel(channel)
        }
    }

    private fun createNotification(): Notification {
        val elapsedTime = if (isRecording) {
            (System.currentTimeMillis() - recordingStartTime - pausedDuration) / 1000
        } else {
            0
        }
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

        // Add actions based on state
        if (isPaused) {
            val resumeIntent = Intent(this, RecordingService::class.java).apply {
                action = ACTION_RESUME
            }
            val resumePendingIntent = PendingIntent.getService(
                this, 0, resumeIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            builder.addAction(0, getString(R.string.notification_action_resume), resumePendingIntent)
        } else if (isRecording) {
            val pauseIntent = Intent(this, RecordingService::class.java).apply {
                action = ACTION_PAUSE
            }
            val pausePendingIntent = PendingIntent.getService(
                this, 0, pauseIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            builder.addAction(0, getString(R.string.notification_action_pause), pausePendingIntent)
        }

        val stopIntent = Intent(this, RecordingService::class.java).apply {
            action = ACTION_STOP
        }
        val stopPendingIntent = PendingIntent.getService(
            this, 0, stopIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        builder.addAction(0, getString(R.string.notification_action_stop), stopPendingIntent)

        return builder.build()
    }

    private fun updateNotification() {
        val notification = createNotification()
        val notificationManager = getSystemService(NotificationManager::class.java)
        notificationManager.notify(NOTIFICATION_ID, notification)
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
                        handlePhoneStateChange(state)
                    }
                }
                phoneStateListener = callback
                telephonyManager?.registerTelephonyCallback(
                    mainExecutor,
                    callback
                )
            }
        } else {
            @Suppress("DEPRECATION")
            val listener = object : PhoneStateListener() {
                override fun onCallStateChanged(state: Int, phoneNumber: String?) {
                    handlePhoneStateChange(state)
                }
            }
            phoneStateListener = listener
            @Suppress("DEPRECATION")
            telephonyManager?.listen(listener, PhoneStateListener.LISTEN_CALL_STATE)
        }
    }

    private fun handlePhoneStateChange(state: Int) {
        when (state) {
            TelephonyManager.CALL_STATE_RINGING,
            TelephonyManager.CALL_STATE_OFFHOOK -> {
                // Phone call started
                if (isRecording && !isPaused) {
                    isInPhoneCall = true
                    currentStatus = getString(R.string.status_paused_phone_call)
                    handlePause()
                }
            }
            TelephonyManager.CALL_STATE_IDLE -> {
                // Phone call ended
                if (isInPhoneCall) {
                    isInPhoneCall = false
                    if (isPaused) {
                        handleResume()
                    }
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
                // Show notification about headset change
                showHeadsetChangeNotification()
            }
        }
        
        registerReceiver(headsetReceiver, filter)
    }

    private fun showHeadsetChangeNotification() {
        // Just log for now, could show a Toast or separate notification
        Log.d(TAG, getString(R.string.notification_headset_changed))
    }

    override fun onDestroy() {
        super.onDestroy()
        Log.d(TAG, "Service destroyed")
        
        serviceScope.launch {
            // Handle process death - finalize current chunk
            if (isRecording && currentChunkFile != null) {
                try {
                    finalizeCurrentChunk()
                } catch (e: Exception) {
                    Log.e(TAG, "Failed to finalize chunk on destroy", e)
                }
            }
        }
        
        // Cleanup
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
            Log.e(TAG, "Error during cleanup", e)
        }
        
        serviceScope.cancel()
    }
}
