# Android Voice Recording App - Implementation

A robust voice recording application for Android with automatic transcription and AI-powered summary generation. This implementation handles real-world edge cases including phone calls, audio focus changes, device source switching, and process death recovery.

## 🏗️ Architecture

### Tech Stack
- **Language**: Kotlin
- **UI**: Jetpack Compose (100% declarative UI)
- **Architecture**: MVVM (Model-View-ViewModel)
- **Dependency Injection**: Hilt
- **Database**: Room
- **Background Processing**: WorkManager
- **Async Operations**: Coroutines & Flow
- **Minimum SDK**: 24 (Android 7.0)
- **Target SDK**: 34 (Android 14)

### Architecture Layers

```
┌─────────────────────────────────────────────────────────┐
│                     UI Layer (Compose)                  │
│  Dashboard | Recording Screen | Summary Screen          │
│  ViewModels with StateFlow                              │
└─────────────────────────────────────────────────────────┘
                           ↓
┌─────────────────────────────────────────────────────────┐
│                  Service & Workers                       │
│  RecordingService (Foreground)                          │
│  FinalizeChunkWorker → TranscriptionWorker →            │
│  SummaryWorker (Foreground)                             │
└─────────────────────────────────────────────────────────┘
                           ↓
┌─────────────────────────────────────────────────────────┐
│                  Repository Layer                        │
│  MeetingRepository | ChunkRepository                    │
│  TranscriptRepository | SummaryRepository               │
└─────────────────────────────────────────────────────────┘
                           ↓
┌─────────────────────────────────────────────────────────┐
│              Data Layer (Room Database)                  │
│  Meeting | Chunk | TranscriptSegment                    │
│  Summary | SessionState                                 │
└─────────────────────────────────────────────────────────┘
                           ↓
┌─────────────────────────────────────────────────────────┐
│               Audio & API Layer                          │
│  AudioRecorder | ChunkManager | SilenceDetector         │
│  TranscriptionApi (Mock) | SummaryApi (Mock)            │
└─────────────────────────────────────────────────────────┘
```

## 🎯 Features Implemented

### 1. Audio Recording
- ✅ Foreground service with persistent notification
- ✅ 30-second audio chunks with 2-second overlap
- ✅ WAV file format with proper headers
- ✅ Silence detection (10-second threshold)
- ✅ Real-time recording timer on lock screen
- ✅ Pause/Resume/Stop controls in notification
- ✅ Low storage check and graceful stop
- ✅ Process-death recovery with session state persistence

### 2. Edge Case Handling
- ✅ **Phone Calls**: Auto-pause on incoming/outgoing calls
- ✅ **Audio Focus Loss**: Pause when other apps take audio focus
- ✅ **Headset/Bluetooth Changes**: Continue recording with source change notification
- ✅ **Low Storage**: Check before start and monitor during recording
- ✅ **Process Death**: Persist state and resume via WorkManager

### 3. Transcription (Mock)
- ✅ Deterministic mock transcription provider
- ✅ Automatic chunk-by-chunk processing
- ✅ Ordered transcript segments in Room database
- ✅ Retry policy with exponential backoff
- ✅ Network requirement constraints

### 4. Summary Generation (Mock Streaming)
- ✅ Streaming summary updates (title, text, action items, key points)
- ✅ Progressive UI updates as data streams
- ✅ Foreground WorkManager for process survival
- ✅ Retry mechanism on failure
- ✅ Structured JSON storage for action items and key points

### 5. User Interface
- ✅ **Dashboard**: List all meetings with status indicators
- ✅ **Recording Screen**: Timer, status, pause/resume/stop controls, chunk list
- ✅ **Summary Screen**: Title, summary text, action items, key points with streaming updates
- ✅ **Settings Screen**: Provider selection (mock vs real) and API key input

## 📦 Project Structure

