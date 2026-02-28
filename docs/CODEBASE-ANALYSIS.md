# SpeekEZ Codebase Analysis Report

**Date:** 2026-02-28
**Scope:** Full architectural, class design, functional, and performance review
**Goal:** Production-scale standalone app readiness

---

## Table of Contents

1. [Executive Summary](#1-executive-summary)
2. [Architectural Review](#2-architectural-review)
3. [Class Design Review](#3-class-design-review)
4. [Functional Progression Review](#4-functional-progression-review)
5. [Performance Review](#5-performance-review)
6. [Security Review](#6-security-review)
7. [Test Coverage Review](#7-test-coverage-review)
8. [Documentation Review](#8-documentation-review)
9. [Production Readiness Checklist](#9-production-readiness-checklist)
10. [Prioritized Fix List](#10-prioritized-fix-list)

---

## 1. Executive Summary

SpeekEZ is a voice-to-text Android keyboard with AI refinement, forked from FlorisBoard. The codebase is well-structured with 10 Gradle modules, clean separation of concerns, and a modern tech stack (Kotlin, Compose, Room, Retrofit, Coroutines). However, there are **critical issues** that must be addressed before production deployment.

### Severity Legend
- **P0 (Critical):** Crashes, data loss, security vulnerabilities — must fix before release
- **P1 (High):** Functional bugs, resource leaks, architectural debt — fix before production
- **P2 (Medium):** Code quality, testability, maintainability — fix in next sprint
- **P3 (Low):** Nice-to-have improvements, polish — backlog

### Summary of Findings
| Category | P0 | P1 | P2 | P3 |
|---|---|---|---|---|
| Architecture | 2 | 5 | 3 | 2 |
| Class Design | 1 | 4 | 6 | 2 |
| Functional | 3 | 6 | 5 | 3 |
| Performance | 1 | 3 | 3 | 1 |
| Security | 2 | 2 | 1 | 0 |
| Testing | 0 | 3 | 4 | 2 |
| Documentation | 0 | 0 | 3 | 0 |
| **Total** | **9** | **23** | **25** | **10** |

---

## 2. Architectural Review

### 2.1 Module Structure (Good)

```
app/          Main application — activities, screens (Compose), IME service
api/          API clients — Whisper, Claude, OpenRouter
core/         Shared utilities (ApiMode, ModelTier, NetworkUtils)
data/         Room database, entities, DAOs, seeders
security/     EncryptedSharedPreferences manager
voice/        VoiceManager, AudioHandler, VoiceStateMachine, haptics
widget/       Home screen widgets, floating widget service
lib/          FlorisBoard internal libraries (android, color, compose, kotlin, native, snygg)
```

**Verdict:** The multi-module structure is clean and enforces good boundaries. Each SpeekEZ-specific module (`api`, `core`, `data`, `security`, `voice`, `widget`) has a clear single responsibility.

### 2.2 Issues Found

#### [A-01] P0 — VoiceManager CoroutineScope never cancelled
**File:** `voice/src/main/kotlin/com/speekez/voice/VoiceManager.kt:54`
```kotlin
private val scope = CoroutineScope(Dispatchers.Main + SupervisorJob())
```
The `scope` is created but **never cancelled**. This means:
- Coroutines launched in `processAudio()` survive past the lifecycle of the keyboard service
- Memory leak: VoiceManager holds references to Context, DAOs, API clients forever
- The `init {}` block's state collector runs indefinitely

**Fix:** Add a `destroy()` method that cancels the scope, and call it from `FlorisImeService.onDestroy()` and widget lifecycle.

---

#### [A-02] P1 — No Dependency Injection framework
All dependencies are constructed manually via `lazy` fields in `VoiceManager`, and `remember { ... }` in Composables. This causes:
- Multiple instances of `EncryptedPreferencesManager` created across `ModelSettingsScreen`, `VoiceManager`, and `ApiRouterManager`
- Multiple instances of `ApiRouterManager` with separate OkHttpClient connection pools
- Hard to test in isolation (tests use reflection to inject mocks)

**Fix:** Introduce Hilt/Koin for DI. At minimum, ensure singleton instances via a centralized service locator.

---

#### [A-03] P1 — Duplicate OkHttpClient instances
**Files:** `ApiRouterManager.kt:16`, `AnthropicClaudeClient.kt:31-35`, `OpenRouterClaudeClient.kt:30-34`

Three separate `OkHttpClient` instances are created with different timeout configurations:
- `ApiRouterManager`: 30s connect, 60s read
- `AnthropicClaudeClient`: 15s connect, 30s read
- `OpenRouterClaudeClient`: 15s connect, 30s read

Each `OkHttpClient` creates its own connection pool, thread pool, and dispatcher.

**Fix:** Share a single configured OkHttpClient and pass it through to all clients via constructor injection.

---

#### [A-04] P1 — Database seeding race condition
**File:** `data/src/main/kotlin/com/speekez/data/SpeekEZDatabase.kt:57-63`
```kotlin
CoroutineScope(Dispatchers.IO + SupervisorJob()).launch {
    PresetSeeder.seedDefaultPresetsIfEmpty(instance.presetDao())
}
```
The seeder runs on a fire-and-forget coroutine scope that:
- Has no lifecycle management (never cancelled)
- Could race with the first `presetDao.getAllPresets()` call from the UI
- If the UI queries presets before seeding completes, the empty state shows momentarily

**Fix:** Use `RoomDatabase.Callback.onCreate()` for seeding, or use a `addCallback` approach that guarantees ordering.

---

#### [A-05] P1 — No ViewModel layer
All Composable screens (`DashboardScreen`, `HistoryScreen`, `SettingsScreen`, `ModelSettingsScreen`, `PresetSettingsScreen`) directly access DAOs and preferences from within `@Composable` functions using `remember { context.presetDao() }`.

This violates Android architecture guidelines:
- Database access mixed with UI layer
- No surviving configuration changes
- No testability of business logic
- State held in Composable local variables is lost on process death

**Fix:** Introduce ViewModels for each screen that encapsulate business logic and expose UI state via `StateFlow`.

---

#### [A-06] P2 — Hardcoded preference key coupling
**File:** `VoiceManager.kt:136-138`
```kotlin
context.getSharedPreferences("florisboard-app-prefs", Context.MODE_PRIVATE)
    .getBoolean("speekez__copy_to_clipboard", true)
```
The voice module directly reads from FlorisBoard's preference file by hardcoded name. This creates tight coupling between modules.

**Fix:** Pass settings as a provider/callback or use a shared preference interface.

---

#### [A-07] P2 — Two separate Context extension patterns
`data/Extensions.kt` provides `context.presetDao()` and `voice/Extensions.kt` provides `context.voiceManager()`. These use different patterns (direct vs. `Lazy`) and different access styles. Inconsistent API.

**Fix:** Standardize extension patterns or consolidate into a DI solution.

---

#### [A-08] P2 — No error boundary / crash reporting for SpeekEZ code
The app inherits FlorisBoard's `CrashUtility`, but SpeekEZ-specific code (API calls, audio processing, widget service) doesn't have structured error boundaries beyond try/catch with Log.e.

**Fix:** Add structured error reporting for production (e.g., Firebase Crashlytics, Sentry).

---

#### [A-09] P3 — Benchmark module commented out
**File:** `settings.gradle.kts` — `:benchmark` is included but appears unused.

**Fix:** Either enable and use the benchmark module or remove it to reduce build noise.

---

#### [A-10] P3 — CI pipeline doesn't run tests
**File:** `.github/workflows/android.yml` — The CI only runs `assembleDebug`, never `test` or `check`.

**Fix:** Add `./gradlew test` to the CI pipeline.

---

#### [A-11] P0 — Database migration uses RENAME COLUMN (requires API 30+)
**File:** `data/src/main/kotlin/com/speekez/data/SpeekEZDatabase.kt:42`
```kotlin
db.execSQL("ALTER TABLE presets RENAME COLUMN output_language TO output_languages")
```
`ALTER TABLE ... RENAME COLUMN` requires SQLite 3.25.0+, which is only available on Android API 30 (Android 11) and above. The app's `minSdk` is 26. On devices running Android 9 or 10, this migration will crash the app on launch.

Additionally, the column rename from `output_language` (single string) to `output_languages` (JSON array) does **not transform the existing data**. Old rows will contain a plain string (e.g., `"en"`) where a JSON array (`["en"]`) is expected, causing the `Converters.fromStringList()` TypeConverter to fail or return unexpected results.

**Fix:**
1. Replace `RENAME COLUMN` with a full table recreation strategy (create new table, copy data with transformation, drop old, rename new)
2. Transform existing data from `"en"` to `["en"]` format during migration
3. Alternatively, use Room's `autoMigrations` with `@RenameColumn` annotation

---

#### [A-12] P1 — Root project name still "FlorisBoard"
**File:** `settings.gradle.kts:17`
```kotlin
rootProject.name = "FlorisBoard"
```
The root project name hasn't been updated to "SpeekEZ". This affects Gradle build output paths, IDE project display, and artifact naming.

**Fix:** Change to `rootProject.name = "SpeekEZ"`.

---

#### [A-13] P1 — App namespace still uses FlorisBoard package
**File:** `app/build.gradle.kts:59`
```kotlin
namespace = "dev.patrickgold.florisboard"
```
The app module's namespace is still `dev.patrickgold.florisboard` instead of `com.speekez.app`. This means:
- Generated `R` class is under the old package
- AndroidManifest components reference the old namespace
- PlayStore listing will use the old package as the application ID

**Fix:** Migrate the namespace to `com.speekez.app` (requires updating all resource references).

---

#### [A-14] P1 — No overlay permission check before adding floating widget view
**File:** `widget/src/main/kotlin/com/speekez/widget/FloatingWidgetService.kt:157`
```kotlin
windowManager.addView(composeView, params)
```
The `FloatingWidgetService` uses `TYPE_APPLICATION_OVERLAY` but never checks `Settings.canDrawOverlays()` before calling `addView()`. If the permission isn't granted, this will throw a `WindowManager.BadTokenException` and crash.

**Fix:** Check `Settings.canDrawOverlays(this)` in `onCreate()` and gracefully stop the service if not granted.

---

#### [A-15] P1 — Concurrent recording conflicts from multiple entry points
Recording can be initiated from three places: the keyboard IME, the floating widget service, and the home screen widget. All three share the same `VoiceManager` singleton but there is no guard to prevent one entry point from starting a recording while another is already in progress from a different entry point.

**Fix:** Add entry-point tracking to VoiceManager and reject concurrent recording attempts from different sources with a user-facing message.

---

## 3. Class Design Review

### 3.1 API Layer

#### [C-01] P1 — `handleError()` returns `Nothing` but is called from `onFailure` (dead code pattern)
**Files:** `AnthropicClaudeClient.kt:88`, `OpenAiWhisperClient.kt:51`, `OpenRouterClaudeClient.kt:88`, `OpenRouterAudioClient.kt:99`

```kotlin
.onFailure { e ->
    if (e is HttpException) {
        handleError(e)  // throws — but onFailure's return is Unit
    }
}.getOrThrow()
```
The `handleError()` throws inside `onFailure {}`. However, `Result.onFailure` catches this new exception — the rethrown error from `handleError()` is **swallowed**, and then `.getOrThrow()` rethrows the **original** exception instead.

This means the user-friendly error messages ("Invalid API Key", "Rate Limit Exceeded") are **never delivered** to the caller. The raw `HttpException` propagates instead.

**Fix:** Move error mapping to a `mapError()` function called after `getOrThrow()`, or use `Result.fold()` instead.

---

#### [C-02] P0 — `OpenRouterAudioClient.transcribe()` loads entire audio file into memory
**File:** `OpenRouterAudioClient.kt:59`
```kotlin
val audioBytes = audioFile.readBytes()
val base64Audio = Base64.encodeToString(audioBytes, Base64.NO_WRAP)
```
A 5-minute AAC recording at 16kHz can be 2-5 MB. Base64 encoding increases this by 33%. The entire file is loaded into memory twice (raw bytes + base64 string). On low-memory devices, this can cause OOM.

**Fix:** Use streaming Base64 encoding or chunked upload if the API supports it. At minimum, delete the original bytes array after encoding.

---

#### [C-03] P1 — `AnthropicRequest.maxTokens` defaults to 2048 but refinement could need more
**File:** `api/src/main/kotlin/com/speekez/api/model/AnthropicModels.kt:9`
```kotlin
@SerializedName("max_tokens") val maxTokens: Int = 2048
```
A 5-minute recording at 150 WPM = ~750 words = ~1000 tokens input. The refined output could easily exceed 2048 tokens for longer recordings.

**Fix:** Make `maxTokens` dynamic based on input text length (e.g., `max(2048, estimatedInputTokens * 2)`).

---

#### [C-04] P1 — `OpenRouterRefinementRequest` has hardcoded `temperature = 0.3`
**File:** `api/src/main/kotlin/com/speekez/api/model/OpenRouterRefinementModels.kt:9`
But `AnthropicRequest` has no temperature parameter at all.

Inconsistent behavior between providers — same text refined with OpenRouter vs Anthropic direct will produce different variability.

**Fix:** Add temperature as a configurable parameter on the `RefinementClient` interface, or at least ensure parity across providers.

---

#### [C-05] P2 — `SttClient` interface lacks `validateKey()` method
`RefinementClient` has `validateKey()` but `SttClient` does not. The API test in `ModelSettingsScreen` only tests the refinement key, not the STT key.

**Fix:** Add `validateKey()` to `SttClient` and test both during API validation.

---

#### [C-06] P2 — `Converters.toRefinementLevel()` and `toModelTier()` will crash on unknown values
**File:** `data/src/main/kotlin/com/speekez/data/converter/Converters.kt:29-30, 39-40`
```kotlin
fun toRefinementLevel(value: String): RefinementLevel = RefinementLevel.valueOf(value)
fun toModelTier(value: String): ModelTier = ModelTier.valueOf(value)
```
If a future migration adds/renames enum values, existing database rows with old values will crash the app.

**Fix:** Use `try/catch` with a sensible default, or use `entries.find { it.name == value }`.

---

#### [C-07] P2 — `Preset` entity uses `Long` ID but `VoiceManager.startRecording()` takes `Int`
**File:** `voice/src/main/kotlin/com/speekez/voice/VoiceManager.kt:185`
```kotlin
fun startRecording(presetId: Int) {
    ...
    val preset = presetDao.getPresetById(presetId.toLong())
```
ID narrowing from `Long` to `Int` and back. While unlikely to overflow, this is a type safety issue.

**Fix:** Use `Long` consistently for preset IDs across all layers.

---

#### [C-08] P2 — `VoiceState` enum in `VoiceStateMachine.kt` should be in its own file
`VoiceState` and `VoiceStateMachine` are in the same file, but `VoiceState` is used across many modules (VoiceManager, widget, app). It should be a standalone file for clarity.

**Fix:** Extract `VoiceState` to its own file.

---

#### [C-09] P2 — `OpenRouterMessage.content` is `Any` type
**File:** `OpenRouterAudioClient.kt:28`
```kotlin
data class OpenRouterMessage(
    val role: String,
    val content: Any // Can be String or List<OpenRouterContent>
)
```
Using `Any` bypasses type safety and relies on Gson to figure out serialization. This is fragile.

**Fix:** Use a sealed class or `@JsonAdapter` to properly handle the polymorphic content field.

---

#### [C-10] P3 — `NoOpRefinementClient` always returns `true` for `validateKey()`
This is correct behavior but could be confusing during testing if a user accidentally selects NO_KEYS mode.

---

#### [C-11] P3 — `TranscriptionResponse` is a standalone data class in `OpenAiWhisperClient.kt`
It should be in the `model` package with the other API models for consistency.

---

### 3.2 Voice Layer

#### [C-12] P1 — `AudioHandler` doesn't set max duration at the MediaRecorder level
**File:** `voice/src/main/kotlin/com/speekez/voice/AudioHandler.kt:64-72`

The 5-minute limit is enforced only by `VoiceStateMachine`'s coroutine delay. If the coroutine is delayed (e.g., main thread is busy), recording could exceed 5 minutes. `MediaRecorder.setMaxDuration()` provides a hardware-level guarantee.

**Fix:** Add `setMaxDuration(300_000)` and `setOnInfoListener` to `MediaRecorder` setup.

---

#### [C-13] P2 — `AudioHandler.onAutoStop` callback is dead code
**File:** `voice/src/main/kotlin/com/speekez/voice/AudioHandler.kt:41`
```kotlin
var onAutoStop: (() -> Unit)? = null
```
This callback is declared but never invoked anywhere in the codebase. The 5-minute auto-stop is handled entirely by `VoiceStateMachine`, making this field unused dead code.

**Fix:** Remove `onAutoStop` or wire it to `MediaRecorder.setOnInfoListener` if implementing hardware-level max duration.

---

#### [C-14] P1 — `OpenRouterAudioClient.transcribe()` ignores the `languages` parameter
**File:** `api/src/main/kotlin/com/speekez/api/OpenRouterAudioClient.kt:58`
```kotlin
override suspend fun transcribe(audioFile: File, model: String, languages: List<String>): String {
```
The `languages` parameter is accepted but never used in the request body. The system prompt says "Transcribe the audio exactly as spoken" but doesn't specify which language to expect. This means:
- Multi-language presets won't affect OpenRouter STT behavior
- The model may misidentify the spoken language

**Fix:** Include the languages in the system prompt (e.g., "The audio is in one of these languages: ${languages.joinToString()}").

---

#### [C-15] P2 — `AccessibilityEvent.obtain()` is deprecated
**File:** `core/src/main/kotlin/com/speekez/core/AccessibilityUtils.kt:30`
```kotlin
val event = AccessibilityEvent.obtain(AccessibilityEvent.TYPE_ANNOUNCEMENT)
```
`AccessibilityEvent.obtain()` is deprecated since API 33. The replacement is to use the constructor directly: `AccessibilityEvent(TYPE_ANNOUNCEMENT)`.

**Fix:** Use `AccessibilityEvent(AccessibilityEvent.TYPE_ANNOUNCEMENT)` with an API level check, or use `announceForAccessibility()` on a View instead.

---

## 4. Functional Progression Review

### 4.1 Complete User Flow Analysis

```
Setup Flow: Welcome → API Key → Enable Keyboard → Mic Permission → Complete
Main Flow:  Dashboard (stats) → History (transcriptions) → Settings (config)
Voice Flow: Select Preset → Hold/Tap → Record → STT → [Refine] → Insert → Stats
Widget Flow: 1x1 widget → Recording Activity → Process → Clipboard
Floating Flow: Service → Overlay → Select Preset → Hold → Record → Clipboard
```

### 4.2 Issues Found

#### [F-01] P0 — `handleProcessing()` called from both state collector and potentially `onAutoStop`
**File:** `VoiceManager.kt:88-91, 118-133, 140-153`

The code comments mention "ISSUE-001" about a double-processing race condition, and the auto-stop callback only calls `stateMachine.stopRecording()` — but the init block's state collector triggers `handleProcessing()` on any transition to PROCESSING. However, the `isProcessingAudio` guard is not atomic.

```kotlin
private var isProcessingAudio = false  // not thread-safe

private fun handleProcessing() {
    if (isProcessingAudio) return  // race condition possible
    isProcessingAudio = true
```

If two coroutines on `Dispatchers.Main` both observe PROCESSING state, the flag check isn't guaranteed to prevent double entry.

**Fix:** Use `AtomicBoolean` or ensure all state transitions and processing happen on a single dispatcher with proper synchronization.

---

#### [F-02] P0 — `audioHandler.stop()` called on Main thread in `handleProcessing()`
**File:** `VoiceManager.kt:145`
```kotlin
val audioFile = audioHandler.stop()  // MediaRecorder.stop() on Main thread
```
`MediaRecorder.stop()` can block and is documented as potentially slow. Calling it on the main thread can cause ANR.

**Fix:** Move `audioHandler.stop()` to `Dispatchers.IO`.

---

#### [F-03] P1 — Setup flow doesn't validate API key before completing
**File:** `app/src/main/kotlin/com/speekez/app/screens/SetupFlow.kt`
The setup flow collects an API key but doesn't validate it. Users can complete setup with an invalid key and then get cryptic errors when trying to record.

**Fix:** Add key validation in the setup flow (similar to `ModelSettingsScreen`'s "Save & Test API").

---

#### [F-04] P1 — Widget `CoroutineScope` survives widget destruction
**File:** `widget/src/main/kotlin/com/speekez/widget/SpeekEZWidget1x1.kt:18`
```kotlin
private val scope = CoroutineScope(Dispatchers.Main + SupervisorJob())
```
`AppWidgetProvider` is a `BroadcastReceiver` — it is short-lived. The coroutine scope created here will be garbage collected unpredictably, and coroutines may be cancelled mid-flight.

**Fix:** Use `goAsync()` and manage the coroutine within the broadcast lifecycle, or use `WorkManager` for async widget updates.

---

#### [F-05] P1 — `FloatingWidgetService` uses system icon
**File:** `widget/src/main/kotlin/com/speekez/widget/FloatingWidgetService.kt:207`
```kotlin
.setSmallIcon(android.R.drawable.ic_btn_speak_now) // Use a system icon for now
```
The "for now" comment indicates this is a known TODO. Using system icons looks unprofessional and may not exist on all devices.

**Fix:** Use a proper SpeekEZ branded notification icon.

---

#### [F-06] P1 — No Groq key validation
The setup and settings flows validate OpenRouter, OpenAI, and Anthropic keys, but **never validate the Groq key**. A user could enter an invalid Groq key and it would silently fail at transcription time.

**Fix:** Add Groq key validation using a minimal Whisper API call.

---

#### [F-07] P1 — `DailyStats.date` uses string format "YYYY-MM-DD" as primary key
**File:** `data/src/main/kotlin/com/speekez/data/entity/DailyStats.kt:10`
Using a string date as a primary key works but:
- Depends on `SimpleDateFormat` locale consistency
- No timezone handling — if user travels across time zones, stats could split or duplicate

**Fix:** Consider using epoch-day (Int) as PK, or at minimum use a fixed `Locale.US` and timezone (already uses Locale.US in VoiceManager).

---

#### [F-08] P2 — Search query in `TranscriptionDao` is case-sensitive and SQL-injection-adjacent
**File:** `data/src/main/kotlin/com/speekez/data/dao/TranscriptionDao.kt:31`
```sql
WHERE raw_text LIKE '%' || :query || '%' OR refined_text LIKE '%' || :query || '%'
```
While Room parameterizes this safely (no SQL injection), the LIKE is case-sensitive by default in SQLite. Users searching "hello" won't find "Hello".

**Fix:** Use `LOWER()` for case-insensitive search:
```sql
WHERE LOWER(raw_text) LIKE '%' || LOWER(:query) || '%'
```

---

#### [F-09] P2 — No pagination for transcription history
`TranscriptionDao.getAllTranscriptions()` returns ALL transcriptions as a Flow. If a user has 10,000+ transcriptions, this will load everything into memory.

**Fix:** Add `PagingSource` support via Room's `@Query` with `LIMIT/OFFSET` or use Paging 3 library.

---

#### [F-10] P2 — WeeklyTrendChart uses fixed Monday-Sunday week
Users in locales where the week starts on Sunday (US, etc.) will see misaligned charts.

**Fix:** Use `WeekFields.of(Locale.getDefault()).firstDayOfWeek` for locale-aware week calculation.

---

#### [F-11] P2 — `calculateTimeSaved()` assumes typing speed of 75 WPM
**File:** `DashboardScreen.kt:209`
This is a reasonable average but should be configurable or at least documented in the UI.

---

#### [F-12] P3 — No transcription deletion from history
`TranscriptionDao` has a `delete()` method but the `HistoryScreen` doesn't expose a delete action. Users can't clean up their history.

**Fix:** Add swipe-to-delete or long-press delete in history.

---

#### [F-13] P3 — No export/backup of transcription data
For a standalone production app, users should be able to export their transcription history.

---

#### [F-14] P3 — FlorisBoard's keyboard settings are still accessible
The app ships with the full FlorisBoard settings stack (theme editor, keyboard layout, dictionary, etc.) via `FlorisAppActivity`. This could confuse users of a voice-focused app.

**Fix:** Audit and hide irrelevant FlorisBoard settings, or clearly separate "keyboard settings" from "SpeekEZ settings".

---

#### [F-15] P0 — Migration data corruption: `output_language` data not transformed to JSON array format
**File:** `data/src/main/kotlin/com/speekez/data/SpeekEZDatabase.kt:42`
Related to [A-11]. When `output_language` is renamed to `output_languages`, existing data contains a plain string (e.g., `"en"`) but the `Preset` entity expects a `List<String>` which is stored as a JSON array (e.g., `["en"]`). The `Converters.fromStringList()` TypeConverter will attempt to parse `"en"` as JSON and fail, potentially crashing the app on first access after migration.

**Fix:** Add a data transformation step in the migration that wraps existing values in JSON array format:
```sql
UPDATE presets SET output_languages = '[\"' || output_languages || '\"]'
```

---

#### [F-16] P1 — `AudioHandler.stop()` leaks file reference on failure
**File:** `voice/src/main/kotlin/com/speekez/voice/AudioHandler.kt:89-103`
```kotlin
fun stop(): File? {
    return try {
        mediaRecorder?.stop()
        currentFile
    } catch (e: Exception) {
        null  // returns null but doesn't delete currentFile
    } finally {
        mediaRecorder?.release()
        mediaRecorder = null
        isRecording = false
    }
}
```
When `MediaRecorder.stop()` throws, `currentFile` remains on disk but the reference is lost (returns null). The temp file `voice_temp_*.m4a` accumulates in the cache directory without cleanup.

**Fix:** Delete `currentFile` in the catch block, or keep a reference for later cleanup.

---

#### [F-17] P2 — Widget hardcodes class name string for VoiceShortcutActivity
**File:** `widget/src/main/kotlin/com/speekez/widget/SpeekEZWidget2x1.kt`
Uses a hardcoded string `"com.speekez.app.VoiceShortcutActivity"` for the intent target. If the activity is renamed or moved, the widget will silently fail.

**Fix:** Use `ComponentName` with the class reference directly, or use an explicit import.

---

## 5. Performance Review

### 5.1 Issues Found

#### [P-01] P0 — Base64 audio encoding on the IO thread with full file in memory
**File:** `OpenRouterAudioClient.kt:59-60`
Already covered in [C-02]. A 5-minute recording's Base64 encoding can spike memory by 6-10 MB.

**Fix:** Stream the encoding or allocate on a constrained buffer.

---

#### [P-02] P1 — `DashboardScreen` creates 4 separate Flow collectors
**File:** `app/src/main/kotlin/com/speekez/app/screens/DashboardScreen.kt:35-56`
```kotlin
val allStatsFlow = remember { dailyStatsDao.getAllStats() }
val overallAvgWpmFlow = remember { transcriptionDao.getOverallAvgWpm() }
val totalWordsFlow = remember { dailyStatsDao.getTotalWordCount() }
val weeklyStatsFlow = remember { dailyStatsDao.getWeeklyStats(...) }
```
Each collector opens a separate database cursor and invalidation tracker. This is 4 concurrent database observers for a single screen.

**Fix:** Combine into a single DAO query or a ViewModel that merges the flows.

---

#### [P-03] P1 — `FloatingWidgetService` creates a new `Retrofit` instance per `voiceManager` access
The `voiceManager` is lazy, but `ApiRouterManager` inside it creates Retrofit instances with separate connection pools. The floating widget service is a long-running foreground service, so these persist.

**Fix:** Share a single OkHttpClient/Retrofit instance application-wide.

---

#### [P-04] P1 — No connection pooling configuration
Each `OkHttpClient` uses default settings: 5 max idle connections, 5 minute keep-alive. With 3 separate clients, that's potentially 15 idle connections.

**Fix:** Configure connection pools appropriately for a mobile app (max 2-3 idle connections, 30s keep-alive).

---

#### [P-05] P2 — `getAllStats()` fetches ALL daily stats when only today's and this week's are needed
**File:** `DashboardScreen.kt:35`
For a user who has been using the app for a year, this loads 365+ rows into memory.

**Fix:** Separate queries for `todayStats` and `weeklyStats` (already exist in DAO but allStats is fetched additionally).

---

#### [P-06] P2 — `PresetSeeder` creates a new `CoroutineScope` every time
**File:** `SpeekEZDatabase.kt:57`
Even though the seeder only runs when count is 0, a new scope with `SupervisorJob` is created every time `getInstance()` is called for the first time. This scope is never cancelled.

**Fix:** Use `RoomDatabase.Callback` which is the idiomatic way.

---

#### [P-07] P2 — No ProGuard obfuscation
**File:** `app/proguard-rules.pro` — Contains `-dontobfuscate`
For an open-source project this is fine, but for a production standalone app:
- APK size increases
- API key handling code is more readable in decompiled APK
- String constants (base URLs, model names) are exposed

**Fix:** Enable obfuscation for release builds with proper keep rules.

---

#### [P-08] P3 — `Gson` used for both Retrofit and Room type converters
Kotlin serialization (`kotlinx.serialization`) is already a dependency. Using Gson alongside it means two JSON libraries in the APK.

**Fix:** Migrate to kotlinx.serialization throughout for smaller APK and better Kotlin support.

---

## 6. Security Review

### 6.1 Issues Found

#### [S-01] P0 — API keys held in Compose state as plain strings
**File:** `ModelSettingsScreen.kt:47-50`
```kotlin
var openRouterKey by remember { mutableStateOf(prefs.getOpenRouterKey() ?: "") }
var groqKey by remember { mutableStateOf(prefs.getGroqKey() ?: "") }
var openAiKey by remember { mutableStateOf(prefs.getOpenAiKey() ?: "") }
var anthropicKey by remember { mutableStateOf(prefs.getAnthropicKey() ?: "") }
```
Decrypted API keys are held in Compose `mutableStateOf` — these exist in the app's process memory and could be exposed via:
- Process memory dump
- Android debug tools
- Screenshots/screen recording (the masked transformation is UI-only)

**Fix:** Don't hold decrypted keys in state longer than needed. Show only masked versions and pass cleartext directly to the save function.

---

#### [S-02] P0 — `EncryptedPreferencesManager` silently deletes all prefs on corruption
**File:** `security/src/main/kotlin/com/speekez/security/EncryptedPreferencesManager.kt:20-24`
```kotlin
return try {
    buildEncryptedPrefs(context)
} catch (e: Exception) {
    Log.e("EncryptedPrefsManager", "Encrypted prefs corrupted, recreating", e)
    context.deleteSharedPreferences(FILE_NAME)
    buildEncryptedPrefs(context)
}
```
If `EncryptedSharedPreferences` throws (which can happen after OS updates, key rotation, or backup/restore), **all API keys are silently deleted** with no user notification.

**Fix:** Notify the user that keys need to be re-entered, and add a backup/recovery mechanism.

---

#### [S-03] P1 — OpenRouter API key sent via HTTP-Referer header
**File:** `OpenRouterAudioClient.kt:86-87`
```kotlin
referer = "https://github.com/speekez/speekez",
title = "SpeekEZ Android Keyboard",
```
This is an OpenRouter requirement for attribution, not a security issue per se, but the referer URL should point to an actual project page. If `speekez/speekez` doesn't exist on GitHub, this could cause issues.

**Fix:** Update to the correct project URL.

---

#### [S-04] P1 — No certificate pinning
API calls to `api.anthropic.com`, `openrouter.ai`, `api.openai.com`, and `api.groq.com` don't use certificate pinning. A man-in-the-middle attack on a compromised network could intercept API keys.

**Fix:** Add OkHttp `CertificatePinner` for production API endpoints.

---

#### [S-05] P2 — `EncryptedPreferencesManager` uses alpha-level security-crypto library
**File:** Build uses `security-crypto 1.1.0-alpha06` — this is a pre-release library. It has known issues with key backup/restore and Android version upgrades.

**Fix:** Monitor for stable release, or add fallback handling for known issues.

---

## 7. Test Coverage Review

### 7.1 Coverage Summary

| Module | Test Files | Key Classes Tested | Untested Classes |
|---|---|---|---|
| api | 4 | All 4 API clients | ApiRouterManager |
| voice | 4 | VoiceStateMachine, AudioHandler, VoiceManager, PermissionUtils | VoiceHapticManager |
| data | 1 | PresetSeeder | DAOs (integration), Converters |
| security | 1 | EncryptedPreferencesManager | — |
| core | 1 | NetworkUtils | AccessibilityUtils |
| widget | 0 | — | All widget classes |
| app (screens) | 0 | — | All screens |

### 7.2 Issues Found

#### [T-01] P1 — Zero test coverage for widget module
The widget module (`FloatingWidgetService`, `SpeekEZWidget1x1`, `SpeekEZWidget2x1`, recording activity) has no tests.

**Fix:** Add unit tests for widget logic (preset ID storage, update flows).

---

#### [T-02] P1 — Zero UI tests for SpeekEZ screens
None of the Compose screens have UI tests. For a production app, at minimum the critical user flows (setup, recording, settings) should be tested.

**Fix:** Add Compose UI tests using `createComposeRule()`.

---

#### [T-03] P1 — `ApiRouterManager` has no tests
The routing logic (which client to use based on mode, tier, Groq availability) is complex and completely untested.

**Fix:** Add unit tests for all routing permutations.

---

#### [T-04] P2 — `VoiceManagerTest` uses reflection to inject mocks
```kotlin
// Uses lazy field injection via reflection
```
This is fragile and breaks when field names change.

**Fix:** Make dependencies injectable via constructor or use a DI framework.

---

#### [T-05] P2 — No integration tests for database migrations
`MIGRATION_1_2` modifies schema but isn't tested with Room's `MigrationTestHelper`.

**Fix:** Add migration tests to verify data integrity across schema changes.

---

#### [T-06] P2 — CI pipeline doesn't run tests
**File:** `.github/workflows/android.yml`
Only runs `assembleDebug`, never `test`.

**Fix:** Add `./gradlew test check` to CI.

---

#### [T-07] P2 — No Kover coverage reports generated in CI
Kover is configured but never run as part of CI/CD.

**Fix:** Add `./gradlew koverReport` and set coverage thresholds.

---

#### [T-08] P3 — FlorisBoard inherited tests (lib/snygg, lib/kotlin) are comprehensive but unrelated
The Snygg library has 11 test files with thorough coverage. These are inherited from FlorisBoard and not SpeekEZ-specific.

---

#### [T-09] P3 — No snapshot/screenshot tests for UI
For a keyboard app where visual appearance matters, screenshot tests would catch regressions.

---

## 8. Documentation Review

### 8.1 Issues Found

#### [D-01] P2 — AGENTS.md lists incorrect VoiceStateMachine states
**File:** `AGENTS.md:126`
```
VoiceStateMachine.kt — States: IDLE, RECORDING, TRANSCRIBING, REFINING, COMMITTING, ERROR
```
The actual states in `VoiceStateMachine.kt` are: `IDLE, RECORDING, PROCESSING, DONE, ERROR`. The documented states appear to be from an earlier design and do not match the implementation.

**Fix:** Update AGENTS.md to reflect the actual state machine states.

---

#### [D-02] P2 — API-ARCHITECTURE.md and UX-FLOW.md still reference "WhisperDroid"
**Files:** `docs/API-ARCHITECTURE.md:1`, `docs/UX-FLOW.md:1,9,19,35,40,71,72,84`, `docs/KEYBOARD-RESEARCH.md:2`

Multiple documentation files still reference the old project name "WhisperDroid" instead of "SpeekEZ". This creates confusion about the project identity.

**Fix:** Find and replace all "WhisperDroid" references with "SpeekEZ" in documentation.

---

#### [D-03] P2 — VISION.md references WhisperDroid in competitive comparison
**File:** `docs/VISION.md:248`
The comparison table still labels the project as "WhisperDroid".

**Fix:** Update to "SpeekEZ" throughout VISION.md.

---

## 9. Production Readiness Checklist


| Requirement | Status | Issue |
|---|---|---|
| All critical bugs fixed | Needs work | 9 P0 issues |
| Database migration safe for all API levels | Broken | [A-11], [F-15] |
| Dependency injection | Missing | [A-02] |
| ViewModel layer | Missing | [A-05] |
| Structured error reporting | Missing | [A-08] |
| API key validation in setup | Missing | [F-03] |
| Certificate pinning | Missing | [S-04] |
| ProGuard obfuscation | Disabled | [P-07] |
| Unit test coverage > 80% | ~40% | [T-01-T-07] |
| UI tests for critical flows | Missing | [T-02] |
| CI runs tests | No | [T-06] |
| Proper notification icon | Missing | [F-05] |
| Memory leak protection | Needs work | [A-01] |
| Pagination for large datasets | Missing | [F-09] |
| Thread-safe state management | Needs work | [F-01] |
| Overlay permission check | Missing | [A-14] |
| Concurrent recording guard | Missing | [A-15] |
| Project identity (namespace/naming) | Incomplete | [A-12], [A-13], [D-01-D-03] |

---

## 10. Prioritized Fix List

### Phase 1: Critical Fixes (P0) — Must fix before any release

| # | Issue | Module | Effort |
|---|---|---|---|
| 1 | [A-11] Migration RENAME COLUMN crashes on API < 30 | data | Medium |
| 2 | [F-15] Migration data not transformed — crashes on Preset access | data | Medium |
| 3 | [A-01] VoiceManager scope never cancelled — memory leak | voice | Small |
| 4 | [F-01] Race condition in `handleProcessing()` — double processing | voice | Medium |
| 5 | [F-02] `MediaRecorder.stop()` on main thread — ANR | voice | Small |
| 6 | [C-01] `handleError()` in `onFailure` — error messages swallowed | api | Medium |
| 7 | [C-02/P-01] Audio file loaded fully into memory — OOM risk | api | Medium |
| 8 | [S-01] API keys in plain Compose state — memory exposure | app | Medium |
| 9 | [S-02] Silent key deletion on encrypted prefs corruption | security | Medium |

### Phase 2: High Priority (P1) — Fix before production release

| # | Issue | Module | Effort |
|---|---|---|---|
| 10 | [A-12] Root project name still "FlorisBoard" | build | Small |
| 11 | [A-13] App namespace still `dev.patrickgold.florisboard` | app | Large |
| 12 | [A-14] No overlay permission check in FloatingWidgetService | widget | Small |
| 13 | [A-15] Concurrent recording conflicts from multiple entry points | voice | Medium |
| 14 | [A-02] No dependency injection | all | Large |
| 15 | [A-03] Duplicate OkHttpClient instances | api | Medium |
| 16 | [A-04] Database seeding race condition | data | Small |
| 17 | [A-05] No ViewModel layer | app | Large |
| 18 | [C-03] maxTokens hardcoded to 2048 | api | Small |
| 19 | [C-04] Temperature inconsistency between providers | api | Small |
| 20 | [C-12] AudioHandler lacks hardware max duration | voice | Small |
| 21 | [C-14] OpenRouterAudioClient ignores `languages` parameter | api | Small |
| 22 | [F-03] Setup flow doesn't validate API key | app | Medium |
| 23 | [F-04] Widget CoroutineScope lifecycle | widget | Medium |
| 24 | [F-05] System icon for floating widget notification | widget | Small |
| 25 | [F-06] No Groq key validation | api/app | Medium |
| 26 | [F-07] String date as primary key | data | Medium |
| 27 | [F-16] AudioHandler.stop() leaks file on failure | voice | Small |
| 28 | [P-02] 4 concurrent database observers on Dashboard | app | Medium |
| 29 | [P-03] Multiple Retrofit instances in services | api | Medium |
| 30 | [P-04] No connection pool configuration | api | Small |
| 31 | [S-03] Incorrect referer URL | api | Small |
| 32 | [S-04] No certificate pinning | api | Medium |
| 33 | [T-01] Zero widget tests | widget | Medium |
| 34 | [T-02] Zero UI tests | app | Large |
| 35 | [T-03] ApiRouterManager untested | api | Medium |

### Phase 3: Medium Priority (P2) — Fix in next sprint

| # | Issue | Module | Effort |
|---|---|---|---|
| 36 | [A-06] Hardcoded preference key coupling | voice | Small |
| 37 | [A-07] Inconsistent extension patterns | data/voice | Small |
| 38 | [A-08] No structured error reporting | all | Medium |
| 39 | [C-05] SttClient lacks validateKey() | api | Small |
| 40 | [C-06] TypeConverter crashes on unknown enum | data | Small |
| 41 | [C-07] Long vs Int preset ID mismatch | voice | Small |
| 42 | [C-08] VoiceState should be in own file | voice | Small |
| 43 | [C-09] `Any` type for message content | api | Medium |
| 44 | [C-13] AudioHandler.onAutoStop dead code | voice | Small |
| 45 | [C-15] Deprecated AccessibilityEvent.obtain() | core | Small |
| 46 | [F-08] Case-sensitive search | data | Small |
| 47 | [F-09] No pagination for history | data/app | Medium |
| 48 | [F-10] Locale-specific week start | app | Small |
| 49 | [F-11] Hardcoded 75 WPM typing speed | app | Small |
| 50 | [F-17] Widget hardcodes VoiceShortcutActivity class name | widget | Small |
| 51 | [P-05] getAllStats fetches unnecessary data | data/app | Small |
| 52 | [P-06] PresetSeeder scope management | data | Small |
| 53 | [P-07] ProGuard obfuscation disabled | app | Medium |
| 54 | [S-05] Alpha security-crypto library | security | Small |
| 55 | [T-04] Reflection-based test injection | voice | Medium |
| 56 | [T-05] No migration tests | data | Medium |
| 57 | [T-06] CI doesn't run tests | ci | Small |
| 58 | [T-07] No Kover coverage in CI | ci | Small |
| 59 | [D-01] AGENTS.md has incorrect VoiceStateMachine states | docs | Small |
| 60 | [D-02] API-ARCHITECTURE.md and UX-FLOW.md reference "WhisperDroid" | docs | Small |
| 61 | [D-03] VISION.md references WhisperDroid | docs | Small |

### Phase 4: Low Priority (P3) — Backlog

| # | Issue | Module | Effort |
|---|---|---|---|
| 62 | [A-09] Benchmark module unused | build | Small |
| 63 | [A-10] CI doesn't run tests (duplicate of T-06) | ci | Small |
| 64 | [C-10] NoOp validateKey always true | api | Small |
| 65 | [C-11] TranscriptionResponse misplaced | api | Small |
| 66 | [F-12] No delete from history UI | app | Medium |
| 67 | [F-13] No export/backup | app | Large |
| 68 | [F-14] FlorisBoard settings still visible | app | Medium |
| 69 | [P-08] Dual JSON libraries (Gson + kotlinx) | all | Large |
| 70 | [T-08] Inherited tests not SpeekEZ-specific | lib | None |
| 71 | [T-09] No screenshot tests | app | Medium |

---

## Summary

The codebase has a **solid foundation** with good module separation, clean Kotlin code, and a well-thought-out feature set. The major gaps for production readiness are:

1. **Database migration** — RENAME COLUMN crashes on API < 30, data not transformed (P0)
2. **Lifecycle management** — CoroutineScope leaks, no ViewModel layer
3. **Thread safety** — Race conditions in VoiceManager, main-thread blocking
4. **Dependency management** — No DI, duplicate instances
5. **Project identity** — Namespace still `dev.patrickgold.florisboard`, root project name "FlorisBoard", docs reference "WhisperDroid"
6. **Security hardening** — Keys in memory, no cert pinning, no overlay permission check, alpha crypto library
7. **Test coverage** — No UI tests, no widget tests, no integration tests
8. **CI/CD** — Tests not running in pipeline

Start with **Phase 1** (9 critical fixes) and **Phase 2** (26 high-priority fixes) to get the app production-ready. The architecture is sound enough that these fixes are incremental improvements, not rewrites.

**Total issues: 67** (9 P0, 23 P1, 25 P2, 10 P3)
