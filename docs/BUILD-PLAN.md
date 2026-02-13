# Speekez V1.0 Build Plan

*Created: 2026-02-13 11:08 EST*  
*Based on: Claude Evaluation (EVALUATION-CLAUDE.md)*

## Executive Summary

**Approach:** Research-first validated. Proceeding with build phase.  
**Total Tasks:** 20 (16 implementation + 4 retrospectives)  
**Estimated Duration:** ~48-72 hours of Jules execution  
**Confidence:** 75% (Medium-High)

**Key Decisions:**
- ✅ MediaRecorder for audio (simpler, sufficient quality)
- ✅ OpenRouter single-key V1 (simplify onboarding)
- ✅ Model presets (Speed/Balanced/Accuracy)
- ✅ Maestro for UI testing
- ✅ VoiceManager architecture pattern
- ⚠️ Error handling matrix needed before P1
- ⚠️ Waveform approach: Pick ONE before P2-1

---

## Priority 0: Foundation (Sequential)

**Goal:** Scaffold VoiceManager, add dependencies, permission flow, basic recording

**Must Complete Before P1:** Yes (P1 depends on P0)

### P0.1: VoiceManager Scaffold
**Duration:** 2-3h  
**Description:** Create VoiceManager class following Florisboard manager pattern
**Deliverables:**
- `app/src/main/kotlin/dev/patrickgold/florisboard/voice/VoiceManager.kt`
- Register in FlorisApplication
- Add `context.voiceManager()` extension
- Empty methods: startRecording(), stopRecording(), isRecording()
- Lifecycle hooks: onWindowShown(), onWindowHidden()

**Success Criteria:**
- VoiceManager accessible from keyboard context
- No crashes on keyboard lifecycle events

---

### P0.2: Build Dependencies
**Duration:** 1-2h  
**Description:** Add Retrofit, OkHttp, EncryptedSharedPreferences to build system
**Deliverables:**
- Update `gradle/libs.versions.toml` with versions
- Add to `app/build.gradle.kts`
- Configure ProGuard rules for networking
- Verify clean build

**Dependencies to Add:**
```kotlin
retrofit = "2.9.0"
okhttp = "4.12.0"
androidx-security-crypto = "1.1.0-alpha06"
```

**Success Criteria:**
- Project builds successfully
- No ProGuard warnings
- Dependencies available in VoiceManager

---

### P0.3: Permission Flow
**Duration:** 3-4h  
**Description:** Request RECORD_AUDIO permission with transparent activity pattern
**Deliverables:**
- `PermissionActivity.kt` (transparent, finish after request)
- Update AndroidManifest.xml
- Store permission state in EncryptedSharedPreferences
- Handle permission denied gracefully (show rationale, disable voice button)

**UX Flow:**
1. User taps voice button first time
2. Transparent activity launches
3. Show permission rationale dialog
4. Request RECORD_AUDIO
5. Store result
6. Return to keyboard

**Success Criteria:**
- Permission requested on first voice button tap
- Rationale shown before system dialog
- Denied state disables voice features
- No permission leaks or crashes

---

### P0.4: Basic Audio Recording
**Duration:** 2-3h  
**Description:** MediaRecorder setup with lifecycle management
**Deliverables:**
- MediaRecorder initialization in VoiceManager
- Record to `cache/voice_temp.m4a`
- Start/stop recording methods
- Cleanup on keyboard hidden
- Maximum duration limit (60s)

**Implementation Notes:**
- Audio format: AAC, 64kbps, 16kHz (good for speech, small files)
- File size: ~480KB per minute
- Use lifecycle scope for coroutines
- Force stop recording on onWindowHidden()

**Success Criteria:**
- Recording starts/stops without crashes
- Audio file created in cache dir
- Recording auto-stops at 60s
- Cleanup on keyboard dismiss
- No resource leaks (test with multiple record cycles)

---

