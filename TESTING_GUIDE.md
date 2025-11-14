# End-to-End Testing Guide

This document explains how to run the comprehensive end-to-end tests for the Android Voice Recording App.

## Test Overview

The test suite includes:

1. **End-to-End Integration Tests** (`EndToEndIntegrationTest.kt`)
   - Complete data flow from meeting creation to summary generation
   - Session state persistence and recovery
   - Chunk transcription ordering
   - Mock API determinism

2. **Data Layer Unit Tests** (`DataLayerUnitTest.kt`)
   - Mock API functionality
   - Entity relationships
   - API determinism and variation

3. **Silence Detector Unit Tests** (`SilenceDetectorTest.kt`)
   - Silence detection logic
   - Threshold handling
   - Reset functionality

## Running the Tests

### Option 1: Using Gradle (Command Line)

#### Run All Unit Tests (JVM)
```bash
./gradlew test
```

This runs tests in:
- `app/src/test/java/com/androidvoiceapp/DataLayerUnitTest.kt`
- `app/src/test/java/com/androidvoiceapp/util/SilenceDetectorTest.kt`

#### Run All Android Instrumentation Tests (Requires Device/Emulator)
```bash
./gradlew connectedAndroidTest
```

This runs tests in:
- `app/src/androidTest/java/com/androidvoiceapp/EndToEndIntegrationTest.kt`

#### Run Specific Test Class
```bash
# Unit test
./gradlew test --tests com.androidvoiceapp.DataLayerUnitTest

# Android test
./gradlew connectedAndroidTest --tests com.androidvoiceapp.EndToEndIntegrationTest
```

#### Run Specific Test Method
```bash
./gradlew test --tests com.androidvoiceapp.DataLayerUnitTest.testMockTranscriptionApiReturnsSegments
```

### Option 2: Using Android Studio

1. Open the project in Android Studio
2. Navigate to the test file in Project view
3. Right-click on the test class or method
4. Select "Run [TestName]"

**For instrumentation tests**: Make sure you have a device connected or emulator running.

### Option 3: Run Tests with Reports

```bash
# Generate HTML test reports
./gradlew test --info

# View report at:
# app/build/reports/tests/testDebugUnitTest/index.html
```

## Test Coverage

### EndToEndIntegrationTest

#### Test 1: `testCompleteMeetingFlow`
**What it tests:**
- ✅ Meeting creation and retrieval
- ✅ Audio chunk creation (3 chunks)
- ✅ Chunk finalization
- ✅ Mock transcription API integration
- ✅ Transcript segment persistence
- ✅ Chronological ordering of segments
- ✅ Mock summary API streaming
- ✅ Summary progress updates
- ✅ Meeting completion
- ✅ Data integrity verification

**Expected Output:**
```
=== Starting End-to-End Integration Test ===

1. Creating meeting...
✓ Meeting created with ID: 1
✓ Meeting verified in database

2. Creating audio chunks...
✓ Chunk 0 created and finalized (ID: 1)
✓ Chunk 1 created and finalized (ID: 2)
✓ Chunk 2 created and finalized (ID: 3)
✓ All chunks verified in database

3. Transcribing chunks...
✓ Chunk 0 transcribed (1 segments)
✓ Chunk 1 transcribed (1 segments)
✓ Chunk 2 transcribed (1 segments)
✓ Total transcript segments: 3
✓ Transcript segments are properly ordered

4. Generating summary...
✓ Summary update 1: 10% complete
✓ Summary update 2: 30% complete
✓ Summary update 3: 50% complete
✓ Summary update 4: 70% complete
✓ Summary update 5: 90% complete
✓ Summary update 6: 100% complete
✓ Summary generation complete

5. Finalizing meeting...
✓ Meeting marked as completed

6. Verifying data integrity...
✓ Data integrity verified

=== End-to-End Integration Test PASSED ===
Summary:
  - Meeting ID: 1
  - Chunks created: 3
  - Transcript segments: 3
  - Summary status: completed
  - Summary title: Meeting Discussion Summary
```

#### Test 2: `testSessionStatePersistence`
**What it tests:**
- ✅ Session state creation
- ✅ Session state retrieval
- ✅ Session state updates (pause/resume simulation)
- ✅ Session state cleanup

#### Test 3: `testChunkTranscriptionOrdering`
**What it tests:**
- ✅ Out-of-order chunk creation
- ✅ Proper chronological ordering of transcripts
- ✅ Ordering by startTime regardless of creation order

