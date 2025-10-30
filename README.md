# Voice Recorder - Android Take-Home Assignment

A complete Android voice recording application with transcription and summary generation, featuring robust edge case handling and a clean MVVM architecture.

## Overview

This application implements a comprehensive voice recording solution with the following key features:
- **Foreground recording service** with 30-second WAV chunks and 2-second overlap
- **Automatic transcription** of audio chunks using mock/real APIs
- **AI-powered summary generation** with streaming updates
- **Robust edge case handling** for phone calls, audio focus, headset changes, low storage, and process death
- **100% Jetpack Compose UI** with Material 3 design

## Architecture

### Tech Stack
- **Language**: Kotlin
- **UI**: Jetpack Compose with Material 3
- **Architecture**: MVVM (Model-View-ViewModel)
- **DI**: Hilt for dependency injection
- **Database**: Room for local persistence
- **Background Processing**: WorkManager for reliable background tasks
- **Audio**: AudioRecord API with custom WAV writer
- **Min SDK**: API 24 (Android 7.0)
- **Target SDK**: API 34

### Project Structure

```
app/
├── src/main/java/com/voicerecorder/
│   ├── audio/              # Audio recording components
│   │   ├── AudioRecordWrapper.kt    # Wrapper around AudioRecord API
│   │   └── WavWriter.kt             # WAV file writer with proper headers
│   ├── data/               # Data layer
│   │   ├── room/           # Room entities and DAOs
│   │   │   ├── Meeting.kt
│   │   │   ├── Chunk.kt
│   │   │   ├── TranscriptSegment.kt
│   │   │   ├── Summary.kt
│   │   │   ├── SessionState.kt
│   │   │   └── AppDatabase.kt
│   │   └── repository/     # Repository interfaces and implementations
│   ├── service/            # Foreground service
│   │   └── RecordingService.kt      # Handles recording, chunks, edge cases
│   ├── workers/            # WorkManager workers
│   │   ├── FinalizeChunkWorker.kt   # Finalizes audio chunks
│   │   ├── TranscriptionWorker.kt   # Transcribes audio chunks
│   │   └── SummaryWorker.kt         # Generates summaries with streaming
│   ├── api/                # API interfaces
│   │   ├── ApiInterfaces.kt
│   │   └── mock/           # Mock implementations
│   │       ├── MockTranscriptionApi.kt
│   │       └── MockSummaryApi.kt
│   ├── ui/                 # Compose UI screens
│   │   ├── dashboard/      # Meeting list screen
│   │   ├── meeting/        # Recording screen with controls
│   │   ├── summary/        # Summary display screen
│   │   ├── settings/       # Settings screen
│   │   └── navigation/     # Navigation setup
│   ├── util/               # Utility classes
│   │   ├── SilenceDetector.kt       # Detects silent audio
│   │   ├── StorageChecker.kt        # Checks available storage
│   │   └── AudioFocusHelper.kt      # Manages audio focus
│   └── di/                 # Hilt DI modules
└── src/test/              # Unit tests
```

## Features Implementation

### 1. Audio Recording
- **30-second chunks** with 2-second overlap for speech continuity
- **PCM 16-bit mono WAV format** at 16kHz sample rate
- **Silence detection**: Warns after 10 seconds of no audio input
- **Low storage check**: Stops gracefully when storage is low
- **Persistent notification** with timer and pause/resume/stop actions
- **Lock screen visibility** with live status updates

### 2. Edge Case Handling

#### Phone Calls
- Automatically pauses recording when phone call starts
- Shows "Paused - Phone call" status
- User can manually resume after call ends

#### Audio Focus Loss
- Pauses when another app takes audio focus
- Shows "Paused - Audio focus lost" notification
- Provides Resume/Stop actions in notification

#### Headset Changes
- Shows notification when headset is plugged/unplugged
- Continues recording when possible
- Handles Bluetooth and wired headsets

