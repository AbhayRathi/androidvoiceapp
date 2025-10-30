# Android Voice Recording App

A complete voice recording application with transcription and summary generation capabilities. Built with Kotlin, Jetpack Compose, and MVVM architecture.

## 🎯 Features

### Core Functionality
- **Robust Audio Recording**: Foreground service with 30-second WAV chunks and 2-second overlap
- **Automatic Transcription**: Mock and pluggable real API support (OpenAI Whisper, Google Gemini)
- **Smart Summaries**: Streaming summary generation with structured output (Title, Summary, Action Items, Key Points)
- **Modern UI**: 100% Jetpack Compose with Material Design 3

### Edge Case Handling
- ✅ Phone call interruptions (automatic pause/resume)
- ✅ Audio focus management (pause when other apps need audio)
- ✅ Headset/Bluetooth changes (seamless continuation)
- ✅ Low storage detection (graceful stop)
- ✅ Process death recovery (state persistence and worker recovery)
- ✅ Silence detection (10-second threshold with warning)

### Technical Highlights
- **Architecture**: MVVM with Repository pattern
- **Dependency Injection**: Hilt
- **Database**: Room with Flow-based reactive queries
- **Background Work**: WorkManager with retry policies and ordering
- **Audio Format**: PCM 16-bit mono WAV (16kHz sample rate)
- **Min SDK**: API 24 (Android 7.0)
- **Target SDK**: API 34

## 📋 Requirements

- Android Studio Hedgehog (2023.1.1) or later
- JDK 17
- Android SDK with API 34
- Minimum device: Android 7.0 (API 24)

## 🚀 Getting Started

### 1. Clone the Repository

```bash
git clone https://github.com/AbhayRathi/androidvoiceapp.git
cd androidvoiceapp
```

### 2. Build the Project

```bash
./gradlew build
```

### 3. Run on Emulator/Device

```bash
./gradlew installDebug
```

Or use Android Studio:
1. Open project in Android Studio
2. Select your device/emulator
3. Click **Run** (Shift+F10)

### 4. Grant Permissions

On first launch, the app will request:
- 🎤 Microphone permission (required for recording)
- 🔔 Notification permission (Android 13+, for foreground service)
- 📞 Phone state permission (optional, for call interruption handling)

## 🎬 Demo Script

### Starting a Recording

1. Launch the app
2. Tap the **+** button on the Dashboard
3. Enter a meeting title (or use auto-generated one)
4. Tap **Start Recording**
5. The app will:
   - Start a foreground service with persistent notification
   - Begin recording 30-second WAV chunks with 2-second overlap
   - Display a live timer and status on the Recording screen

### Recording Controls

- **Pause**: Temporarily pause recording
- **Resume**: Resume after pause
- **Stop**: End recording and trigger transcription

### Viewing Progress

- Navigate to the **Recording screen** to see:
  - Live timer
  - Current status (Recording, Paused, etc.)
  - List of audio chunks with their transcription status
  
### Viewing Summary

- After recording stops, transcription runs automatically
- Once complete, tap **View Summary** or navigate from Dashboard
- Watch the summary generate in real-time (streaming updates)
- See structured output:
  - Title
  - Summary
  - Action Items
  - Key Points

### Testing Edge Cases

#### Simulated Phone Call
*Note: Real phone call testing requires physical device*

1. Start a recording
2. Make or receive a call
3. Observe:
   - Recording pauses automatically
   - Notification shows "Paused - Phone call"
   - Recording resumes when call ends

#### Low Storage
1. Fill device storage to near capacity
2. Start recording
3. App will detect and stop gracefully with message

#### Silence Detection
1. Start recording in a quiet environment
2. After 10 seconds of silence:
   - Notification shows "No audio detected - Check microphone"
   - Recording continues but alerts user

#### Process Death
1. Start recording
2. Kill the app (swipe away from Recent Apps)
3. Observe:
   - Service continues in background
   - State is preserved
   - Workers finalize partial chunks

## 📱 UI Screens

