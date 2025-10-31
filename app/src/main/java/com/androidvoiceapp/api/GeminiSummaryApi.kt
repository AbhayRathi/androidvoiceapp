package com.androidvoiceapp.api

import com.google.ai.client.generativeai.GenerativeModel
import com.google.ai.client.generativeai.type.content
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class GeminiSummaryApi @Inject constructor() : SummaryApi {

    companion object {
        // This should be loaded from a secure place, but using the provided key for now.
        private const val API_KEY = "AIzaSyB_JY45L0dBPrWrKqu-bGDMe7U0Dad97wE"
    }

    private val generativeModel = GenerativeModel(
        modelName = "gemini-1.5-flash",
        apiKey = API_KEY,
    )

    override fun generateSummary(transcript: String): Flow<SummaryUpdate> {
        val prompt = """
            Based on the following meeting transcript, generate a structured summary with these four sections:
            1. Title: A concise, descriptive title for the meeting.
            2. Summary: A brief paragraph summarizing the key discussion points and outcomes.
            3. Action Items: A bulleted list of specific tasks assigned to individuals or the team.
            4. Key Points: A bulleted list of the most important decisions, conclusions, or information shared.

            Format the output clearly with each section heading. Start each section with 'Title:', 'Summary:', 'Action Items:', and 'Key Points:'.

            Transcript:
            $transcript
        """.trimIndent()

        return generativeModel.generateContentStream(content { text(prompt) }).map {
            val text = it.text ?: ""
            SummaryUpdate(text = text, isDone = false) // The flow's completion will signal that it's done.
        }
    }
}
