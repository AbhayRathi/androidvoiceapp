package com.abhay.voiceapp.ui.viewmodel

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.abhay.voiceapp.data.entity.Summary
import com.abhay.voiceapp.data.entity.TranscriptSegment
import com.abhay.voiceapp.data.repository.SummaryRepository
import com.abhay.voiceapp.data.repository.TranscriptRepository
import com.abhay.voiceapp.worker.SummaryWorker
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import android.content.Context
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

data class SummaryUiState(
    val title: String = "",
    val summaryText: String = "",
    val actionItems: List<String> = emptyList(),
    val keyPoints: List<String> = emptyList(),
    val isLoading: Boolean = false,
    val hasError: Boolean = false
)

@HiltViewModel
class SummaryViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val summaryRepository: SummaryRepository,
    private val transcriptRepository: TranscriptRepository,
    savedStateHandle: SavedStateHandle
) : ViewModel() {
    
    private val meetingId: Long = savedStateHandle.get<String>("meetingId")?.toLongOrNull() ?: -1
    private val gson = Gson()
    
    val summaryState: StateFlow<SummaryUiState> = summaryRepository.getSummaryByMeetingId(meetingId)
        .map { summary ->
            summary?.let {
                SummaryUiState(
                    title = it.title ?: "",
                    summaryText = it.summaryText ?: "",
                    actionItems = parseJsonList(it.actionItems),
                    keyPoints = parseJsonList(it.keyPoints),
                    isLoading = it.status == com.abhay.voiceapp.data.entity.SummaryStatus.GENERATING,
                    hasError = it.status == com.abhay.voiceapp.data.entity.SummaryStatus.FAILED
                )
            } ?: SummaryUiState(isLoading = true)
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = SummaryUiState(isLoading = true)
        )
    
    val transcript: StateFlow<List<TranscriptSegment>> = 
        transcriptRepository.getTranscriptByMeetingId(meetingId)
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5000),
                initialValue = emptyList()
            )
    
    private fun parseJsonList(json: String?): List<String> {
        if (json.isNullOrBlank()) return emptyList()
        return try {
            val type = object : TypeToken<List<String>>() {}.type
            gson.fromJson(json, type)
        } catch (e: Exception) {
            emptyList()
        }
    }
    
    fun retryGenerateSummary() {
        SummaryWorker.enqueue(context, meetingId)
    }
}