```
app/src/main/java/com/abhay/voiceapp/
├── VoiceApp.kt                          # Application class with Hilt
├── audio/
│   ├── AudioRecorder.kt                 # AudioRecord wrapper
│   ├── WavFileWriter.kt                 # WAV file creation
│   ├── ChunkManager.kt                  # 30s chunks with 2s overlap
│   └── SilenceDetector.kt               # 10s silence detection
├── service/
│   └── RecordingService.kt              # Foreground service with notifications
├── worker/
│   ├── FinalizeChunkWorker.kt           # Convert temp to WAV
│   ├── TranscriptionWorker.kt           # Mock transcription
│   └── SummaryWorker.kt                 # Mock streaming summary
├── data/
│   ├── entity/
│   │   ├── Meeting.kt                   # Meeting entity
│   │   ├── Chunk.kt                     # Audio chunk entity
│   │   ├── TranscriptSegment.kt         # Transcript segment entity
│   │   ├── Summary.kt                   # Summary entity
│   │   └── SessionState.kt              # Recovery state entity
│   ├── dao/
│   │   ├── MeetingDao.kt                # Meeting DAO
│   │   ├── ChunkDao.kt                  # Chunk DAO
│   │   ├── TranscriptSegmentDao.kt      # Transcript DAO
│   │   ├── SummaryDao.kt                # Summary DAO
│   │   └── SessionStateDao.kt           # Session state DAO
│   ├── database/
│   │   ├── AppDatabase.kt               # Room database
│   │   └── Converters.kt                # Type converters
│   └── repository/
│       ├── Repositories.kt              # Repository interfaces
│       └── RepositoryImpl.kt            # Repository implementations
├── api/
│   ├── TranscriptionApi.kt              # Transcription API interface
│   ├── MockTranscriptionProvider.kt     # Mock implementation
│   ├── SummaryApi.kt                    # Summary API interface
│   └── MockSummaryProvider.kt           # Mock streaming implementation
├── ui/
│   ├── MainActivity.kt                  # Main activity with navigation
│   ├── navigation/
│   │   └── Screen.kt                    # Navigation routes
│   ├── screen/
│   │   ├── DashboardScreen.kt           # Meeting list screen
│   │   ├── RecordingScreen.kt           # Recording control screen
│   │   ├── SummaryScreen.kt             # Summary display screen
│   │   └── SettingsScreen.kt            # Settings screen
│   ├── viewmodel/
│   │   ├── DashboardViewModel.kt        # Dashboard ViewModel
│   │   ├── RecordingViewModel.kt        # Recording ViewModel
│   │   └── SummaryViewModel.kt          # Summary ViewModel
│   └── theme/
│       ├── Color.kt                     # Material3 colors
│       ├── Theme.kt                     # Material3 theme
│       └── Type.kt                      # Typography
├── di/
│   ├── DatabaseModule.kt                # Database injection
│   ├── RepositoryModule.kt              # Repository injection
│   └── ApiModule.kt                     # API injection (mock by default)
└── util/
    ├── StorageManager.kt                # Storage check utilities
    └── TimeFormatter.kt                 # Time formatting utilities

app/src/test/java/com/abhay/voiceapp/
└── audio/
    ├── SilenceDetectorTest.kt           # Silence detection tests
    └── ChunkManagerTest.kt              # Chunking logic tests
```

## 🚀 Getting Started

### Prerequisites
- Android Studio Hedgehog (2023.1.1) or later
- JDK 17
- Android SDK with minimum API 24

### Build and Run

1. **Clone the repository**
   ```bash
   git clone https://github.com/AbhayRathi/androidvoiceapp.git
   cd androidvoiceapp
   ```

2. **Open in Android Studio**
   - Open Android Studio
   - Select "Open an Existing Project"
   - Navigate to the cloned directory
   - Wait for Gradle sync to complete

3. **Run the app**
   - Connect an Android device or start an emulator (API 24+)
   - Click the "Run" button or press Shift+F10
   - Grant required permissions when prompted:
     - Microphone access
     - Notification access
     - Phone state (for call detection)

### Running with Mock Providers (Default)

The app is configured to use **mock providers by default**, so you can test all features without any API keys:

1. Launch the app
2. Tap the "+" button to start a new recording
3. Grant microphone permissions when prompted
4. Start recording - the mock transcription will automatically process chunks
5. Stop recording - the mock summary will be generated automatically
6. View the generated summary with title, summary text, action items, and key points

**Mock Behavior**:
- **Transcription**: Returns deterministic sample text based on chunk index with 2-3 second delay
- **Summary**: Streams structured summary data progressively over 8-10 seconds
- **No API calls**: Everything runs locally for testing

## 🔌 Plugging in Real API Providers

To integrate real transcription and summary APIs in the future:

### 1. Update Dependencies (if using Retrofit for APIs)

Add to `app/build.gradle.kts`:
```kotlin
// Retrofit for API calls
implementation("com.squareup.retrofit2:retrofit:2.9.0")
implementation("com.squareup.retrofit2:converter-gson:2.9.0")
implementation("com.squareup.okhttp3:okhttp:4.11.0")
implementation("com.squareup.okhttp3:logging-interceptor:4.11.0")
```

### 2. Create Real Implementations

Create new files:

**RealTranscriptionProvider.kt**:
```kotlin
class RealTranscriptionProvider @Inject constructor(
    private val apiKeyProvider: ApiKeyProvider
) : TranscriptionApi {
    override suspend fun transcribe(audioFile: File, chunkIndex: Int): TranscriptionResult {
        // Implement OpenAI Whisper or Google Gemini API call
        val apiKey = apiKeyProvider.getTranscriptionApiKey()
        // Make API call with audioFile
        // Return TranscriptionResult
    }
}
```