#### Test 4: `testMockApiDeterminism`
**What it tests:**
- ✅ Mock API returns consistent results for same input
- ✅ Deterministic behavior for testing

### DataLayerUnitTest

#### Test 1: `testMockTranscriptionApiReturnsSegments`
**What it tests:**
- ✅ Mock API returns valid transcript segments
- ✅ Segment contains correct IDs and text

#### Test 2: `testMockSummaryApiStreamsUpdates`
**What it tests:**
- ✅ Summary API emits multiple updates
- ✅ Progress increases monotonically
- ✅ Final update is marked complete
- ✅ All sections are populated

#### Test 3: `testTranscriptionApiDeterminism`
**What it tests:**
- ✅ Same input produces same output
- ✅ API is deterministic for testing

#### Test 4: `testTranscriptionApiSequenceVariation`
**What it tests:**
- ✅ Different sequence numbers produce varied output
- ✅ API cycles through sample texts

#### Test 5: `testEntityRelationships`
**What it tests:**
- ✅ Entity foreign key relationships
- ✅ Meeting → Chunk → Segment hierarchy
- ✅ Meeting → Summary relationship

### SilenceDetectorTest

All 6 tests verify:
- ✅ Silence detection with threshold
- ✅ Loud audio detection
- ✅ Reset on loud sample
- ✅ Manual reset
- ✅ Boundary conditions
- ✅ Mixed amplitude handling

## Interpreting Test Results

### Success
All tests should pass with output showing:
- ✅ Green checkmarks
- No errors or failures
- Expected output messages

### Common Issues

#### "No connected devices"
**Solution**: Start an emulator or connect a physical device for instrumentation tests

#### "Test failed: Unable to resolve configuration"
**Solution**: Run `./gradlew clean` and rebuild

#### "Database locked"
**Solution**: Tests use in-memory database, this shouldn't happen. Check for parallel test execution.

## Test Data Flow Verification

The end-to-end test verifies this complete flow:

```
1. Create Meeting
   ↓
2. Create 3 Audio Chunks (30s each with 2s overlap)
   ↓
3. Finalize Chunks
   ↓
4. Transcribe Each Chunk (Mock API)
   ↓
5. Verify Transcript Segments (Ordered by Time)
   ↓
6. Generate Summary (Mock API with Streaming)
   ↓
7. Verify Summary Updates (6 progressive updates)
   ↓
8. Mark Meeting Complete
   ↓
9. Verify Data Integrity (All relationships intact)
```

## Manual Verification

After tests pass, you can manually verify in the app:

1. **Launch app** on device/emulator
2. **Start a meeting** - Creates meeting in DB
3. **Record for ~90s** - Creates 3 chunks with overlap
4. **Stop recording** - Triggers workers
5. **View chunks** - All should show "transcribed" status
6. **View summary** - Should show streaming updates
7. **Navigate away and back** - Data persists

## Continuous Integration

To run tests in CI/CD pipeline:

```yaml
# Example GitHub Actions
- name: Run Unit Tests
  run: ./gradlew test --stacktrace

- name: Run Integration Tests
  run: ./gradlew connectedAndroidTest --stacktrace
```

## Test Maintenance

When modifying the app:

1. **Update tests** if data model changes
2. **Add new tests** for new features
3. **Run tests** before committing
4. **Keep tests fast** - Use mocks where possible
5. **Document changes** in test comments

## Performance Expectations

- **Unit Tests**: < 5 seconds total
- **Integration Tests**: < 30 seconds total
- **All Tests**: < 1 minute total

## Troubleshooting

### Test Timeout
Increase timeout in test annotation:
```kotlin
@Test(timeout = 30000) // 30 seconds
fun myTest() { ... }
```

### Flaky Tests
- Check for race conditions in coroutines
- Ensure proper test isolation
- Use `runTest` for coroutine tests

### Memory Issues
- Tests use in-memory database (cleared after each test)
- No persistent files (temp files deleted)
- Should not cause memory leaks

## Coverage Report

Generate code coverage report:

```bash
./gradlew testDebugUnitTestCoverage
```

View report at:
`app/build/reports/coverage/test/debug/index.html`

## Next Steps

After tests pass:

1. ✅ All data layer components verified
2. ✅ Mock APIs functioning correctly
3. ✅ Database operations working
4. ✅ Ready for manual UI testing
5. ✅ Ready for real API integration

---

**Note**: The integration tests require an Android device or emulator. Unit tests can run on JVM without Android dependencies.
