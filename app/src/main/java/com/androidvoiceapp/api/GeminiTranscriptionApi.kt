package com.androidvoiceapp.api

import com.androidvoiceapp.data.room.TranscriptSegmentEntity
import com.google.ai.client.generativeai.GenerativeModel
import com.google.ai.client.generativeai.type.content
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class GeminiTranscriptionApi @Inject constructor() : TranscriptionApi {

    companion object {
        // This should be loaded from a secure place, but using the provided key for now.
        private const val API_KEY = "AIzaSyB_JY45L0dBPrWrKqu-bGDMe7U0Dad97wE"
    }

    private val generativeModel = GenerativeModel(
        modelName = "gemini-1.5-flash",
        apiKey = API_KEY
    )

    override suspend fun transcribe(
        chunkFile: File,
        meetingId: Long,
        chunkId: Long,
        sequenceNumber: Int,
        chunkStartTime: Long
    ): List<TranscriptSegmentEntity> = withContext(Dispatchers.IO) {
        try {
            val response = generativeModel.generateContent(
                content {
                    // The Gemini 1.5 API supports direct file uploads.
                    // For this use case, we send the prompt and the file data.
                    text("Transcribe this audio file.")
                    blob("audio/wav", chunkFile.readBytes())
                }
            )

            val transcribedText = response.text ?: ""

            val segment = TranscriptSegmentEntity(
                meetingId = meetingId,
                chunkId = chunkId,
                text = transcribedText,
                startTime = chunkStartTime,
                endTime = chunkStartTime + 30000, // Assuming 30s chunk
                confidence = 0.9f // Placeholder confidence
            )
            listOf(segment)
        } catch (e: Exception) {
            // Log the exception
            emptyList()
        }
    }

    override fun isAvailable(): Boolean {
        return API_KEY.isNotEmpty() && API_KEY != "YOUR_API_KEY"
    }
}
