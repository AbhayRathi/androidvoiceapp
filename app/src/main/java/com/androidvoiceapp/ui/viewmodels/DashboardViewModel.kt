package com.androidvoiceapp.ui.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.androidvoiceapp.data.repository.MeetingRepository
import com.androidvoiceapp.data.room.MeetingEntity
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
    
    val meetings: StateFlow<List<MeetingEntity>> = meetingRepository.getAllMeetings()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )
    
    fun deleteMeeting(meeting: MeetingEntity) {
        viewModelScope.launch {
            meetingRepository.deleteMeeting(meeting)
        }
    }
}