### 1. Dashboard
- List of all meetings
- Status badges (Recording, Stopped, Processing, Completed)
- Quick navigation to Recording or Summary screens
- Settings access

### 2. Recording Screen
- Live timer display
- Current status indicator
- Pause/Resume/Stop controls
- Real-time chunk list with status

### 3. Summary Screen
- Streaming summary display
- Structured sections (Title, Summary, Action Items, Key Points)
- Progress indicator during generation
- Retry option on failure

### 4. Settings Screen
- API provider selection (Mock, OpenAI, Gemini)
- API key entry
- Integration instructions

## 🔌 API Integration

### Using Mock APIs (Default)

The app ships with mock implementations that work without API keys:
- **MockTranscriptionApi**: Returns deterministic sample transcripts
- **MockSummaryApi**: Simulates streaming with structured output

### Plugging Real APIs

#### 1. Open Settings Screen

Navigate to Settings from the Dashboard

#### 2. Select Provider

Choose between:
- OpenAI Whisper (real transcription)
- Google Gemini (real transcription)

#### 3. Enter API Key

Input your API key for the selected provider

#### 4. Save Settings

Settings are stored securely (ready for implementation with EncryptedSharedPreferences)

### Implementation Notes for Real APIs

To implement real API integration:

1. **Create API Interface** (e.g., `OpenAIApi.kt`)
```kotlin
interface OpenAIApi {
    suspend fun transcribe(audioFile: File): TranscriptResponse
}
```

2. **Update DI Module** (`ApiModule.kt`)
```kotlin
@Provides
fun provideTranscriptionApi(
    settings: SettingsRepository
): TranscriptionApi {
    return when (settings.getProvider()) {
        "OpenAI" -> OpenAITranscriptionApi(settings.getApiKey())
        "Gemini" -> GeminiTranscriptionApi(settings.getApiKey())
        else -> MockTranscriptionApi()
    }
}
```

3. **Update Workers**

Workers already accept API implementations via DI. Simply replace the mock with real implementation.

## 🏗️ Architecture

### Layers

```
app/
├── data/
│   ├── room/          # Database entities and DAOs
│   └── repository/    # Repository interfaces and implementations
├── service/           # RecordingService (foreground)
├── workers/           # WorkManager workers
│   ├── FinalizeChunkWorker
│   ├── TranscriptionWorker
│   └── SummaryWorker
├── audio/             # Audio recording components
│   ├── AudioRecordWrapper
│   └── WavWriter
├── util/              # Utility classes
│   ├── SilenceDetector
│   ├── StorageChecker
│   └── AudioFocusHelper
├── ui/
│   ├── screens/       # Compose screens
│   ├── viewmodels/    # ViewModels
│   ├── navigation/    # Navigation setup
│   └── theme/         # Material theming
├── api/mock/          # Mock API implementations
└── di/                # Hilt modules
```

### Data Flow

```
UI (Compose) 
  ↕️ Flow
ViewModel
  ↕️
Repository
  ↕️
Room DAO / WorkManager
```

### Recording Flow

```
User taps Start
  ↓
RecordingService starts
  ↓
AudioRecordWrapper captures audio
  ↓
WavWriter writes 30s chunks (with 2s overlap)
  ↓
Chunk saved to DB with status "recording"
  ↓
FinalizeChunkWorker updates to "finalized"
  ↓
TranscriptionWorker processes chunk
  ↓
Transcript segments saved to DB
  ↓
When all chunks transcribed → SummaryWorker
  ↓
Summary streams to DB (UI updates in real-time)
```

## 🧪 Testing

### Run Unit Tests

```bash
./gradlew test
```

Current test coverage:
- ✅ SilenceDetector (6 test cases)
- More tests can be added for other components

### Manual Testing Checklist

- [ ] Start/Stop recording
- [ ] Pause/Resume recording
- [ ] Check chunk creation (30s each)
- [ ] Verify transcription completion
- [ ] Watch summary streaming
- [ ] Test phone call interruption (physical device)
- [ ] Test low storage scenario
- [ ] Verify process death recovery
- [ ] Test silence detection
- [ ] Check notification actions
- [ ] Navigate between screens