#### Process Death Recovery
- Persists session state in Room database
- Enqueues FinalizeChunkWorker on process death
- Resumes transcription automatically on app restart

### 3. Transcription
- **Ordered transcription**: Chunks are transcribed in sequence
- **Retry-all-on-failure**: If any chunk fails permanently, all chunks are re-enqueued
- **Mock implementation**: Generates deterministic transcript for testing
- **Real API ready**: Pluggable architecture for real transcription APIs

### 4. Summary Generation
- **Streaming updates**: Summary progressively updates in the database and UI
- **Foreground WorkManager**: Survives app kill using expedited work
- **Structured output**: Title, Summary, Action Items, Key Points
- **Mock implementation**: Simulates streaming API responses
- **Error handling**: Shows error state with retry button

### 5. UI Screens

#### Dashboard
- Lists all meetings with status
- FAB to start new recording
- Settings access

#### Meeting/Recording Screen
- Live timer showing elapsed time
- Status indicator (Recording/Paused/Stopped)
- Control buttons (Start/Pause/Resume/Stop)
- List of chunks with transcription status
- Permissions handling

#### Summary Screen
- Four sections: Title, Summary, Action Items, Key Points
- Loading state with progress percentage
- Error state with retry functionality
- Full transcript display

#### Settings Screen
- Provider selection (Mock/Real API)
- API key input (stored securely for future use)
- Information about mock vs real providers

## Building and Running

### Prerequisites
- Android Studio Hedgehog (2023.1.1) or later
- JDK 17
- Android SDK with API 34
- Gradle 8.2+

### Build Instructions

1. **Clone the repository**
   ```bash
   git clone https://github.com/AbhayRathi/androidvoiceapp.git
   cd androidvoiceapp
   ```

2. **Open in Android Studio**
   - Open Android Studio
   - Select "Open an existing project"
   - Navigate to the cloned directory

3. **Sync Gradle**
   - Android Studio will automatically sync Gradle
   - Wait for dependencies to download

4. **Build Debug APK**
   ```bash
   ./gradlew assembleDebug
   ```
   The APK will be located at: `app/build/outputs/apk/debug/app-debug.apk`

5. **Install on Device/Emulator**
   ```bash
   ./gradlew installDebug
   ```
   Or use Android Studio's Run button

### Running Tests

```bash
# Run unit tests
./gradlew test

# Run specific test class
./gradlew test --tests SilenceDetectorTest
./gradlew test --tests WavWriterTest
```

## Demo Script

### Basic Recording Flow

1. **Start the app** - Opens Dashboard with empty meeting list
2. **Tap the + FAB** - Creates new meeting and navigates to Meeting screen
3. **Grant permissions** - Allow audio recording and notifications
4. **Tap "Start Recording"** - Begins recording with foreground service
5. **Observe the timer** - Updates every second
6. **Check notification** - Shows live timer and status on lock screen
7. **Tap "Pause"** - Pauses recording, finishes current chunk
8. **Tap "Resume"** - Continues recording with new chunk
9. **Tap "Stop"** - Stops recording and finalizes all chunks

### Transcription and Summary

10. **Wait for transcription** - Chunks are automatically transcribed in background
11. **Observe chunk status** - Changes from PENDING → IN_PROGRESS → COMPLETED
12. **Tap article icon** - Navigates to Summary screen
13. **Watch streaming summary** - Progressively updates as it generates
14. **View all sections** - Title, Summary, Action Items, Key Points, Transcript

### Edge Cases

**Simulate Phone Call:**
- Place/receive a call during recording
- Recording automatically pauses
- Status shows "Paused - Phone call"
- Resume manually after call ends

**Test Audio Focus:**
- Play music from another app during recording
- Recording pauses automatically
- Notification shows Resume/Stop actions

**Test Low Storage:**
- Recording checks storage periodically
- Stops gracefully if storage is low
- Shows "Recording stopped - Low storage"

