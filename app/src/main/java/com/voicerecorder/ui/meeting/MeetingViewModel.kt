package com.voicerecorder.ui.meeting

import android.content.Context
import android.content.Intent
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.voicerecorder.data.repository.ChunkRepository
import com.voicerecorder.data.repository.MeetingRepository
import com.voicerecorder.data.repository.SessionStateRepository
import com.voicerecorder.data.room.Chunk
import com.voicerecorder.data.room.Meeting
import com.voicerecorder.data.room.SessionState
import com.voicerecorder.service.RecordingService
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class MeetingViewModel @Inject constructor(
    private val meetingRepository: MeetingRepository,
    private val chunkRepository: ChunkRepository,
    private val sessionStateRepository: SessionStateRepository
) : ViewModel() {

    private val _meetingId = MutableStateFlow<Long?>(null)
    val meetingId: StateFlow<Long?> = _meetingId.asStateFlow()

    val meeting: StateFlow<Meeting?> = _meetingId
        .filterNotNull()
        .flatMapLatest { meetingRepository.getMeetingById(it) }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = null
        )

    val chunks: StateFlow<List<Chunk>> = _meetingId
        .filterNotNull()
        .flatMapLatest { chunkRepository.getChunksForMeeting(it) }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    val sessionState: StateFlow<SessionState?> = sessionStateRepository.getSessionState()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = null
        )

    fun setMeetingId(id: Long) {
        _meetingId.value = id
    }

    fun startRecording(context: Context, meetingId: Long) {
        viewModelScope.launch {
            val intent = Intent(context, RecordingService::class.java).apply {
                action = RecordingService.ACTION_START
                putExtra(RecordingService.EXTRA_MEETING_ID, meetingId)
            }
            context.startForegroundService(intent)
        }
    }

    fun pauseRecording(context: Context) {
        val intent = Intent(context, RecordingService::class.java).apply {
            action = RecordingService.ACTION_PAUSE
        }
        context.startService(intent)
    }

    fun resumeRecording(context: Context) {
        val intent = Intent(context, RecordingService::class.java).apply {
            action = RecordingService.ACTION_RESUME
        }
        context.startService(intent)
    }

    fun stopRecording(context: Context) {
        val intent = Intent(context, RecordingService::class.java).apply {
            action = RecordingService.ACTION_STOP
        }
        context.startService(intent)
    }
}
