package com.androidvoiceapp.ui.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.androidvoiceapp.data.repository.ChunkRepository
import com.androidvoiceapp.data.repository.MeetingRepository
import com.androidvoiceapp.data.repository.SessionStateRepository
import com.androidvoiceapp.data.room.ChunkEntity
import com.androidvoiceapp.data.room.MeetingEntity
import com.androidvoiceapp.data.room.SessionStateEntity
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class RecordingViewModel @Inject constructor(
    private val meetingRepository: MeetingRepository,
    private val chunkRepository: ChunkRepository,
    private val sessionStateRepository: SessionStateRepository
) : ViewModel() {
    
    private val _meetingId = MutableStateFlow(-1L)
    
    val meeting: StateFlow<MeetingEntity?> = _meetingId
        .flatMapLatest { id ->
            if (id != -1L) {
                meetingRepository.getMeetingById(id)
            } else {
                flowOf(null)
            }
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = null
        )
    
    val chunks: StateFlow<List<ChunkEntity>> = _meetingId
        .flatMapLatest { id ->
            if (id != -1L) {
                chunkRepository.getChunksByMeetingId(id)
            } else {
                flowOf(emptyList())
            }
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )
    
    val sessionState: StateFlow<SessionStateEntity?> = _meetingId
        .flatMapLatest { id ->
            if (id != -1L) {
                sessionStateRepository.getSessionState(id)
            } else {
                flowOf(null)
            }
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = null
        )
    
    fun setMeetingId(id: Long) {
        _meetingId.value = id
    }
}