## 🔧 Configuration

### Audio Settings

Located in `RecordingService.kt`:
```kotlin
private const val CHUNK_DURATION_MS = 30000L  // 30 seconds
private const val OVERLAP_DURATION_MS = 2000L  // 2 seconds
private const val SAMPLE_RATE = 16000         // 16 kHz
```

### Silence Detection

Located in `util/SilenceDetector.kt`:
```kotlin
SilenceDetector(
    silenceThresholdAmplitude: Int = 500,  // Threshold
    silenceDurationMs: Long = 10000        // 10 seconds
)
```

### Storage Check

Located in `util/StorageChecker.kt`:
```kotlin
private const val MIN_STORAGE_BYTES = 100 * 1024 * 1024L // 100 MB
```

## 📦 Building Release APK

### Debug APK

```bash
./gradlew assembleDebug
```

Output: `app/build/outputs/apk/debug/app-debug.apk`

### Release APK

1. Create a keystore (first time only):
```bash
keytool -genkey -v -keystore my-release-key.jks -keyalg RSA -keysize 2048 -validity 10000 -alias my-key-alias
```

2. Update `app/build.gradle.kts`:
```kotlin
android {
    signingConfigs {
        create("release") {
            storeFile = file("../my-release-key.jks")
            storePassword = "your-password"
            keyAlias = "my-key-alias"
            keyPassword = "your-password"
        }
    }
    buildTypes {
        release {
            signingConfig = signingConfigs.getByName("release")
            // ... other config
        }
    }
}
```

3. Build release APK:
```bash
./gradlew assembleRelease
```

Output: `app/build/outputs/apk/release/app-release.apk`

## 🐛 Troubleshooting

### Build Errors

**Issue**: Gradle sync fails
- **Solution**: Ensure JDK 17 is configured in Android Studio
- File → Project Structure → SDK Location → JDK location

**Issue**: Room schema export errors
- **Solution**: Already disabled in build.gradle (`exportSchema = false`)

### Runtime Issues

**Issue**: Recording doesn't start
- **Solution**: Check microphone permission is granted
- Verify notification permission on Android 13+

**Issue**: Transcription fails
- **Solution**: Check logs for worker failures
- Verify chunks are being created and finalized

**Issue**: Summary not generating
- **Solution**: Ensure transcription completed first
- Check WorkManager logs

### Permission Denials

**Issue**: Microphone permission denied
- **Solution**: Go to App Settings → Permissions → Enable Microphone

**Issue**: Phone state permission denied
- **Solution**: Phone call handling will be disabled but recording continues

## 📚 Dependencies

Key libraries used:
- **Jetpack Compose**: 2023.10.01 BOM
- **Hilt**: 2.48
- **Room**: 2.6.1
- **WorkManager**: 2.9.0
- **Coroutines**: 1.7.3
- **Navigation Compose**: 2.7.6
- **Kotlinx Serialization**: 1.6.2

## 🤝 Contributing

This is a take-home assignment implementation. For production use:

1. Fork the repository
2. Create a feature branch
3. Make your changes
4. Add tests
5. Submit a pull request

## 📄 License

This project is created as part of an Android Developer take-home assignment.

## 🔮 Future Enhancements

- [ ] Real OpenAI Whisper integration
- [ ] Real Google Gemini integration
- [ ] EncryptedSharedPreferences for API keys
- [ ] Audio compression (e.g., Opus codec)
- [ ] Export recordings
- [ ] Share summaries
- [ ] Search functionality
- [ ] Dark mode preference
- [ ] Multiple language support
- [ ] Custom chunk duration setting
- [ ] Audio visualization
- [ ] Speaker diarization
- [ ] Cloud sync

## 📞 Support

For issues or questions related to this implementation, please open an issue on GitHub.

---

**Built with ❤️ using Kotlin & Jetpack Compose**
