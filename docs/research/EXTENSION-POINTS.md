# Extension Points for Voice Integration

This document outlines how and where to integrate the voice-to-text features into the existing Florisboard architecture.

## 1. Adding the Voice Button

The best place to add a voice button is the **Smartbar** (toolbar).
- **Extension Point**: `QuickAction.kt` and `QuickActionArrangement.kt`.
- **Action**: Add a new `QuickAction` type or use the existing `KeyCode.VOICE_INPUT` if appropriate.
- **UI**: The `Smartbar` automatically renders `QuickAction` items. We can add a microphone icon to the default set of actions.

## 2. Input Handling

When the voice button is pressed:
- **Extension Point**: `KeyboardManager.onInputKeyUp`.
- **Action**: Instead of `switchToVoiceInputMethod()` (which switches to another IME), we will intercept `KeyCode.VOICE_INPUT` to trigger our internal recording logic.

## 3. Recording & Permission Handling

- **Permission**: `RECORD_AUDIO` is required.
- **Pattern**: Use `SetupScreen.kt` or create a new activity-based permission request flow. Since IMEs cannot request permissions directly, we must launch a small transparent activity or use the main settings activity.
- **Lifecycle**: `FlorisImeService` provides the lifecycle scope needed for asynchronous recording and API calls.

## 4. Transcription & Refinement (Whisper + Claude)

- **Manager**: Create a new `VoiceManager` (or `SpeechManager`) and register it in `FlorisApplication`.
- **Integration**:
    1. `VoiceManager.startRecording()`
    2. `VoiceManager.stopRecordingAndProcess()`
    3. Call Whisper API for transcription.
    4. (Optional) Call Claude API for refinement.
    5. Call `EditorInstance.commitText(result)` to insert the text.

## 5. UI Feedback (Recording State)

Users need to know when the keyboard is listening.
- **Extension Point**: `Smartbar.kt` or `TextInputLayout.kt`.
- **Action**: Show an overlay or change the Smartbar background color/icon when `activeState.isRecording` is true.
- **Theming**: Use `FlorisImeUi.kt` to define a "recording-indicator" element that can be styled via Snygg.

## 6. Settings & Preferences

- **Extension Point**: `FlorisPreferenceStore.kt` and `SettingsActivity`.
- **Action**: Add a "Voice" category in settings for:
    - API Key management.
    - Whisper model selection.
    - Claude prompt customization.
    - Language selection.

## 7. Service Lifecycle Hooks

- **Extension Point**: `FlorisImeService.onWindowHidden`.
- **Action**: Ensure recording is stopped and resources are released when the keyboard is hidden.
