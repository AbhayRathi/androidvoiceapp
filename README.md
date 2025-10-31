# Android Voice App

## Overview

This Android application is designed to record audio, transcribe it in near real-time, and generate a concise summary. It leverages modern Android development practices, including Jetpack Compose for the UI, Hilt for dependency injection, and WorkManager for reliable background processing. Transcription and summarization are handled by the Google Gemini API.

## Core Functionality

- **Audio Recording**: Record audio from the device's microphone.
- **Background Processing**: The app uses `WorkManager` to process audio reliably, even if the app is in the background. Audio is split into 30-second chunks for processing.
- **Transcription**: Each audio chunk is sent to the Gemini 1.5 Flash model for transcription.
- **Summarization**: Once all chunks are transcribed, the full text is sent to the Gemini API to generate a summary of the meeting.
- **Dynamic UI**: The UI, built with Jetpack Compose, updates asynchronously to show the status of each chunk (e.g., "TRANSCRIBED") and displays the final summary.

## Setup Instructions

1.  **Clone the Repository**:
    ```sh
    git clone <repository-url>
    ```

2.  **Open in Android Studio**: Open the cloned project in the latest version of Android Studio.

3.  **Add Gemini API Key**:
    The project requires a valid Google Gemini API key to function. You must add your key to the following two files:
    - `app/src/main/java/com/androidvoiceapp/api/GeminiTranscriptionApi.kt`
    - `app/src/main/java/com/androidvoiceapp/api/GeminiSummaryApi.kt`

    Find the `API_KEY` constant in each file and replace `"YOUR_API_KEY"` with your actual key.
    ```kotlin
    private const val API_KEY = "AIzaSy..." // Replace with your key
    ```
    *Note: Hardcoding API keys is not a secure practice for production apps, but is used here for simplicity.*

4.  **Configure Emulator Audio**:
    For reliable audio recording in the Android Emulator, use the following settings in **Extended Controls > Microphone**:
    - **Virtual headset plug inserted**: OFF
    - **Virtual headset has microphone**: OFF
    - **Virtual microphone uses host audio input**: **ON**

5.  **Build and Run**: Build the project and run the `app` configuration on an emulator or physical device.

## Testing & Build Variants

This project is configured with different "source sets" for the `main` application and `debug` builds. This allows for different dependencies or code to be used during testing.

### Mock API Provider

The app includes a full mock of the API layer (`MockTranscriptionApi` and `MockSummaryApi`). This allows for end-to-end testing of the entire application pipeline (recording, background workers, database, UI) without making any actual network calls or incurring API costs.

You can switch between the real and mock APIs from the **Settings** screen within the app.

### The `debug` Source Set Mystery

A key challenge during development was a persistent build issue related to the `debug` source set located at `app/src/debug/java/`.

- **Intention**: This folder is intended to provide alternative implementations for testing. For example, a file like `app/src/debug/java/com/androidvoiceapp/di/ApiModule.kt` can override the API implementations provided in the `main` source set.
- **Observed Issue**: In this project, the debug build was consistently failing to execute the real `GeminiTranscriptionApi` code, even when the Hilt module in the `debug` folder was correctly configured to provide it. The build system appeared to be caching and using an old or different version of the code, a problem that even the "Invalidate Caches / Restart" command did not solve.

## Current Status & Known Issues

- **What Works**: The entire application pipeline—including audio recording, chunking, background `WorkManager` jobs, database updates, and UI state changes—**works perfectly**. This has been verified extensively using the **Mock API provider**. The app correctly processes audio and displays the mock transcription and summary.

- **Unresolved Issue**: The integration with the **real Gemini API is not working**.
  - **Symptom**: When using the "Gemini" provider, the app records audio and the background workers run and report `SUCCESS`. The UI updates to show chunks as "TRANSCRIBED". However, the Gemini API silently returns an empty response. No transcription text is ever saved or displayed.
  - **Root Cause**: The issue is confirmed to be a **build system or dependency injection problem**. Logs definitively show that the code within `GeminiTranscriptionApi.kt` is **never executed** during a debug build, despite all configuration files appearing correct. The build system is providing a phantom implementation that does nothing.

### What Has Been Pushed

The code pushed to GitHub reflects this state. It includes:
- All the working application logic.
- The fully functional mock API for testing.
- The problematic but correctly configured Hilt modules.
- Added logging and error handling to prove the source of the issue.