**RealSummaryProvider.kt**:
```kotlin
class RealSummaryProvider @Inject constructor(
    private val apiKeyProvider: ApiKeyProvider
) : SummaryApi {
    override fun generateSummary(transcript: String): Flow<SummaryUpdate> = flow {
        // Implement streaming LLM API call (OpenAI, Google Gemini, etc.)
        val apiKey = apiKeyProvider.getSummaryApiKey()
        // Stream summary updates
        // emit(SummaryUpdate(...))
    }
}
```

### 3. Update Dependency Injection

Modify `di/ApiModule.kt`:
```kotlin
@Module
@InstallIn(SingletonComponent::class)
object ApiModule {
    
    @Provides
    @Singleton
    fun provideTranscriptionApi(
        settingsProvider: SettingsProvider,
        mockProvider: MockTranscriptionProvider,
        realProvider: RealTranscriptionProvider
    ): TranscriptionApi {
        return if (settingsProvider.useMockProvider()) {
            mockProvider
        } else {
            realProvider
        }
    }
    
    @Provides
    @Singleton
    fun provideSummaryApi(
        settingsProvider: SettingsProvider,
        mockProvider: MockSummaryProvider,
        realProvider: RealSummaryProvider
    ): SummaryApi {
        return if (settingsProvider.useMockProvider()) {
            mockProvider
        } else {
            realProvider
        }
    }
}
```

### 4. Store API Keys Securely

Use DataStore or EncryptedSharedPreferences:

```kotlin
@Singleton
class ApiKeyProvider @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val dataStore = context.createDataStore("api_keys")
    
    suspend fun saveTranscriptionApiKey(key: String) {
        // Store encrypted
    }
    
    suspend fun getTranscriptionApiKey(): String {
        // Retrieve and decrypt
    }
}
```

### 5. Configure in Settings

Users can then:
1. Go to Settings
2. Toggle "Real API Provider"
3. Enter their API key
4. Save

The app will automatically use the real provider for new recordings.

## 🧪 Testing

### Run Unit Tests
```bash
./gradlew test
```

Tests included:
- `SilenceDetectorTest.kt`: Tests for silence detection logic
- `ChunkManagerTest.kt`: Tests for chunking calculations

### Manual Testing Checklist

- [ ] Start recording and verify notification appears
- [ ] Verify timer updates every second
- [ ] Pause recording and verify status change
- [ ] Resume recording and verify continuation
- [ ] Stop recording and verify finalization
- [ ] Make a phone call during recording - verify auto-pause
- [ ] Play music to lose audio focus - verify pause
- [ ] Connect/disconnect headphones - verify continued recording
- [ ] Kill app during recording - verify recovery on restart
- [ ] Complete recording and verify transcription starts
- [ ] Verify summary generation after transcription
- [ ] View summary with all sections populated
- [ ] Test retry on summary failure

## 🔒 Security Considerations

- **No API Keys Committed**: The repository contains no API keys
- **Mock by Default**: All functionality works without external services
- **Secure Storage**: Real API keys should be stored using EncryptedSharedPreferences
- **Permissions**: Requests minimal required permissions
- **Foreground Service**: Uses FOREGROUND_SERVICE_TYPE_MICROPHONE for Android 14+

## 📝 Key Implementation Details

### Chunking with Overlap
- Records audio in 30-second chunks
- Maintains 2-second overlap buffer between chunks
- Ensures no speech is cut off at chunk boundaries
- Overlap data prepended to next chunk

### Process Death Recovery
- Session state persisted to Room database
- On service destruction, enqueues FinalizeChunkWorker
- Worker ensures last chunk is properly finalized
- Transcription continues automatically on app restart

### Streaming Summary
- SummaryWorker runs as foreground work
- Streams updates progressively to Room database
- UI observes database changes via Flow
- Survives app kill during generation

### WorkManager Chain
```
FinalizeChunkWorker (per chunk)
    ↓
TranscriptionWorker (per chunk, ordered)
    ↓
SummaryWorker (per meeting, after all chunks transcribed)
```

## 🐛 Known Limitations

- Mock providers return deterministic sample data (not actual transcriptions)
- Real API integration requires additional implementation
- No speaker diarization in current mock implementation
- Summary format is fixed in mock (not customizable)

## 🔮 Future Enhancements

- Real API provider integration (OpenAI Whisper, Google Gemini)
- Speaker diarization support
- Export summaries as PDF/TXT
- Cloud backup for recordings
- Customizable summary templates
- Voice activity detection for smarter chunking
- Multi-language support

## 📄 License

This is a take-home assignment implementation. 

## 👤 Author

Implementation by the Copilot coding agent for the Android Developer take-home assignment.

## 🙏 Acknowledgments

- Android Jetpack libraries
- Hilt for dependency injection
- Material3 design system
- Kotlin coroutines and Flow

---

**Note**: This implementation focuses on demonstrating architectural decisions, edge case handling, and mock provider flows. Real API integration is designed to be pluggable without major refactoring.
