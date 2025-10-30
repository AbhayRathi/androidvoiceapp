package com.abhay.voiceapp.ui.viewmodel

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.IBinder
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.abhay.voiceapp.data.entity.Chunk
import com.abhay.voiceapp.data.entity.Meeting
import com.abhay.voiceapp.data.entity.MeetingStatus
import com.abhay.voiceapp.data.repository.ChunkRepository
import com.abhay.voiceapp.data.repository.MeetingRepository
import com.abhay.voiceapp.service.RecordingService
import com.abhay.voiceapp.service.RecordingState
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class RecordingViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val meetingRepository: MeetingRepository,
    private val chunkRepository: ChunkRepository,
    savedStateHandle: SavedStateHandle
) : ViewModel() {
    
    private val meetingId: Long = savedStateHandle.get<String>("meetingId")?.toLongOrNull() ?: -1
    
    val meeting: StateFlow<Meeting?> = meetingRepository.getMeetingById(meetingId)
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = null
        )
    
    val chunks: StateFlow<List<Chunk>> = chunkRepository.getChunksByMeetingId(meetingId)
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )
    
    private val _recordingState = MutableStateFlow<RecordingState>(RecordingState.Stopped)
    val recordingState: StateFlow<RecordingState> = _recordingState.asStateFlow()
    
    private val _statusMessage = MutableStateFlow("")
    val statusMessage: StateFlow<String> = _statusMessage.asStateFlow()
    
    private val _elapsedTime = MutableStateFlow(0L)
    val elapsedTime: StateFlow<Long> = _elapsedTime.asStateFlow()
    
    private var recordingService: RecordingService? = null
    private val serviceConnection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
            val binder = service as RecordingService.LocalBinder
            recordingService = binder.getService()
            
            // Observe service state
            viewModelScope.launch {
                recordingService?.recordingState?.collect { state ->
                    _recordingState.value = state
                }
            }
            
            viewModelScope.launch {
                recordingService?.statusMessage?.collect { message ->
                    _statusMessage.value = message
                }
            }
        }
        
        override fun onServiceDisconnected(name: ComponentName?) {
            recordingService = null
        }
    }
    
    init {
        // Bind to service if it's running
        bindToService()
        
        // Update elapsed time periodically
        viewModelScope.launch {
            while (true) {
                recordingService?.let {
                    _elapsedTime.value = it.getElapsedTimeMs()
                }
                kotlinx.coroutines.delay(1000)
            }
        }
    }
    
    private fun bindToService() {
        val intent = Intent(context, RecordingService::class.java)
        context.bindService(intent, serviceConnection, Context.BIND_AUTO_CREATE)
    }
    
    fun startRecording(title: String) {
        viewModelScope.launch {
            val id = meetingRepository.createMeeting(title, System.currentTimeMillis())
            
            val intent = Intent(context, RecordingService::class.java).apply {
                action = RecordingService.ACTION_START
                putExtra(RecordingService.EXTRA_MEETING_ID, id)
                putExtra(RecordingService.EXTRA_MEETING_TITLE, title)
            }
            context.startForegroundService(intent)
            bindToService()
        }
    }
    
    fun pauseRecording() {
        val intent = Intent(context, RecordingService::class.java).apply {
            action = RecordingService.ACTION_PAUSE
        }
        context.startService(intent)
    }
    
    fun resumeRecording() {
        val intent = Intent(context, RecordingService::class.java).apply {
            action = RecordingService.ACTION_RESUME
        }
        context.startService(intent)
    }
    
    fun stopRecording() {
        val intent = Intent(context, RecordingService::class.java).apply {
            action = RecordingService.ACTION_STOP
        }
        context.startService(intent)
    }
    
    override fun onCleared() {
        super.onCleared()
        try {
            context.unbindService(serviceConnection)
        } catch (e: Exception) {
            // Service might not be bound
        }
    }
}
