# Dependency Injection (Hilt) Architecture

## Overview

This document describes the Dependency Injection setup using Hilt, focusing on API provider bindings and WorkManager integration.

## Module Structure

### 1. DatabaseModule
**Location:** `di/DatabaseModule.kt`

Provides single canonical bindings for:
- `AppDatabase` - @Singleton Room database instance
- All DAO interfaces (MeetingDao, ChunkDao, TranscriptSegmentDao, SummaryDao, SessionStateDao)

**Key Points:**
- Database is created once and reused throughout app lifecycle
- DAOs are provided as non-singleton (new instance per injection, but backed by same DB)

### 2. RepositoryModule
**Location:** `di/RepositoryModule.kt`

Binds repository implementations to their interfaces:
- `MeetingRepository` → `MeetingRepositoryImpl`
- `ChunkRepository` → `ChunkRepositoryImpl`
- `TranscriptRepository` → `TranscriptRepositoryImpl`
- `SummaryRepository` → `SummaryRepositoryImpl`
- `SessionStateRepository` → `SessionStateRepositoryImpl`

### 3. API Modules (New Architecture)

#### DebugApiModule (Mock Providers)
**Location:** `di/ApiModule.kt` (renamed from ApiModule)

Binds Mock API implementations with `@MockApi` qualifier:
```kotlin
@Binds @Singleton @MockApi
abstract fun bindMockTranscriptionApi(impl: MockTranscriptionApi): TranscriptionApi

@Binds @Singleton @MockApi
abstract fun bindMockSummaryApi(impl: MockSummaryApi): SummaryApi
```

**Purpose:** Always provides mock implementations for testing and development.

#### ProductionApiModule (Real Providers)
**Location:** `di/ProductionApiModule.kt`

**Status:** Stub module ready for real API implementations

**How to Add Real APIs:**
```kotlin
@Provides
@Singleton
@ProductionApi
fun provideRealTranscriptionApi(
    securePrefs: SecurePreferences
): TranscriptionApi? {
    val apiKey = securePrefs.getApiKey()
    return if (apiKey.isNotEmpty()) {
        when (securePrefs.getSelectedProvider()) {
            "OpenAI" -> OpenAITranscriptionApi(apiKey)
            "Gemini" -> GeminiTranscriptionApi(apiKey)
            else -> null
        }
    } else null
}
```

#### ApiProviderModule (Provider Selection)
**Location:** `di/ApiProviderModule.kt`

Provides the actual API implementation to use throughout the app:
```kotlin
@Provides
@Singleton
fun provideTranscriptionApi(
    @MockApi mockApi: TranscriptionApi
    // @ProductionApi realApi: TranscriptionApi? // Uncomment when ready
): TranscriptionApi {
    // Currently returns Mock
    // TODO: return realApi ?: mockApi (when real API is ready)
    return mockApi
}
```

**This is the single point where API selection happens.**

### 4. WorkManagerModule
**Location:** `di/WorkManagerModule.kt`

Provides WorkManager instance:
```kotlin
@Provides
@Singleton
fun provideWorkManager(@ApplicationContext context: Context): WorkManager
```

## API Qualifiers

**Location:** `di/ApiQualifiers.kt`

Two qualifiers distinguish API implementations:
- `@MockApi` - For mock/test implementations
- `@ProductionApi` - For real API implementations

## WorkManager Integration

### HiltWorkerFactory Setup

**Location:** `VoiceApp.kt`

```kotlin
@HiltAndroidApp
class VoiceApp : Application(), Configuration.Provider {
    @Inject
    lateinit var workerFactory: HiltWorkerFactory
    
    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder()
            .setWorkerFactory(workerFactory)
            .build()
}
```

**Key Points:**
- HiltWorkerFactory is injected (not manually created)
- Configuration.Provider interface ensures WorkManager uses Hilt's factory
- Single initialization point in Application class
- All workers annotated with `@HiltWorker` get DI support

### Worker Construction

All workers use assisted injection:
```kotlin
@HiltWorker
class TranscriptionWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted workerParams: WorkerParameters,
    private val transcriptionApi: TranscriptionApi, // Injected!
    // ... other dependencies
) : CoroutineWorker(context, workerParams)
```

**Verified Workers:**
1. `FinalizeChunkWorker` - @HiltWorker ✓
2. `TranscriptionWorker` - @HiltWorker ✓
3. `SummaryWorker` - @HiltWorker ✓