### RETROSPECTIVE R0: Foundation Check
**Duration:** 1h  
**Description:** Evaluate P0 foundation before continuing to P1
**Questions:**
- Is VoiceManager architecture sound?
- Are permissions handled correctly?
- Any refactoring needed?
- Performance issues?
- Edge cases discovered?

**Deliverables:**
- `docs/retrospectives/R0-FOUNDATION.md`
- List of any P0 fixes needed
- Go/no-go decision for P1

---

## Priority 1: Core Features (MVP)

**Goal:** Complete record → transcribe → commit flow

**Can Partially Parallelize:** P1.1 || P1.2/P1.3, then P1.5 || P1.4/P1.6

### P1.1: Whisper Integration
**Duration:** 3-4h  
**Description:** Retrofit service for Whisper API (OpenAI or Groq)
**Deliverables:**
- `WhisperApiService.kt` with Retrofit interface
- Multipart file upload
- Parse transcription response
- Basic error handling (network, 401, 429, 500)
- Timeout: 30s (aggressive, fail fast)

**API Choice V1:** OpenAI Whisper (most reliable)

**Success Criteria:**
- Upload audio file, get transcription back
- Errors surfaced to caller (not silent failures)
- No memory leaks on large files

---

### P1.2: Voice Button UI
**Duration:** 2-3h  
**Description:** Microphone icon in Smartbar with QuickAction integration
**Deliverables:**
- Add microphone icon drawable
- Create VoiceQuickAction
- Register in QuickActions list
- Click handler calls `voiceManager.startRecording()`
- Button disabled if no permission

**Visual Design:**
- Icon: Material Design microphone icon
- Placement: Smartbar (respect existing quick actions)
- State: Enabled when permitted, disabled otherwise

**Success Criteria:**
- Button appears in Smartbar
- Tap triggers recording start
- Disabled state clear to user

---

### P1.3: Recording State UI
**Duration:** 3-4h  
**Description:** Visual feedback for recording/processing states
**Deliverables:**
- Add `isRecording: Boolean` to ObservableKeyboardState
- Recording indicator in Smartbar (animated red dot?)
- State machine: Idle → Recording → Processing → Success/Error
- Timer display during recording
- Processing spinner

**State Transitions:**
- Idle: Normal UI
- Recording: Red dot pulsing, timer counting
- Processing: Spinner + "Transcribing..." text
- Success: Brief checkmark, then Idle
- Error: Brief error icon + message, then Idle

**Success Criteria:**
- User knows when recording is active
- Clear feedback during processing
- Errors communicated visually

---

### P1.4: Text Commitment
**Duration:** 1-2h  
**Description:** Insert transcribed text into active input field
**Deliverables:**
- Use `EditorInstance.commitText()` to insert result
- Handle cursor position correctly
- Clear existing selection if present
- No double spaces/formatting issues

**Success Criteria:**
- Transcribed text appears in input field
- Cursor positioned after inserted text
- Works across different apps (Messages, Notes, etc.)

---

### RETROSPECTIVE R1: Core Flow Check
**Duration:** 1h  
**Description:** Validate record → transcribe → commit works end-to-end
**Questions:**
- Does the full flow work?
- Is latency acceptable (<5s)?
- What errors are we seeing?
- Battery impact noticeable?
- Any UX friction points?

**Deliverables:**
- `docs/retrospectives/R1-CORE-FLOW.md`
- List of any P1 fixes needed
- Decision: Continue to OpenRouter or ship basic version?

---

### P1.5: OpenRouter Integration
**Duration:** 3-4h  
**Description:** Retrofit service for OpenRouter refinement API
**Deliverables:**
- `OpenRouterApiService.kt` with Retrofit interface
- Chat completion request (system + user message)
- Model tier support: Haiku (V1 default), Sonnet 4, Sonnet 4.5
- Response parsing
- Error handling (network, 401, 429, 500)
- Cost parsing from headers

**System Prompt:**
```
Refine this voice-to-text transcription for clarity and formatting. 
Fix capitalization, punctuation, and obvious errors. 
Preserve the user's intended meaning and tone. 
Return ONLY the refined text, no commentary.
```

