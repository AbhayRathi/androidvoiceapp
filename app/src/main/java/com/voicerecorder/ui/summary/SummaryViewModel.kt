package com.voicerecorder.ui.summary

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.voicerecorder.data.repository.SummaryRepository
import com.voicerecorder.data.repository.TranscriptRepository
import com.voicerecorder.data.room.Summary
import com.voicerecorder.data.room.TranscriptSegment
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SummaryViewModel @Inject constructor(
    private val summaryRepository: SummaryRepository,
    private val transcriptRepository: TranscriptRepository,
    private val gson: Gson
) : ViewModel() {

    private val _meetingId = MutableStateFlow<Long?>(null)
    val meetingId: StateFlow<Long?> = _meetingId.asStateFlow()

    val summary: StateFlow<Summary?> = _meetingId
        .filterNotNull()
        .flatMapLatest { summaryRepository.getSummaryForMeeting(it) }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = null
        )

    val transcript: StateFlow<List<TranscriptSegment>> = _meetingId
        .filterNotNull()
        .flatMapLatest { transcriptRepository.getSegmentsForMeeting(it) }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    fun setMeetingId(id: Long) {
        _meetingId.value = id
    }

    fun getActionItems(summary: Summary?): List<String> {
        if (summary?.actionItems.isNullOrEmpty()) return emptyList()
        return try {
            val type = object : TypeToken<List<String>>() {}.type
            gson.fromJson(summary.actionItems, type)
        } catch (e: Exception) {
            emptyList()
        }
    }

    fun getKeyPoints(summary: Summary?): List<String> {
        if (summary?.keyPoints.isNullOrEmpty()) return emptyList()
        return try {
            val type = object : TypeToken<List<String>>() {}.type
            gson.fromJson(summary.keyPoints, type)
        } catch (e: Exception) {
            emptyList()
        }
    }

    fun retrySummaryGeneration() {
        viewModelScope.launch {
            val meetingId = _meetingId.value ?: return@launch
            // TODO: Re-enqueue summary worker
        }
    }
}