**Test Process Death:**
- Kill app during recording (swipe from recent apps)
- Session state is persisted
- Workers finalize last chunk and resume transcription

## Mock vs Real API Integration

### Current State (Mock)
The app currently uses mock implementations for transcription and summary generation:
- **MockTranscriptionApi**: Returns deterministic transcripts based on chunk number
- **MockSummaryApi**: Simulates streaming API with progressive updates
- **No API key required**: Works out of the box for testing

### Real API Integration (Future)

To integrate real APIs (OpenAI Whisper, Google Gemini, etc.):

1. **Implement Real API Classes**
   Create implementations of `TranscriptionApi` and `SummaryApi` in a new package:
   ```kotlin
   // app/src/main/java/com/voicerecorder/api/real/
   class OpenAITranscriptionApi @Inject constructor() : TranscriptionApi {
       override suspend fun transcribe(audioFile: File, chunkNumber: Int): TranscriptResult {
           // TODO: Call OpenAI Whisper API
       }
   }
   ```

2. **Update DI Module**
   Modify `ApiModule.kt` to provide real implementations based on settings:
   ```kotlin
   @Provides
   @Singleton
   fun provideTranscriptionApi(
       settings: SettingsRepository
   ): TranscriptionApi {
       return if (settings.useRealApi()) {
           OpenAITranscriptionApi()
       } else {
           MockTranscriptionApi()
       }
   }
   ```

3. **Add API Key Storage**
   API keys entered in Settings screen can be stored using EncryptedSharedPreferences:
   ```kotlin
   // Already prepared in Settings screen, just needs implementation
   ```

4. **Add Network Dependencies**
   Update `app/build.gradle.kts`:
   ```kotlin
   implementation("com.squareup.retrofit2:retrofit:2.9.0")
   implementation("com.squareup.retrofit2:converter-gson:2.9.0")
   implementation("com.squareup.okhttp3:okhttp:4.12.0")
   implementation("com.squareup.okhttp3:logging-interceptor:4.12.0")
   ```

## Testing

### Unit Tests Included

1. **SilenceDetectorTest**
   - Tests silence detection logic
   - Validates 10-second threshold
   - Tests reset functionality
   - Verifies duration calculation

2. **WavWriterTest**
   - Tests WAV file creation
   - Validates header correctness
   - Tests multiple writes
   - Verifies chunk duration calculations
   - Tests 2-second overlap logic

### Running on Emulator

**Recommended**: API 24+ emulator with:
- Google APIs
- Audio input enabled
- External storage

**To test audio:**
- Use Android Studio's Virtual Microphone
- Or use physical device for real audio testing

## Permissions

The app requires the following permissions:
- `RECORD_AUDIO` - To record audio
- `FOREGROUND_SERVICE` - To run recording service
- `FOREGROUND_SERVICE_MICROPHONE` - For microphone foreground service type
- `POST_NOTIFICATIONS` - To show notifications (Android 13+)
- `READ_PHONE_STATE` - To detect phone calls (optional)
- `WAKE_LOCK` - To keep service alive

## Known Limitations

1. **Mock API Only**: Real API integration needs to be implemented
2. **No Encryption**: Audio files are stored unencrypted (can be added)
3. **No Compression**: Uses uncompressed WAV format (can add compression)
4. **Basic UI**: Minimal Material 3 design (can be enhanced)
5. **No Export**: Cannot export/share recordings (can be added)

## Future Enhancements

- [ ] Real API integration (OpenAI Whisper, Google Gemini)
- [ ] Audio file encryption
- [ ] MP3/AAC compression
- [ ] Cloud backup integration
- [ ] Export/share functionality
- [ ] Voice activity detection for better chunking
- [ ] Speaker diarization
- [ ] Multi-language support
- [ ] Search functionality
- [ ] Tags and categories

## License

This project is created as a take-home assignment demonstration.

## Contact

For questions or issues, please open an issue on GitHub.
