package com.voicerecorder.ui.dashboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.voicerecorder.data.repository.MeetingRepository
import com.voicerecorder.data.room.Meeting
import com.voicerecorder.data.room.MeetingStatus
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class DashboardViewModel @Inject constructor(
    private val meetingRepository: MeetingRepository
) : ViewModel() {

    val meetings: StateFlow<List<Meeting>> = meetingRepository.getAllMeetings()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    fun createNewMeeting(onMeetingCreated: (Long) -> Unit) {
        viewModelScope.launch {
            val meeting = Meeting(
                startTime = System.currentTimeMillis(),
                status = MeetingStatus.RECORDING,
                title = "Meeting ${System.currentTimeMillis()}"
            )
            val meetingId = meetingRepository.createMeeting(meeting)
            onMeetingCreated(meetingId)
        }
    }
}