## Dependency Flow

```
Application (VoiceApp)
  ↓
HiltWorkerFactory (injected)
  ↓
Workers (@HiltWorker)
  ↓
Repositories (injected)
  ↓
APIs (injected via ApiProviderModule)
  ↓
TranscriptionApi / SummaryApi interfaces
  ↓
MockTranscriptionApi / MockSummaryApi (@MockApi)
  OR
RealTranscriptionApi / RealSummaryApi (@ProductionApi)
```

## Benefits of This Architecture

### 1. Single Canonical Bindings
✅ Each API type has exactly one active binding at runtime
✅ No multiple-binding crashes
✅ Type-safe at compile time

### 2. Easy API Switching
✅ Change one line in ApiProviderModule to switch between Mock and Production
✅ No need to modify workers or other code
✅ Conditional logic based on API key presence

### 3. Testability
✅ Mock APIs always available for tests
✅ Can inject different implementations for different test scenarios
✅ Workers can be tested in isolation

### 4. Maintainability
✅ Clear separation between Mock and Production modules
✅ Production module stub has integration guide in comments
✅ Single source of truth for API selection

## Integration Checklist for Real APIs

When adding real API implementations:

- [ ] Create real API implementation class (e.g., `OpenAITranscriptionApi`)
- [ ] Implement `TranscriptionApi` or `SummaryApi` interface
- [ ] Add provider function in `ProductionApiModule.kt`
- [ ] Annotate with `@ProductionApi` qualifier
- [ ] Update `ApiProviderModule.kt` to conditionally use real API
- [ ] Add null check for API key via `SecurePreferences`
- [ ] Test with real API key via Settings screen
- [ ] Verify workers receive real implementation correctly

## Testing

### Unit Tests
Mock implementations can be easily swapped:
```kotlin
@Test
fun testWorkerWithMockApi() {
    val mockApi = MockTranscriptionApi()
    val worker = TranscriptionWorker(..., mockApi, ...)
    // Test worker logic
}
```

### Integration Tests
Hilt provides test modules:
```kotlin
@UninstallModules(ApiProviderModule::class)
@HiltAndroidTest
class TranscriptionWorkerTest {
    @BindValue
    lateinit var testApi: TranscriptionApi
    
    @Before
    fun setup() {
        testApi = FakeTranscriptionApi()
    }
}
```

## Troubleshooting

### Issue: Multiple bindings for TranscriptionApi
**Cause:** Both Mock and Production modules provide unqualified binding
**Fix:** Ensure qualifiers (@MockApi, @ProductionApi) are used and ApiProviderModule selects one

### Issue: Workers not getting dependencies injected
**Cause:** Missing @HiltWorker annotation or HiltWorkerFactory not configured
**Fix:** Verify Application class implements Configuration.Provider and injects HiltWorkerFactory

### Issue: Circular dependency
**Cause:** Repository depends on API, API depends on Repository
**Fix:** APIs should not depend on repositories; use one-way dependency flow

## Migration Notes

### From Old to New Architecture

**Before:**
```kotlin
@Provides
fun provideMockTranscriptionApi(): MockTranscriptionApi
```

**After:**
```kotlin
@Binds @MockApi
fun bindMockTranscriptionApi(impl: MockTranscriptionApi): TranscriptionApi

@Provides
fun provideTranscriptionApi(@MockApi mock: TranscriptionApi): TranscriptionApi
```

**Changes in Workers:**
- Replace `MockTranscriptionApi` parameter with `TranscriptionApi`
- No other code changes needed
- Hilt resolves correct implementation automatically

## Security Considerations

### API Key Storage
- Use `SecurePreferences` helper (wraps EncryptedSharedPreferences)
- Never hardcode API keys in code
- Check for non-empty key before creating real API instance
- Fallback to Mock if no key available

### Provider Selection
- User selects provider via Settings screen
- Selection stored securely with API key
- ApiProviderModule reads from secure storage
- No plaintext storage of sensitive data

## References

- [Hilt Documentation](https://dagger.dev/hilt/)
- [WorkManager Hilt Integration](https://developer.android.com/training/dependency-injection/hilt-jetpack#workmanager)
- [EncryptedSharedPreferences](https://developer.android.com/reference/androidx/security/crypto/EncryptedSharedPreferences)
