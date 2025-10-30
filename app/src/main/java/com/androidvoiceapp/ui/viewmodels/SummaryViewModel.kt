package com.androidvoiceapp.ui.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.androidvoiceapp.data.repository.SummaryRepository
import com.androidvoiceapp.data.room.SummaryEntity
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SummaryViewModel @Inject constructor(
    private val summaryRepository: SummaryRepository
) : ViewModel() {
    
    private val _meetingId = MutableStateFlow(-1L)
    
    val summary: StateFlow<SummaryEntity?> = _meetingId
        .flatMapLatest { id ->
            if (id != -1L) {
                summaryRepository.getSummaryByMeetingId(id)
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
    
    fun retrySummary() {
        viewModelScope.launch {
            val meetingId = _meetingId.value
            if (meetingId != -1L) {
                // TODO: Trigger summary worker again
            }
        }
    }
}