**Success Criteria:**
- Refinement improves transcription quality
- Latency acceptable (+2-5s)
- Cost tracked correctly

---

### P1.6: Settings Screen
**Duration:** 2-3h  
**Description:** Configuration UI for API keys and model selection
**Deliverables:**
- Settings screen in Florisboard settings
- API key input (masked, stored in EncryptedSharedPreferences)
- Model tier selector: Speed/Balanced/Accuracy (maps to Haiku/Sonnet4/Sonnet4.5)
- Enable/disable refinement toggle
- Cost summary display (total spent this month)

**Settings Categories:**
- Voice Input
  - API Key (required)
  - Model Tier (3 presets)
  - Refinement (toggle)
  - Usage This Month ($X.XX)

**Success Criteria:**
- Keys stored securely
- Model selection persists
- Cost tracking accurate

---

### RETROSPECTIVE R2: Integration Check
**Duration:** 1h  
**Description:** Evaluate OpenRouter integration and settings UX
**Questions:**
- Does refinement improve results noticeably?
- Is cost tracking working correctly?
- Settings UX intuitive?
- Any performance issues?
- Ready for polish phase?

**Deliverables:**
- `docs/retrospectives/R2-INTEGRATION.md`
- A/B test results (with/without refinement)
- Go/no-go decision for P2

---

## Priority 2: Polish & Enhancement

**Goal:** Visual polish, error handling, accessibility

**Highly Parallel:** All P2 tasks independent, can run simultaneously

### P2.1: Visual Feedback (Waveform)
**Duration:** 2-3h  
**Description:** Recording visualization with waveform or amplitude animation
**Decision Needed:** Pick ONE approach before starting
**Options:**
1. Amplitude bars (simplest)
2. Circular waveform
3. Linear waveform

**Deliverables:**
- Animated visualization during recording
- Amplitude-based scaling
- Timer display (MM:SS)
- Smooth 60fps animation

**Success Criteria:**
- Visual feedback engaging, not distracting
- No performance impact on typing
- Works in light/dark mode

---

### P2.2: Error Handling Matrix
**Duration:** 3-4h  
**Description:** Comprehensive error handling with user-facing messages
**Deliverables:**
- Error matrix document
- User-friendly error messages
- Retry logic (3 attempts with exponential backoff)
- Actionable error UI (e.g., "Check API key" button)

**Error Types:**
- Network: "No internet connection. Check your connection and try again."
- 401 Unauthorized: "Invalid API key. Check settings."
- 429 Rate Limit: "Too many requests. Wait 60s and retry."
- 500 Server Error: "Service unavailable. Try again later."
- Timeout: "Request timed out. Try again or use shorter recording."
- Empty Transcription: "Couldn't transcribe audio. Try speaking louder."
- Audio Recording Failure: "Microphone error. Check permissions."

**Success Criteria:**
- All error states handled
- User knows what went wrong and how to fix it
- No silent failures

---

### P2.3: Cost Tracking
**Duration:** 2-3h  
**Description:** Parse usage from OpenRouter, store running total, display in settings
**Deliverables:**
- Parse cost from OpenRouter response headers
- Store per-request cost in local DB
- Monthly rollup (reset on 1st of month)
- Display in settings: "This month: $X.XX (Y requests)"
- Optional: Set monthly limit with warning

**Storage:**
- SQLite table: `voice_usage` (timestamp, cost, model)
- Query: SUM where month = current month

**Success Criteria:**
- Cost accurate to OpenRouter billing
- Monthly summary correct
- No data leaks

---

### P2.4: Haptic Feedback
**Duration:** 1h  
**Description:** Vibration patterns for start/stop/error
**Deliverables:**
- Start recording: Short click (50ms)
- Stop recording: Double click (50ms, 50ms pause, 50ms)
- Error: Long buzz (200ms)

