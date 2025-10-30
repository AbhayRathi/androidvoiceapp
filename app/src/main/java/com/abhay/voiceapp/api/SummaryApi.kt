package com.abhay.voiceapp.api

import kotlinx.coroutines.flow.Flow

data class SummaryUpdate(
    val type: SummaryUpdateType,
    val content: String
)

enum class SummaryUpdateType {
    TITLE,
    SUMMARY_TEXT,
    ACTION_ITEM,
    KEY_POINT,
    COMPLETE
}

interface SummaryApi {
    fun generateSummary(transcript: String): Flow<SummaryUpdate>
}