**Success Criteria:**
- Haptics feel natural
- Can be disabled in settings (respect system haptic preference)

---

### P2.5: Dark Mode Theming
**Duration:** 2h  
**Description:** Ensure voice UI looks good in dark mode
**Deliverables:**
- Recording indicator colors (red in light, lighter red in dark)
- Error states high contrast
- Accent colors consistent with Florisboard theme

**Success Criteria:**
- All states visible in dark mode
- No color clashes
- Consistent with Florisboard aesthetic

---

### P2.6: Accessibility
**Duration:** 2-3h  
**Description:** TalkBack support and high contrast mode
**Deliverables:**
- Content descriptions for all voice UI elements
- TalkBack announcements:
  - "Recording started"
  - "Recording stopped, transcribing"
  - "Transcription complete"
  - "Error: [error message]"
- High contrast mode support

**Success Criteria:**
- Blind user can use voice input via TalkBack
- All states announced
- High contrast mode readable

---

### RETROSPECTIVE R3: Pre-Testing Review
**Duration:** 1h  
**Description:** Final check before moving to testing phase
**Questions:**
- Feature complete?
- Known bugs?
- Edge cases?
- Performance acceptable?
- Ready for dual testing?

**Deliverables:**
- `docs/retrospectives/R3-PRE-TESTING.md`
- List of known issues
- Test plan for dual testing phase

---

## Testing Phase (Follows Build)

**Dual Testing Protocol** (from TESTING-SETUP.md):
1. Jules writes Maestro tests
2. I verify manually on Fort Mac
3. Iterate on failures
4. Both pass → V1.0 complete

**Test Scenarios:**
- Happy path (record → transcribe → commit)
- Permission denied
- Network errors
- API errors (401, 429, 500)
- Timeout
- Empty transcription
- Keyboard lifecycle (hide/show mid-recording)
- Background noise
- Long recordings (60s)
- Rapid start/stop

---

## Task Summary

| Phase | Tasks | Estimated Hours |
|-------|-------|-----------------|
| P0 Foundation | 4 + R0 | 9-13h |
| P1 Core Features | 6 + R1 + R2 | 16-22h |
| P2 Polish | 6 + R3 | 12-17h |
| **Total** | **20 tasks** | **37-52h** |

---

## Open Questions

**Before P1:**
1. ⚠️ **ERROR HANDLING MATRIX** - Define all error types and messages BEFORE P1.2
2. ⚠️ **WAVEFORM APPROACH** - Pick ONE visualization before P2.1

**Before P2:**
3. Refinement worth the cost? (A/B test in R2)
4. Max recording duration: 60s enough?
5. Cost limit: Should we enforce or just warn?

**Before Testing:**
6. Test device: Fort Mac available?
7. Sample audio corpus: Need to create?
8. Accessibility testing: How to validate TalkBack?

---

## Success Metrics (V1.0)

**Functional:**
- ✅ Record audio via keyboard
- ✅ Transcribe with Whisper
- ✅ Refine with OpenRouter
- ✅ Commit to input field
- ✅ Settings for API keys and model selection
- ✅ Cost tracking

**Quality:**
- ✅ Latency <10s (record to commit)
- ✅ Transcription accuracy >90% (clear speech)
- ✅ No crashes or resource leaks
- ✅ Works across 5+ apps (Messages, Notes, Browser, etc.)

**Polish:**
- ✅ Visual feedback during recording/processing
- ✅ Error messages clear and actionable
- ✅ Dark mode support
- ✅ Accessibility (TalkBack)

---

## Next Steps (Immediate)

1. ✅ **Commit this BUILD-PLAN.md**
2. ⏳ **Define error handling matrix** (before P1)
3. ⏳ **Create architecture diagram** (visual aid for build)
4. ⏳ **Set up sample audio test corpus**
5. ⏳ **Begin P0.1: VoiceManager Scaffold**

**Awaiting:** Dude's review of this plan before proceeding to P0.1

---

*End of Build Plan*
