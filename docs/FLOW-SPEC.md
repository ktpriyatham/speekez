# SpeekEZ v2 Android App -- Complete User Flow Specification

> **Generated from source code analysis on 2026-02-20**
> **Codebase:** FlorisBoard fork with SpeekEZ voice-to-text features
> **Source:** `/tmp/speekez-repo`

---

## Table of Contents

1. [Flow 1: App Launch / First Run](#flow-1-app-launch--first-run)
2. [Flow 2: SpeekEZ Onboarding (Setup Flow)](#flow-2-speekez-onboarding-setup-flow)
3. [Flow 3: FlorisBoard IME Setup (Setup Screen)](#flow-3-florisboard-ime-setup-setup-screen)
4. [Flow 4: API Key Setup](#flow-4-api-key-setup)
5. [Flow 5: Voice Recording (Keyboard -- Smartbar)](#flow-5-voice-recording-keyboard--smartbar)
6. [Flow 6: Voice Recording (Widget 1x1)](#flow-6-voice-recording-widget-1x1)
7. [Flow 7: Voice Recording (Widget 2x1)](#flow-7-voice-recording-widget-2x1)
8. [Flow 8: Voice Recording (Floating Widget)](#flow-8-voice-recording-floating-widget)
9. [Flow 9: Voice Recording (Voice Shortcut Activity)](#flow-9-voice-recording-voice-shortcut-activity)
10. [Flow 10: Preset Management](#flow-10-preset-management)
11. [Flow 11: History Browsing](#flow-11-history-browsing)
12. [Flow 12: Settings](#flow-12-settings)
13. [Flow 13: Widget Configuration (1x1)](#flow-13-widget-configuration-1x1)
14. [Flow 14: Widget Configuration (2x1)](#flow-14-widget-configuration-2x1)
15. [Flow 15: Keyboard Switching / IME Service Lifecycle](#flow-15-keyboard-switching--ime-service-lifecycle)
16. [Flow 16: Error Flows](#flow-16-error-flows)
17. [Flow 17: Voice Pipeline (Internal State Machine)](#flow-17-voice-pipeline-internal-state-machine)
18. [Flow 18: Application Initialization](#flow-18-application-initialization)
19. [Flow 19: Permission Request Flow](#flow-19-permission-request-flow)
20. [Flow 20: Dashboard Statistics](#flow-20-dashboard-statistics)

---

## Flow 1: App Launch / First Run

### Entry Point
User taps the SpeekEZ app icon on their Android launcher.

### Screens/States
1. **Android Launcher** --> Tap app icon --> `SpeekEZActivity.onCreate()` --> Check `prefs.speekez.setupComplete`
2. If `setupComplete == false` --> **SetupFlow** (see Flow 2)
3. If `setupComplete == true` --> **MainScreen** with bottom nav (Dashboard, History, Settings)

### State Transitions
- `prefs.speekez.setupComplete` (boolean, key: `speekez__setup_complete`, default: `false`)
  - Remains `false` until user completes the setup flow
  - Set to `true` by `SetupFlow.onSetupComplete` callback

### Code References
- `/tmp/speekez-repo/app/src/main/kotlin/com/speekez/app/SpeekEZActivity.kt:41-67` -- `SpeekEZActivity.onCreate()`: Reads `setupComplete`, conditionally renders `SetupFlow` or `MainScreen`
- `/tmp/speekez-repo/app/src/main/kotlin/com/speekez/app/SpeekEZActivity.kt:49` -- `prefs.speekez.setupComplete.collectAsState()` determines routing
- `/tmp/speekez-repo/app/src/main/kotlin/com/speekez/app/SpeekEZActivity.kt:57-61` -- On setup complete, sets `prefs.speekez.setupComplete` to `true`
- `/tmp/speekez-repo/app/src/main/AndroidManifest.xml:80-93` -- `SpeekEZActivity` declared as `MAIN/LAUNCHER` with `singleTask` launch mode
- `/tmp/speekez-repo/app/src/main/kotlin/com/speekez/app/SpeekEZActivity.kt:69-126` -- `SpeekEZTheme()` composable: applies dark/light theme based on `prefs.other.settingsTheme`

### Edge Cases
- **Direct boot (device locked):** `FlorisApplication.onCreate()` checks `UserManagerCompat.isUserUnlocked()`. If locked, only initializes extensions and registers a `BootComplete` receiver. Full `init()` deferred until unlock.
- **Theme not loaded yet:** Theme defaults to `AppTheme.AUTO` which follows system dark mode.
- **Preferences store not loaded:** `FlorisPreferenceStore.initAndroid()` runs in a coroutine; `preferenceStoreLoaded` is a `MutableStateFlow<Boolean>`. The `FlorisAppActivity` splash screen stays until loaded; `SpeekEZActivity` does not explicitly wait (potential race).

### Connections to Other Flows
- Leads to: [Flow 2: SpeekEZ Onboarding] when `setupComplete == false`
- Leads to: [Flow 12: Settings] / [Flow 11: History] / [Flow 20: Dashboard] when `setupComplete == true`

### Verification Checklist
- [x] Entry point exists and is reachable (MAIN/LAUNCHER intent filter)
- [x] All transitions have corresponding code (setupComplete check at line 49)
- [x] Error states are handled (CrashUtility installed in FlorisApplication)
- [ ] Back navigation works (single activity, back exits app)
- [ ] State is preserved across config changes (Compose handles via remember/state)

---

## Flow 2: SpeekEZ Onboarding (Setup Flow)

### Entry Point
Automatically shown when `prefs.speekez.setupComplete == false` inside `SpeekEZActivity`.

### Screens/States
1. **Step 1: WelcomeStep** --> User taps "Get Started" --> `currentStep = 2`
2. **Step 2: ApiKeyStep** --> User enters OpenRouter API key --> User taps "Save & Continue" --> `currentStep = 3`
   - Alternative: User taps "Skip for now" --> Skip warning dialog --> User confirms skip --> `currentStep = 3`
3. **Step 3: EnableKeyboardStep** --> User taps "Go to Settings" --> Opens `Settings.ACTION_INPUT_METHOD_SETTINGS` --> User returns --> Taps "I've enabled it" --> `currentStep = 4`
4. **Step 4: AllSetStep** --> User taps "Finish" --> `onSetupComplete()` called --> `prefs.speekez.setupComplete = true`

### State Transitions
- `currentStep` (Int, local state) changes from 1 -> 2 -> 3 -> 4 sequentially
- `apiKey` (String, local state) updated as user types in step 2
- `error` (String?, local state) set when API key validation fails
- `showSkipWarning` (Boolean, local state) toggled when user attempts to skip
- `prefs.speekez.setupComplete` set to `true` on finish

### Code References
- `/tmp/speekez-repo/app/src/main/kotlin/com/speekez/app/screens/SetupFlow.kt:31-73` -- `SetupFlow()` composable with `AnimatedContent` for step transitions
- `/tmp/speekez-repo/app/src/main/kotlin/com/speekez/app/screens/SetupFlow.kt:76-106` -- `WelcomeStep`: Shows "SpeekEZ" title with gradient, "Get Started" button
- `/tmp/speekez-repo/app/src/main/kotlin/com/speekez/app/screens/SetupFlow.kt:108-225` -- `ApiKeyStep`: OpenRouter key input, validation (`sk-or-` prefix), skip dialog
- `/tmp/speekez-repo/app/src/main/kotlin/com/speekez/app/screens/SetupFlow.kt:186-189` -- Key validation: `apiKey.startsWith("sk-or-")`, saves via `encryptedPrefs.saveOpenRouterKey()` and sets mode to `ApiMode.OPENROUTER`
- `/tmp/speekez-repo/app/src/main/kotlin/com/speekez/app/screens/SetupFlow.kt:227-260` -- `EnableKeyboardStep`: Sends user to Android IME settings
- `/tmp/speekez-repo/app/src/main/kotlin/com/speekez/app/screens/SetupFlow.kt:262-286` -- `AllSetStep`: Final confirmation

### Edge Cases
- **Invalid API key format:** Error message "Invalid key. Must start with 'sk-or-'" displayed below the text field
- **User skips API key:** Warning dialog shown. If confirmed, voice features will not work until API key is configured in Settings > Model tab
- **User doesn't enable keyboard:** Step 3 does not verify that the keyboard was actually enabled. User self-reports by tapping "I've enabled it". No actual check performed.
- **Back navigation between steps:** `AnimatedContent` supports forward/backward slide animations based on step comparison. However, there is no explicit "Back" button in steps 2-4 -- the user cannot go back to a previous step.

### Connections to Other Flows
- Returns to: [Flow 1: App Launch] which then shows MainScreen
- Leads to: [Flow 4: API Key Setup] (setup version, simplified to OpenRouter only)
- Leads to: Android System Settings for IME enablement

### Verification Checklist
- [x] Entry point exists and is reachable (setupComplete == false)
- [x] All transitions have corresponding code (currentStep increments)
- [x] Error states are handled (API key validation, skip dialog)
- [ ] Back navigation works (NO back button between steps -- gap)
- [x] State is preserved across config changes (mutableIntStateOf)

---

## Flow 3: FlorisBoard IME Setup (Setup Screen)

### Entry Point
Accessed via `FlorisAppActivity` when `prefs.internal.isImeSetUp == false`. This is the inherited FlorisBoard setup, separate from the SpeekEZ setup flow.

### Screens/States
1. **Step 1: EnableIme** --> User taps "Open Settings" --> `InputMethodUtils.showImeEnablerActivity()` --> User enables SpeekEZ IME in system settings
2. **Step 2: SelectIme** --> User taps "Switch Keyboard" --> `InputMethodUtils.showImePicker()` --> User selects SpeekEZ
3. **Step 3: SelectNotification** (API 33+ only) --> User taps button --> Notification permission request
4. **Step 4: FinishUp** --> User taps "Finish" --> `prefs.internal.isImeSetUp = true` --> Navigate to Settings Home

### State Transitions
- `FlorisStepState` auto-detects current step based on:
  - `isFlorisBoardEnabled` (observed via `InputMethodUtils.observeIsFlorisboardEnabled`)
  - `isFlorisBoardSelected` (observed via `InputMethodUtils.observeIsFlorisboardSelected`)
  - `notificationPermissionState` (enum: NOT_SET, GRANTED, DENIED)
- `prefs.internal.isImeSetUp` set to `true` on finish

### Code References
- `/tmp/speekez-repo/app/src/main/kotlin/dev/patrickgold/florisboard/app/setup/SetupScreen.kt:67-259` -- Full setup screen implementation
- `/tmp/speekez-repo/app/src/main/kotlin/dev/patrickgold/florisboard/app/FlorisAppActivity.kt:200` -- `startDestination` check: `if (isImeSetUp) Routes.Settings.Home::class else Routes.Setup.Screen::class`
- `/tmp/speekez-repo/app/src/main/kotlin/dev/patrickgold/florisboard/app/FlorisAppActivity.kt:116-121` -- Android 13+ notification permission check resets `isImeSetUp` if NOT_SET

### Edge Cases
- **Polling loop:** A `LaunchedEffect` polls every 200ms to detect when the IME gets enabled, then relaunches `FlorisAppActivity` with appropriate flags
- **Permission denial:** `NotificationPermissionState.DENIED` is saved; the step still advances

### Connections to Other Flows
- Related to: [Flow 2] (SpeekEZ's own setup) -- these are two separate setup flows
- The SpeekEZ setup (Flow 2) happens inside `SpeekEZActivity`; the FlorisBoard setup (Flow 3) happens inside `FlorisAppActivity`

### Verification Checklist
- [x] Entry point exists and is reachable (FlorisAppActivity checks isImeSetUp)
- [x] All transitions have corresponding code (LaunchedEffect auto-detects)
- [x] Error states are handled (permission denied case)
- [x] Back navigation works (FlorisStepLayout handles)
- [x] State is preserved across config changes (rememberSaveable)

---

## Flow 4: API Key Setup

### Entry Point
Settings tab > "Model" tab in the main SpeekEZ app, or during onboarding (Flow 2, Step 2).

### Screens/States
1. **ModelSettingsScreen** --> User sees Provider Mode segmented button (OpenRouter / Separate Keys)
2. If **OpenRouter mode**: Single API key field (`sk-or-...` prefix)
3. If **Separate Keys mode**: Two API key fields (OpenAI `sk-...` and Anthropic `sk-ant-...`)
4. **Model Tier selection**: Cheap / Best / Custom radio buttons
5. If **Custom tier**: Editable STT model and Refinement model text fields
6. **Cost Estimate card**: Shows estimated monthly cost
7. **Save & Test API button** --> Saves all values to `EncryptedPreferencesManager` --> Performs API test --> Shows success/failure snackbar

### State Transitions
- `apiMode` (ApiMode enum: OPENROUTER, SEPARATE, NO_KEYS) -- stored in `EncryptedSharedPreferences`
- `modelTier` (ModelTier enum: CHEAP, BEST, CUSTOM) -- stored in `EncryptedSharedPreferences`
- API keys stored in AES-256-GCM encrypted `SharedPreferences` via `EncryptedPreferencesManager`
- `showSwitchModeDialog` (ApiMode?, local state) -- shown when switching modes with existing keys

### Code References
- `/tmp/speekez-repo/app/src/main/kotlin/com/speekez/app/screens/settings/ModelSettingsScreen.kt:39-480` -- Full model settings screen
- `/tmp/speekez-repo/security/src/main/kotlin/com/speekez/security/EncryptedPreferencesManager.kt:1-127` -- Encrypted storage for all API keys
- `/tmp/speekez-repo/api/src/main/kotlin/com/speekez/api/ApiRouterManager.kt:1-85` -- Routes STT/refinement calls based on ApiMode and ModelTier
- `/tmp/speekez-repo/core/src/main/kotlin/com/speekez/core/ApiMode.kt:1-7` -- `OPENROUTER`, `SEPARATE`, `NO_KEYS`
- `/tmp/speekez-repo/core/src/main/kotlin/com/speekez/core/ModelTier.kt:1-7` -- `CHEAP`, `BEST`, `CUSTOM`

### Edge Cases
- **Switching modes with existing keys:** `showSwitchModeDialog` confirmation dialog warns "Switching modes will clear your current API keys"
- **Invalid key prefix:** Green/red indicator dot next to each key field. Save button disabled if keys invalid (`isKeySetupValid()` at line 436)
- **No internet during test:** `performApiTest()` checks `NetworkUtils.isOnline()` first, throws `IllegalStateException("No internet connection")`
- **API test is simulated:** `performApiTest()` at line 465-479 has a `delay(1500)` and always returns `true` if a refinement client exists -- not a real API test
- **Key visibility toggle:** Each key field has a show/hide toggle using `MaskedKeyTransformation` (shows first 8 and last 4 chars, masks middle)

### Connections to Other Flows
- Leads to: [Flow 5/6/7/8] -- API keys must be configured before voice recording works
- Returns from: [Flow 2] step 2 (onboarding only sets OpenRouter key)

### Verification Checklist
- [x] Entry point exists and is reachable (Settings > Model tab)
- [x] All transitions have corresponding code
- [ ] Error states are handled (API test is simulated, not real)
- [x] Back navigation works (tab-based, no deep navigation)
- [x] State is preserved across config changes (remember states)

---

## Flow 5: Voice Recording (Keyboard -- Smartbar)

### Entry Point
When SpeekEZ keyboard is active and the Smartbar layout is `SmartbarLayout.SPEEKEZ` (default), the Smartbar shows preset chips and a mic button.

### Screens/States
1. **Idle state:** Smartbar shows preset chips (scrollable), add preset button (dashed circle), and mic button (teal circle)
2. **User holds mic button (>200ms):** Gesture detected via `awaitEachGesture` --> `voiceManager.startRecording(activePresetId)` --> `keyboardManager.activeState.isRecording = true` --> `inputFeedbackController.keyLongPress()` (haptic)
3. **Recording state:** Smartbar shows timer (`MM:SS` in red), mic button turns red with pulse animation
4. **User releases mic button:** `voiceManager.stopRecording()` --> `keyboardManager.activeState.isRecording = false` --> `inputFeedbackController.keyPress()` (haptic)
5. **Processing state:** Smartbar shows "Transcribing..." in purple, mic button shows purple spinner animation
6. **Done state:** Mic button turns green with checkmark, auto-dismisses to IDLE after 1.5s
7. **Error state:** Red error banner in smartbar with error message, auto-dismisses after 3s

**Alternative: Hold preset chip:**
1. **User holds a preset chip (>200ms):** `voiceManager.startRecording(preset.id)` with that specific preset
2. Follows same Recording -> Processing -> Done/Error flow
3. **User taps preset chip (<200ms):** Sets `prefs.speekez.activePresetId = preset.id` (selects it as active)

### State Transitions
- `voiceState` (VoiceState enum: IDLE -> RECORDING -> PROCESSING -> DONE -> IDLE, or RECORDING/PROCESSING -> ERROR -> IDLE)
- `keyboardManager.activeState.isRecording` (boolean): Controls keyboard visual state
- `activePresetId` (Long, pref key: `speekez__active_preset_id`): Which preset is currently active for the mic button
- `timerSeconds` (Int, local state): Incremented every 1000ms during RECORDING via LaunchedEffect

### Code References
- `/tmp/speekez-repo/app/src/main/kotlin/dev/patrickgold/florisboard/ime/smartbar/Smartbar.kt:150-203` -- `Smartbar()` composable: checks `smartbarLayout == SmartbarLayout.SPEEKEZ` to render `SpeekEZSmartbarMainRow`
- `/tmp/speekez-repo/app/src/main/kotlin/dev/patrickgold/florisboard/ime/smartbar/Smartbar.kt:211-403` -- `SpeekEZSmartbarMainRow()`: Full implementation with preset chips, mic button, timer, error banner, no-API-key banner
- `/tmp/speekez-repo/app/src/main/kotlin/dev/patrickgold/florisboard/ime/smartbar/Smartbar.kt:430-529` -- `MicButton()`: Hold gesture (200ms threshold), visual states (pulse/spinner animations)
- `/tmp/speekez-repo/app/src/main/kotlin/dev/patrickgold/florisboard/ime/smartbar/Smartbar.kt:488-514` -- Gesture handling: `awaitEachGesture` with `withTimeoutOrNull(200)` to distinguish tap from hold
- `/tmp/speekez-repo/app/src/main/kotlin/dev/patrickgold/florisboard/ime/smartbar/PresetChip.kt:53-141` -- `PresetChip()`: Same 200ms hold/tap gesture, shows emoji icon
- `/tmp/speekez-repo/app/src/main/kotlin/dev/patrickgold/florisboard/ime/smartbar/Smartbar.kt:405-428` -- `AddPresetButton()`: Dashed circle with "+" icon, shows "Need API Key" banner if no keys
- `/tmp/speekez-repo/voice/src/main/kotlin/com/speekez/voice/VoiceManager.kt:133-175` -- `startRecording()`: Validates preset, permission, network, API key; starts AudioHandler
- `/tmp/speekez-repo/voice/src/main/kotlin/com/speekez/voice/VoiceManager.kt:180-186` -- `stopRecording()`: Haptic feedback, transitions state machine to PROCESSING
- `/tmp/speekez-repo/voice/src/main/kotlin/com/speekez/voice/VoiceManager.kt:188-253` -- `processAudio()`: STT transcription, optional refinement, text insertion via InputConnection, clipboard copy, database save, stats update

### Edge Cases
- **No API key configured:** `isNoApiKey` check in Smartbar. Entire row rendered at 40% opacity. Tapping/holding shows "Need API Key" orange banner that auto-dismisses after 5s
- **No internet:** `NetworkUtils.isOnline()` check in `startRecording()`. Error state: "No internet connection"
- **No microphone permission:** `PermissionUtils.hasMicPermission()` check. Error state: "Microphone permission denied"
- **Preset not found:** Error state: "Preset not found"
- **60-second auto-stop:** `AudioHandler.startTimer()` triggers `onAutoStop` after 60s. `VoiceStateMachine.startRecording()` also has a 60s auto-transition timer
- **Keyboard hidden during recording:** `FlorisImeService.onWindowHidden()` calls `voiceManager.onWindowHidden()` which calls `cancelRecording()` if recording
- **Hold cancelled (finger moves off button):** `waitForUpOrCancellation()` returns null -> `onHoldCancel()` -> `voiceManager.cancelRecording()` (stops audio, deletes temp file, resets state)
- **Text insertion:** Result committed via `inputConnectionProvider?.invoke()?.commitText(finalResult, 1)`. If InputConnection is null, text still goes to clipboard if enabled
- **Copy to clipboard:** Controlled by `isCopyToClipboardEnabled()` which reads `speekez__copy_to_clipboard` from SharedPreferences (default: true)

### Connections to Other Flows
- Requires: [Flow 4] API keys configured
- Requires: [Flow 19] Microphone permission granted
- Feeds: [Flow 11] History (saves Transcription entity)
- Feeds: [Flow 20] Dashboard (updates DailyStats)

### Verification Checklist
- [x] Entry point exists and is reachable (Smartbar in keyboard)
- [x] All transitions have corresponding code (VoiceStateMachine)
- [x] Error states are handled (no key, no internet, no permission, no preset)
- [x] Back navigation works (N/A - keyboard context)
- [ ] State is preserved across config changes (keyboard window may recreate)

---

## Flow 6: Voice Recording (Widget 1x1)

### Entry Point
User taps the SpeekEZ 1x1 home screen widget.

### Screens/States
1. **Widget on home screen:** Shows preset emoji icon
2. **User taps widget:** `PendingIntent` launches `SpeekEZWidgetRecordingActivity` with `EXTRA_APPWIDGET_ID`
3. **Recording Activity:** Transparent overlay with `RecordingOverlay` composable
   - Shows `BigMicButton` (80dp, color-changing)
   - Recording immediately starts via `voiceManager.startRecording(presetId)`
4. **Recording state:** Red mic button with pulse, timer showing `MM:SS`, "Recording..." text
5. **User taps mic button during recording:** `onStop()` -> `voiceManager.stopRecording()`
6. **Processing state:** Purple mic button with spinner, "Transcribing..." text
7. **Done state:** Green mic button with checkmark, "Done!" text, auto-finishes after 1.5s
8. **Error state:** Red mic button with error icon, error message displayed, auto-finishes after 3s
9. **User taps outside overlay:** `onCancel()` -> `voiceManager.cancelRecording()` -> `finish()`

### State Transitions
- Same `VoiceState` state machine as Flow 5
- `timerSeconds` (local state, incremented every 1000ms)
- Activity finishes automatically when state reaches DONE (after 1.5s delay) or ERROR (after 3s delay)
- On transcription complete: text copied to system clipboard via `copyToClipboard()`

### Code References
- `/tmp/speekez-repo/widget/src/main/kotlin/com/speekez/widget/SpeekEZWidget1x1.kt:1-103` -- Widget provider: sets up `PendingIntent` to launch `SpeekEZWidgetRecordingActivity`
- `/tmp/speekez-repo/widget/src/main/kotlin/com/speekez/widget/SpeekEZWidgetRecordingActivity.kt:46-104` -- Recording activity: starts recording, observes state, auto-finishes
- `/tmp/speekez-repo/widget/src/main/kotlin/com/speekez/widget/SpeekEZWidgetRecordingActivity.kt:111-254` -- `RecordingOverlay` and `BigMicButton` composables with animations
- `/tmp/speekez-repo/widget/src/main/kotlin/com/speekez/widget/SpeekEZWidgetRecordingActivity.kt:70` -- `voiceManager.startRecording(presetId.toInt())` called immediately in onCreate
- `/tmp/speekez-repo/widget/src/main/kotlin/com/speekez/widget/SpeekEZWidgetRecordingActivity.kt:72-78` -- `onTranscriptionComplete` callback copies to clipboard and shows toast

### Edge Cases
- **Default preset ID:** If widget has no configured preset, `getPresetId()` returns `1L` (first preset)
- **Activity destroyed during recording:** `onDestroy()` clears `onTranscriptionComplete` callback but does NOT cancel recording (recording continues in VoiceManager scope)
- **No API key:** VoiceManager.startRecording() returns error via state machine -- error shown in overlay
- **Clipboard access from widget:** Uses `ClipboardManager` system service directly (not FlorisBoard clipboard)

### Connections to Other Flows
- Requires: [Flow 13] Widget configuration to set preset
- Uses: [Flow 17] Voice Pipeline
- Feeds: [Flow 11] History, [Flow 20] Dashboard

### Verification Checklist
- [x] Entry point exists and is reachable (widget tap -> PendingIntent)
- [x] All transitions have corresponding code
- [x] Error states are handled (shown in overlay, auto-dismiss)
- [ ] Back navigation works (tapping outside cancels, back button not explicitly handled)
- [ ] State is preserved across config changes (activity may recreate)

---

## Flow 7: Voice Recording (Widget 2x1)

### Entry Point
User taps the mic button on the SpeekEZ 2x1 home screen widget.

### Screens/States
1. **Widget on home screen:** Shows preset emoji, name, input languages, and a mic button
2. **User taps mic button:** `PendingIntent` launches `VoiceShortcutActivity` with `preset_id` extra
3. **VoiceShortcutActivity:** Transparent overlay with recording status card
4. See [Flow 9: Voice Recording (Voice Shortcut Activity)] for detailed flow

### State Transitions
- Preset ID stored in `SharedPreferences("SpeekEZWidgetPrefs", "appwidget_" + widgetId)`
- Widget shows "Tap to configure" if no preset assigned (`presetId == -1L`)
- If unconfigured, tapping whole widget launches `SpeekEZWidget2x1ConfigActivity`

### Code References
- `/tmp/speekez-repo/widget/src/main/kotlin/com/speekez/widget/SpeekEZWidget2x1.kt:1-95` -- Widget provider
- `/tmp/speekez-repo/widget/src/main/kotlin/com/speekez/widget/SpeekEZWidget2x1.kt:41-70` -- Update logic: loads preset from DB, sets up mic button PendingIntent to `VoiceShortcutActivity`
- `/tmp/speekez-repo/widget/src/main/kotlin/com/speekez/widget/SpeekEZWidget2x1.kt:72-84` -- Unconfigured state: shows "Tap to configure", launches config activity

### Edge Cases
- **Preset deleted after widget configured:** `presetDao().getPresetById(presetId)` returns null, widget shows "Preset not found"
- **No preset assigned:** Shows "Tap to configure" text; entire widget becomes a button to open `SpeekEZWidget2x1ConfigActivity`

### Connections to Other Flows
- Leads to: [Flow 9] VoiceShortcutActivity
- Requires: [Flow 14] Widget configuration (2x1)

### Verification Checklist
- [x] Entry point exists and is reachable (widget mic button)
- [x] All transitions have corresponding code
- [x] Error states are handled (preset not found, unconfigured)
- [x] Back navigation works (activity finishes on done/error)
- [ ] State is preserved across config changes (N/A - immediate action)

---

## Flow 8: Voice Recording (Floating Widget)

### Entry Point
When `prefs.speekez.floatingWidgetEnabled == true` AND `Settings.canDrawOverlays()` returns true, the `FloatingWidgetService` starts automatically from `FlorisApplication.init()`.

### Screens/States
1. **Collapsed state:** Small 44dp teal circle with mic icon, anchored to right edge of screen
   - User can drag vertically to reposition (Y position persisted in SharedPreferences)
   - User taps to expand
2. **Expanded state:** 200dp-wide card showing:
   - "SpeekEZ" gradient title with close button
   - Status indicator (if not IDLE)
   - List of all presets with emoji, name, and languages
3. **User press-and-holds a preset row:** `onStartRecording(preset.id)` called on press, `onStopRecording()` called on release
4. **Recording state:** Collapsed widget pulses and turns red; expanded widget shows red "Recording..." status
5. **Processing state:** Collapsed widget shows purple sync icon; expanded shows "Processing..." status
6. **Done state:** Collapsed widget shows green checkmark; expanded shows "Done!"
7. **Error state:** Collapsed widget shows red error icon; expanded shows error message

### State Transitions
- `isExpanded` (Boolean, local state): Toggles between collapsed and expanded views
- `voiceState` (observed from VoiceManager)
- Widget Y position persisted in `SharedPreferences("floating_widget_prefs", "y_pos")`
- Transcription results copied to system clipboard via `copyToClipboard()`

### Code References
- `/tmp/speekez-repo/widget/src/main/kotlin/com/speekez/widget/FloatingWidgetService.kt:59-196` -- Full service implementation
- `/tmp/speekez-repo/widget/src/main/kotlin/com/speekez/widget/FloatingWidgetService.kt:91-108` -- `onCreate()`: Sets up SavedStateRegistry, foreground notification, window manager, ComposeView
- `/tmp/speekez-repo/widget/src/main/kotlin/com/speekez/widget/FloatingWidgetService.kt:110-148` -- `setupComposeView()`: Creates ComposeView with lifecycle owners, sets up collapsed/expanded toggle
- `/tmp/speekez-repo/widget/src/main/kotlin/com/speekez/widget/FloatingWidgetService.kt:198-260` -- `CollapsedWidget`: 44dp circle, drag gesture, tap gesture, pulse animation during recording
- `/tmp/speekez-repo/widget/src/main/kotlin/com/speekez/widget/FloatingWidgetService.kt:262-322` -- `ExpandedWidget`: Card with preset list, status indicator
- `/tmp/speekez-repo/widget/src/main/kotlin/com/speekez/widget/FloatingWidgetService.kt:351-396` -- `PresetRow`: Press-and-hold gesture to record, highlight on press
- `/tmp/speekez-repo/app/src/main/kotlin/dev/patrickgold/florisboard/FlorisApplication.kt:141-155` -- Auto-start/stop service based on pref and overlay permission

### Edge Cases
- **No overlay permission:** `FlorisApplication.init()` checks `Settings.canDrawOverlays()`. If false, service not started even if pref is true. In GeneralSettingsScreen, toggling the switch opens `Settings.ACTION_MANAGE_OVERLAY_PERMISSION`
- **Service foreground notification:** Required for foreground service. Creates "floating_widget_service" notification channel with `IMPORTANCE_LOW`
- **FLAG_NOT_FOCUSABLE:** Window params include this flag, meaning the floating widget won't steal focus from other apps. BUT: the expanded widget's `PresetRow` uses `detectTapGestures(onPress)` which may not receive input correctly with this flag
- **Memory leak risk:** `ComposeView` is added to `WindowManager` and removed in `onDestroy()`. If service is killed without `onDestroy()`, the view leaks
- **No InputConnection:** Floating widget runs outside IME context. Text goes to clipboard only, NOT inserted into focused field

### Connections to Other Flows
- Requires: [Flow 12] Settings > General > Floating Widget toggle
- Uses: [Flow 17] Voice Pipeline
- Feeds: [Flow 11] History, [Flow 20] Dashboard

### Verification Checklist
- [x] Entry point exists and is reachable (auto-started from FlorisApplication)
- [x] All transitions have corresponding code
- [x] Error states are handled (VoiceManager error states reflected in UI)
- [ ] Back navigation works (N/A - overlay)
- [x] State is preserved across config changes (service persists)

---

## Flow 9: Voice Recording (Voice Shortcut Activity)

### Entry Point
Launched via explicit `Intent` from widget 2x1 mic button, with `preset_id` extra.

### Screens/States
1. **onCreate:** Extracts `preset_id` from intent. If `-1`, finishes immediately
2. **Sets up onTranscriptionComplete callback:** Copies result to FlorisBoard clipboard via `clipboardManager.addNewPlaintext(text)`
3. **Starts recording immediately:** `voiceManager.startRecording(presetId)`
4. **Overlay UI:** Semi-transparent black background with centered card showing:
   - State text: "Starting...", "Recording...", "Transcribing...", "Done!", "Error"
   - Error message if applicable
   - "Stop Recording" button during RECORDING state
   - "Close" button during ERROR or DONE states
5. **Auto-dismiss:** `LaunchedEffect` monitors state. Once `hasStarted == true` and state returns to IDLE, activity finishes

### State Transitions
- `hasStarted` (Boolean, local state): Set to true when state first leaves IDLE. Prevents premature finish
- Activity finishes when state returns to IDLE after having started

### Code References
- `/tmp/speekez-repo/app/src/main/kotlin/com/speekez/app/VoiceShortcutActivity.kt:21-128` -- Full implementation
- `/tmp/speekez-repo/app/src/main/kotlin/com/speekez/app/VoiceShortcutActivity.kt:28-31` -- Preset ID extraction, early finish if invalid
- `/tmp/speekez-repo/app/src/main/kotlin/com/speekez/app/VoiceShortcutActivity.kt:37-39` -- Clipboard callback uses FlorisBoard's `clipboardManager.addNewPlaintext()`
- `/tmp/speekez-repo/app/src/main/kotlin/com/speekez/app/VoiceShortcutActivity.kt:43` -- Immediate `startRecording(presetId)`
- `/tmp/speekez-repo/app/src/main/kotlin/com/speekez/app/VoiceShortcutActivity.kt:51-58` -- Auto-finish logic via `hasStarted` flag

### Edge Cases
- **No preset_id in intent:** Activity finishes immediately with `Log.e()` message
- **Preset not found:** VoiceManager sets error "Preset not found", shown in overlay
- **Activity paused during recording:** `onPause()` does nothing -- comment says recording may continue
- **Uses FlorisBoard clipboard:** Unlike widget recording activity, this one uses `(application as FlorisApplication).clipboardManager` for clipboard access

### Connections to Other Flows
- Called from: [Flow 7] Widget 2x1
- Uses: [Flow 17] Voice Pipeline

### Verification Checklist
- [x] Entry point exists and is reachable (Intent with preset_id)
- [x] All transitions have corresponding code
- [x] Error states are handled (no preset_id, VoiceManager errors)
- [ ] Back navigation works (activity finishes on back, but recording may continue in VoiceManager)
- [x] State is preserved across config changes (single instance launch mode)

---

## Flow 10: Preset Management

### Entry Point
Settings tab > "Presets" tab in the main SpeekEZ app.

### Screens/States
1. **Preset list:** `LazyColumn` showing all presets sorted by `usage_count DESC`
   - Each card shows emoji, name, language summary, refinement level, model tier
   - FAB (+) button to add new preset (max 10 presets)
2. **User taps preset card:** `PresetEditForm` slides in from right
3. **PresetEditForm:** Full-screen form with:
   - Close (X) button and "Save" text button in header
   - Name text field
   - Icon emoji field with live preview (max 2 chars)
   - Input Languages multi-select (en, te, hi, es, fr, de, it, ja, ko, zh) with default language star
   - Output Languages multi-select (same list)
   - Refinement Level selector (None / Light / Full)
   - Model Tier selector (Cheap / Best) -- Custom tier force-mapped to Cheap on save
   - System Prompt text area
   - Delete button (red, only shown for existing presets when more than 1 preset exists)
4. **User taps Save:** Validates name and icon not blank, calls `presetDao.insertPreset()` or `presetDao.updatePreset()`, calls `SpeekEZWidget1x1.updateAllWidgets()` on update, closes form
5. **User taps Delete:** Confirmation dialog, then `presetDao.deletePreset()`, closes form

### State Transitions
- `editingPreset` (Preset?, local state): The preset currently being edited
- `isFormVisible` (Boolean, local state): Controls slide-in animation
- `showDeleteDialog` (Boolean, local state): Controls delete confirmation
- Form fields (name, iconEmoji, inputLanguages, etc.) are local `mutableStateOf` initialized from preset

### Code References
- `/tmp/speekez-repo/app/src/main/kotlin/com/speekez/app/screens/settings/PresetSettingsScreen.kt:32-116` -- `PresetSettingsScreen()`: List + FAB + animated form
- `/tmp/speekez-repo/app/src/main/kotlin/com/speekez/app/screens/settings/PresetSettingsScreen.kt:118-153` -- `PresetCard()`: Display card for each preset
- `/tmp/speekez-repo/app/src/main/kotlin/com/speekez/app/screens/settings/PresetSettingsScreen.kt:155-424` -- `PresetEditForm()`: Full edit form with all fields
- `/tmp/speekez-repo/app/src/main/kotlin/com/speekez/app/screens/settings/PresetSettingsScreen.kt:426-441` -- `createEmptyPreset()`: Factory for new presets with defaults
- `/tmp/speekez-repo/app/src/main/kotlin/com/speekez/app/screens/settings/PresetSettingsScreen.kt:443-510` -- `LanguageMultiSelect()`: Multi-select chip component with default language indicator
- `/tmp/speekez-repo/data/src/main/kotlin/com/speekez/data/entity/Preset.kt:1-35` -- Preset Room entity
- `/tmp/speekez-repo/data/src/main/kotlin/com/speekez/data/dao/PresetDao.kt:1-34` -- PresetDao interface
- `/tmp/speekez-repo/data/src/main/kotlin/com/speekez/data/seed/PresetSeeder.kt:1-64` -- Seeds 3 default presets (AI Mode, Personal, Work) if DB is empty

### Edge Cases
- **Max 10 presets:** FAB only clickable when `presets.size < 10`
- **Min 1 preset:** Delete button only shown when `presets.size > 1`
- **Empty name or icon:** Save button validates `name.isNotBlank() && iconEmoji.isNotBlank()` -- silently does nothing if invalid (no error message shown)
- **Custom model tier:** Force-mapped to `ModelTier.CHEAP` on save at line 213: `modelTier = if (modelTier == ModelTier.CUSTOM) ModelTier.CHEAP else modelTier`
- **Widget update on preset edit:** `SpeekEZWidget1x1.updateAllWidgets(context)` called after update to refresh widget display
- **Default language logic:** When a selected language is removed and it was the default, the default switches to the first remaining language
- **Emoji length limit:** `iconEmoji` input capped at 2 characters (for surrogate pair emojis)
- **Database seeding:** `PresetSeeder.seedDefaultPresetsIfEmpty()` creates 3 presets on first run. Called from... (NOT explicitly shown in code read -- likely called from FlorisApplication or database builder callback)

### Connections to Other Flows
- Affects: [Flow 5/6/7/8/9] -- presets determine recording behavior
- Affects: [Flow 13/14] -- widget configuration shows preset list
- Affects: [Flow 11] -- transcriptions reference preset IDs

### Verification Checklist
- [x] Entry point exists and is reachable (Settings > Presets tab)
- [x] All transitions have corresponding code
- [ ] Error states are handled (empty name/icon silently fails -- gap)
- [x] Back navigation works (close button on edit form)
- [x] State is preserved across config changes (Compose state)

---

## Flow 11: History Browsing

### Entry Point
Bottom nav > "History" tab in the main SpeekEZ app.

### Screens/States
1. **History list:** Search bar + filter chips + transcription list
   - **Search bar:** Filters by `rawText` or `refinedText` content (case-insensitive)
   - **Filter chips:** All, Today, This Week, Favorites (with badge count)
   - **Hint text:** "Tap to open - Hold to copy"
2. **Transcription item:** Card with preset emoji, timestamp, word count, text preview (1 line), favorite star, arrow icon
   - **Tap:** Opens detail panel (slides in from right)
   - **Long press:** Copies `refinedText` (or `rawText` if refined is blank) to clipboard, shows Toast
3. **Detail panel:** Full-screen overlay with:
   - Back arrow, formatted date, duration, word count, preset name
   - Copy button, Favorite toggle button (star)
   - Full transcription text (scrollable)

### State Transitions
- `searchQuery` (String, local state)
- `selectedFilter` (HistoryFilter enum: ALL, TODAY, THIS_WEEK, FAVORITES)
- `selectedTranscriptionId` (Long?, local state): Controls detail panel visibility
- `lastSelectedTranscription` (Transcription?, local state): Keeps reference for exit animation

### Code References
- `/tmp/speekez-repo/app/src/main/kotlin/com/speekez/app/screens/HistoryScreen.kt:54-235` -- `HistoryScreen()`: Full list implementation
- `/tmp/speekez-repo/app/src/main/kotlin/com/speekez/app/screens/HistoryScreen.kt:74-88` -- Filtering logic: search + filter applied together
- `/tmp/speekez-repo/app/src/main/kotlin/com/speekez/app/screens/HistoryScreen.kt:237-319` -- `HistoryDetailPanel()`: Detail view with copy and favorite
- `/tmp/speekez-repo/app/src/main/kotlin/com/speekez/app/screens/HistoryScreen.kt:321-413` -- `TranscriptionItem()`: Card with combinedClickable for tap and long press
- `/tmp/speekez-repo/data/src/main/kotlin/com/speekez/data/dao/TranscriptionDao.kt:1-36` -- TranscriptionDao: getAllTranscriptions, setFavorite, getOverallAvgWpm
- `/tmp/speekez-repo/data/src/main/kotlin/com/speekez/data/entity/Transcription.kt:1-39` -- Transcription entity with foreign key to Preset

### Edge Cases
- **No transcriptions:** Shows `EmptyState` with mic icon: "No transcriptions yet. Hold the mic button to start."
- **No search results:** Shows "No results found" text
- **Deleted preset:** Preset lookup returns null, shows default emoji ("\uD83C\uDFA4") and "Unknown" name
- **Favorites badge:** Dynamically counts `transcriptions.count { it.isFavorite }`, shown on Favorites chip only when > 0
- **Timestamp formatting:** Relative formatting -- today shows time only, last 7 days shows day+time, older shows month+day+time
- **Foreign key on delete:** `ForeignKey.SET_NULL` -- deleting a preset sets `preset_id` to null on transcriptions

### Connections to Other Flows
- Fed by: [Flow 5/6/7/8/9] -- voice recordings create transcriptions
- References: [Flow 10] -- displays preset names/emojis

### Verification Checklist
- [x] Entry point exists and is reachable (bottom nav)
- [x] All transitions have corresponding code
- [x] Error states are handled (empty states, deleted presets)
- [x] Back navigation works (back arrow on detail panel)
- [x] State is preserved across config changes (remember states)

---

## Flow 12: Settings

### Entry Point
Bottom nav > "Settings" tab in the main SpeekEZ app.

### Screens/States
1. **Settings screen:** Three tabs: General, Model, Presets
2. **General tab:** See details below
3. **Model tab:** See [Flow 4]
4. **Presets tab:** See [Flow 10]

### General Tab Details
1. **Copy to Clipboard toggle:** Controls `prefs.speekez.copyToClipboard` (default: true)
2. **Haptic Feedback toggle:** Controls `prefs.speekez.hapticEnabled` (default: true)
3. **Floating Widget toggle:** Controls `prefs.speekez.floatingWidgetEnabled` (default: false)
   - If toggled ON without overlay permission: Opens `Settings.ACTION_MANAGE_OVERLAY_PERMISSION` intent, toggle NOT enabled
4. **Theme nav item:** Opens `ThemePickerDialog` with 3 options: Light, Dark, System Default
   - Controls `prefs.other.settingsTheme` (enum: LIGHT, DARK, AUTO)

### Code References
- `/tmp/speekez-repo/app/src/main/kotlin/com/speekez/app/screens/settings/SettingsScreen.kt:1-59` -- Tab layout with 3 tabs
- `/tmp/speekez-repo/app/src/main/kotlin/com/speekez/app/screens/settings/GeneralSettingsScreen.kt:35-266` -- General settings with toggles and theme picker
- `/tmp/speekez-repo/app/src/main/kotlin/com/speekez/app/screens/settings/GeneralSettingsScreen.kt:104-152` -- `SettingToggleItem()`: Reusable toggle component
- `/tmp/speekez-repo/app/src/main/kotlin/com/speekez/app/screens/settings/GeneralSettingsScreen.kt:188-236` -- `ThemePickerDialog()`: Three radio button options

### Edge Cases
- **Overlay permission check:** `Settings.canDrawOverlays(context)` checked when enabling floating widget. If not granted, redirect to system settings, toggle returns `false` to prevent enabling
- **Theme application:** `SpeekEZTheme` in `SpeekEZActivity` observes `prefs.other.settingsTheme` and recomposes with new color scheme immediately

### Connections to Other Flows
- Controls: [Flow 8] Floating Widget (enable/disable)
- Controls: [Flow 5] Haptic feedback and clipboard behavior
- Contains: [Flow 4] Model settings, [Flow 10] Preset management

### Verification Checklist
- [x] Entry point exists and is reachable (bottom nav)
- [x] All transitions have corresponding code
- [x] Error states are handled (overlay permission)
- [x] Back navigation works (tab-based)
- [x] State is preserved across config changes (JetPref datastore)

---

## Flow 13: Widget Configuration (1x1)

### Entry Point
Android home screen > Add widget > SpeekEZ Voice (1x1) > Configuration activity launches.

There are actually TWO configuration activities for the 1x1 widget:
- `SpeekEZWidgetConfigActivity` (in `widget` module)
- `WidgetConfigActivity` (in `app/widgets` package)

Both are declared in AndroidManifest with `APPWIDGET_CONFIGURE` intent filter. The widget XML (`widget_info_1x1`) determines which one is used.

### Screens/States

**SpeekEZWidgetConfigActivity (widget module):**
1. **Preset list:** `LazyColumn` with `WidgetConfigScreen` showing presets as cards
2. User taps a preset card --> `onPresetSelected(preset)` called
3. Saves preset ID via `SpeekEZWidget1x1.savePresetId()` to SharedPreferences
4. Sends update broadcast to widget
5. Sets `RESULT_OK` and finishes

**WidgetConfigActivity (app module):**
1. **Preset list:** `LazyColumn` with selectable cards (border highlight on selection)
2. User taps to select, then taps checkmark in toolbar to confirm
3. Saves preset ID to `SharedPreferences("speekez_widgets", "widget_" + widgetId)` -- NOTE: different SharedPreferences name!
4. Sets `RESULT_OK` and finishes

### Code References
- `/tmp/speekez-repo/widget/src/main/kotlin/com/speekez/widget/SpeekEZWidgetConfigActivity.kt:26-143` -- Widget module config
- `/tmp/speekez-repo/app/src/main/kotlin/com/speekez/app/widgets/WidgetConfigActivity.kt:34-191` -- App module config
- `/tmp/speekez-repo/widget/src/main/kotlin/com/speekez/widget/SpeekEZWidget1x1.kt:76-102` -- Static companion methods for saving/getting preset ID

### Edge Cases
- **Invalid widget ID:** Both activities check `INVALID_APPWIDGET_ID` and finish immediately
- **No presets:** Widget module shows `CircularProgressIndicator` (presets loading); App module shows "No presets found" message
- **Back button:** Both set `RESULT_CANCELED` initially, so backing out cancels widget placement
- **Two config activities bug:** Both registered for `APPWIDGET_CONFIGURE` in manifest. The actual behavior depends on which one the widget XML (`widget_info_1x1`) references. The `SpeekEZWidgetConfigActivity` saves to `SpeekEZWidget1x1.savePresetId()`, while `WidgetConfigActivity` saves to a different SharedPreferences store -- potential desync.

### Connections to Other Flows
- Configures: [Flow 6] Widget 1x1 recording
- Reads: [Flow 10] Preset list from database

### Verification Checklist
- [x] Entry point exists and is reachable (widget add flow)
- [x] All transitions have corresponding code
- [x] Error states are handled (invalid widget ID, empty presets)
- [x] Back navigation works (RESULT_CANCELED default)
- [ ] State is preserved across config changes (two config activities may conflict)

---

## Flow 14: Widget Configuration (2x1)

### Entry Point
Android home screen > Add widget > SpeekEZ Preset (2x1) > Configuration activity launches.

### Screens/States
1. **SpeekEZWidget2x1ConfigActivity:** `PresetSelectionScreen` with dark theme
2. Shows "Select a Preset" heading
3. `LazyColumn` with `ListItem` rows (emoji, name, languages)
4. User taps a preset row --> `handlePresetSelected(preset)` called
5. Saves preset ID via `SpeekEZWidget2x1.savePresetId()` to `SharedPreferences("SpeekEZWidgetPrefs")`
6. Updates widget via `SpeekEZWidget2x1.updateAppWidget()`
7. Sets `RESULT_OK` and finishes

### Code References
- `/tmp/speekez-repo/widget/src/main/kotlin/com/speekez/widget/SpeekEZWidget2x1ConfigActivity.kt:22-113` -- Full config activity
- `/tmp/speekez-repo/widget/src/main/kotlin/com/speekez/widget/SpeekEZWidget2x1.kt:89-93` -- `savePresetId()` static method

### Edge Cases
- **Invalid widget ID:** Checks `INVALID_APPWIDGET_ID`, finishes immediately
- **Empty preset list:** Shows empty LazyColumn (no explicit empty state message)
- **Transparent theme:** Uses `FlorisAppTheme.Transparent`, but `WidgetConfigTheme` applies dark theme

### Connections to Other Flows
- Configures: [Flow 7] Widget 2x1 recording
- Reads: [Flow 10] Preset list from database

### Verification Checklist
- [x] Entry point exists and is reachable (widget add flow)
- [x] All transitions have corresponding code
- [ ] Error states are handled (no empty state for zero presets)
- [x] Back navigation works (RESULT_CANCELED default)
- [ ] State is preserved across config changes (single activity)

---

## Flow 15: Keyboard Switching / IME Service Lifecycle

### Entry Point
Any text field in any Android app triggers the IME lifecycle.

### Screens/States
1. **onStartInput:** `EditorInfo` wrapped into `FlorisEditorInfo`, passed to `editorInstance.handleStartInput()`
2. **onStartInputView:** Sets `imeUiMode` to `ImeUiMode.TEXT` (unless clipboard mode and hide-on-next disabled), updates selection mode
3. **onWindowShown:** Calls `voiceManager.onWindowShown()` (currently a no-op log), updates window controller
4. **Keyboard visible:** Smartbar renders based on `SmartbarLayout` (default: `SPEEKEZ`)
5. **onWindowHidden:** Calls `voiceManager.onWindowHidden()` which cancels any active recording, resets IME state
6. **onFinishInputView / onFinishInput:** Cleans up editor instance, clears inline suggestions

### Keyboard Modes
- **Text mode:** Standard QWERTY keyboard + SpeekEZ Smartbar
- **Clipboard mode:** Clipboard history view
- **Media mode:** Emoji/emoticon/kaomoji picker
- **Selection mode:** Text selection actions

### IME Switching
- `switchToPrevInputMethod()`: Uses `switchToPreviousInputMethod()` (API 28+) or deprecated `switchToLastInputMethod()`
- `switchToNextInputMethod()`: Uses `switchToNextInputMethod(false)` (API 28+) or deprecated token-based method
- `switchToVoiceInputMethod()`: Iterates `enabledInputMethodList`, finds subtypes with `mode == "voice"`, switches to it
- `showImePicker()`: Shows system IME picker dialog
- `launchSettings()`: Hides keyboard, launches `FlorisAppActivity` with clear-top flags

### Code References
- `/tmp/speekez-repo/app/src/main/kotlin/dev/patrickgold/florisboard/FlorisImeService.kt:90-543` -- Full IME service
- `/tmp/speekez-repo/app/src/main/kotlin/dev/patrickgold/florisboard/FlorisImeService.kt:281-286` -- `onCreate()`: Sets up voiceManager.inputConnectionProvider, window controller
- `/tmp/speekez-repo/app/src/main/kotlin/dev/patrickgold/florisboard/FlorisImeService.kt:426-449` -- `onWindowShown/Hidden`: Voice manager integration
- `/tmp/speekez-repo/app/src/main/kotlin/dev/patrickgold/florisboard/FlorisImeService.kt:234-257` -- `switchToVoiceInputMethod()`: Finds and switches to voice IME

### Edge Cases
- **No voice IME installed:** Shows toast "Failed to find voice IME, do you have one installed?"
- **Physical keyboard attached:** `onEvaluateInputViewShown()` returns true if `KEYBOARD_NOKEYS` or `showOnScreenKeyboard` pref is true
- **Fullscreen/landscape mode:** Controlled by `landscapeInputUiMode` pref (DYNAMICALLY_SHOW, NEVER_SHOW, ALWAYS_SHOW)
- **Recording cancelled on hide:** `onWindowHidden()` calls `voiceManager.onWindowHidden()` which cancels recording if active

### Connections to Other Flows
- Contains: [Flow 5] Keyboard voice recording
- Leads to: [Flow 3] FlorisBoard setup when IME not configured

### Verification Checklist
- [x] Entry point exists and is reachable (system IME lifecycle)
- [x] All transitions have corresponding code
- [x] Error states are handled (no voice IME, recording cancellation)
- [x] Back navigation works (system handles IME lifecycle)
- [x] State is preserved across config changes (onConfigurationChanged handled)

---

## Flow 16: Error Flows

### Entry Point
Various error conditions across the application.

### Error Scenarios

#### 1. No API Key
- **Where:** Smartbar mic button hold, any voice recording attempt
- **Detection:** `ApiRouterManager.getSttClient()` returns null when `ApiMode == NO_KEYS`
- **User sees:**
  - Smartbar: Orange "Need API Key" banner (auto-dismisses 5s)
  - Smartbar: Entire row at 40% opacity
  - VoiceManager: Error state "API key not configured" (auto-dismisses 3s)
- **Code:** `VoiceManager.kt:157-161`, `Smartbar.kt:225-226, 378-401`

#### 2. No Internet Connection
- **Where:** `VoiceManager.startRecording()`
- **Detection:** `NetworkUtils.isOnline(context)` returns false
- **User sees:** Error state "No internet connection" with haptic error buzz (200ms)
- **Code:** `VoiceManager.kt:151-155`, `NetworkUtils.kt:12-20`

#### 3. Microphone Permission Denied
- **Where:** `VoiceManager.startRecording()`
- **Detection:** `PermissionUtils.hasMicPermission(context)` returns false
- **User sees:** Error state "Microphone permission denied" with haptic error buzz
- **Code:** `VoiceManager.kt:145-149`, `PermissionUtils.kt:37-42`

#### 4. Preset Not Found
- **Where:** `VoiceManager.startRecording()`
- **Detection:** `presetDao.getPresetById(presetId)` returns null
- **User sees:** Error state "Preset not found" with haptic error buzz
- **Code:** `VoiceManager.kt:138-141`

#### 5. Audio Recording Failure
- **Where:** `AudioHandler.start()`
- **Detection:** `MediaRecorder` throws exception during `prepare()` or `start()`
- **User sees:** No explicit error shown (AudioHandler calls `cleanup()` silently). VoiceManager may or may not detect this depending on timing
- **Code:** `AudioHandler.kt:52-82`

#### 6. Audio Processing Failure
- **Where:** `VoiceManager.handleProcessing()` or `processAudio()`
- **Detection:** `audioHandler.stop()` returns null, or API call throws exception
- **User sees:** Error state with exception message, haptic error buzz
- **Code:** `VoiceManager.kt:114-126, 243-246`

#### 7. STT API Error
- **Where:** `VoiceManager.processAudio()` via SttClient.transcribe()
- **Detection:** Exception thrown during API call
- **User sees:** Error state with `e.message` or "Unknown error during processing"
- **Code:** `VoiceManager.kt:243-246`

#### 8. Refinement API Error
- **Where:** `VoiceManager.processAudio()` via RefinementClient.refine()
- **Detection:** Exception thrown, or refinement client is null
- **User sees:** Error state with exception message
- **Code:** `VoiceManager.kt:205-208`

#### 9. API Test Failure
- **Where:** `ModelSettingsScreen.performApiTest()`
- **Detection:** `NetworkUtils.isOnline()` false, or exception during test
- **User sees:** Snackbar "API Test Failed" or "Error: [message]"
- **Code:** `ModelSettingsScreen.kt:465-479`

#### 10. App Crash
- **Where:** Anywhere in the application
- **Detection:** `CrashUtility.install()` sets up crash handler
- **User sees:** `CrashDialogActivity` with crash details
- **Code:** `FlorisApplication.kt:109`, `CrashDialogActivity.kt`

### Error State Auto-Dismissal
- `VoiceState.ERROR` auto-dismisses to `VoiceState.IDLE` after **3 seconds** (`VoiceStateMachine.kt:109-116`)
- `VoiceState.DONE` auto-dismisses to `VoiceState.IDLE` after **1.5 seconds** (`VoiceStateMachine.kt:89-95`)
- "Need API Key" banner in Smartbar auto-dismisses after **5 seconds** (`Smartbar.kt:246-249`)

### Haptic Feedback for Errors
- All error states trigger `hapticManager.vibrateError()`: 200ms buzz at amplitude 200
- Only fires if both SpeekEZ haptic pref AND system haptic setting are enabled

### Verification Checklist
- [x] Entry points exist for all error scenarios
- [x] All error transitions have corresponding code
- [x] Error states auto-dismiss appropriately
- [ ] Some errors are silent (AudioHandler failure -- gap)
- [x] Error messages are user-friendly

---

## Flow 17: Voice Pipeline (Internal State Machine)

### Entry Point
Any call to `VoiceManager.startRecording()`.

### State Machine

```
     startRecording()                stopRecording()           setDone()
IDLE ──────────────> RECORDING ──────────────> PROCESSING ─────────> DONE
                        │                          │                   │
                        │ 60s auto-stop             │                   │ 1.5s auto
                        ├─────────────────> PROCESSING                 │
                        │                          │                   │
                        │ setError()               │ setError()        │
                        ├─────────> ERROR          ├────────> ERROR    v
                        │             │            │            │    IDLE
                        │             │ 3s auto    │            │ 3s auto
                        │             v            │            v
                        │           IDLE           │          IDLE
                        │
                        │ cancelRecording()
                        └─────────> IDLE (immediate reset)
```

### Pipeline Steps (processAudio)
1. **Get STT client** from `ApiRouterManager.getSttClient()` based on `ApiMode`
2. **Get STT model** from `ApiRouterManager.getSttModel(preset.modelTier)` based on tier
3. **Build language list:** `[preset.defaultInputLanguage] + (preset.inputLanguages - defaultInputLanguage)` (prioritized)
4. **Transcribe:** `sttClient.transcribe(file, sttModel, prioritizedLanguages)` returns raw text
5. **Refine (if enabled):** If `preset.refinementLevel != RefinementLevel.NONE`:
   - Get refinement client and model
   - `refinementClient.refine(rawText, refinementModel, preset.systemPrompt)` returns refined text
6. **Insert text:** `inputConnectionProvider?.invoke()?.commitText(finalResult, 1)` (keyboard context only)
7. **Copy to clipboard** (if enabled): Via system `ClipboardManager` AND `onTranscriptionComplete` callback
8. **Calculate stats:** Word count, WPM, time saved (wordCount / 75.0 minutes)
9. **Save transcription:** `transcriptionDao.insert(transcription)` to Room database
10. **Update daily stats:** `dailyStatsDao.insertOrUpdate()` with rolling average WPM
11. **Increment preset usage:** `presetDao.incrementUsageCount(preset.id)`
12. **Cleanup:** Delete temp audio file

### Audio Recording Details
- Format: MPEG_4 container, AAC encoder, 16kHz sample rate, mono channel
- Source: `AudioSource.VOICE_RECOGNITION`
- File location: `context.cacheDir/voice_temp_[timestamp].m4a`
- Max duration: 60 seconds (enforced by both AudioHandler timer AND VoiceStateMachine timer)

### Code References
- `/tmp/speekez-repo/voice/src/main/kotlin/com/speekez/voice/VoiceStateMachine.kt:1-127` -- Complete state machine
- `/tmp/speekez-repo/voice/src/main/kotlin/com/speekez/voice/VoiceManager.kt:1-319` -- Pipeline orchestrator
- `/tmp/speekez-repo/voice/src/main/kotlin/com/speekez/voice/AudioHandler.kt:1-162` -- MediaRecorder wrapper
- `/tmp/speekez-repo/voice/src/main/kotlin/com/speekez/voice/VoiceHapticManager.kt:1-116` -- Haptic feedback patterns
- `/tmp/speekez-repo/api/src/main/kotlin/com/speekez/api/ApiRouterManager.kt:1-85` -- API routing

### Edge Cases
- **Dual 60s timer:** Both `AudioHandler.startTimer()` and `VoiceStateMachine.startRecording()` set 60s timers. `AudioHandler.onAutoStop` fires first and calls `stateMachine.stopRecording()` + `handleProcessing()`. The state machine timer is a safety net.
- **State machine guards:** `startRecording()` only works from IDLE. `stopRecording()` only works from RECORDING. `setDone()` only works from PROCESSING. `setError()` only works from RECORDING or PROCESSING.
- **Concurrent recordings:** Not possible -- `startRecording()` guard prevents it. However, there's no explicit mutex.
- **Temp file cleanup:** Audio file deleted in `finally` block of `processAudio()` (line 248-249)
- **InputConnection null:** Text not inserted but still saved to history and clipboard

### Verification Checklist
- [x] Entry point exists and is reachable (VoiceManager.startRecording)
- [x] All transitions have corresponding code
- [x] Error states are handled (try/catch in processAudio, finally for cleanup)
- [x] Back navigation works (N/A - internal pipeline)
- [x] State is preserved across config changes (coroutine scope survives)

---

## Flow 18: Application Initialization

### Entry Point
Android system creates the `FlorisApplication` instance.

### Initialization Sequence
1. `FlorisApplication.onCreate()`:
   - Sets `FlorisApplicationReference` weak reference
   - Installs Flog (logging), CrashUtility, EmojiCompat
   - Checks if user is unlocked (`UserManagerCompat.isUserUnlocked()`)
   - If locked: Clears cache, inits extensions, registers `BootComplete` receiver, returns
   - If unlocked: Calls `init()`

2. `FlorisApplication.init()`:
   - Clears cache directory
   - Initializes `FlorisPreferenceStore` (JetPref datastore) asynchronously
   - Sets `preferenceStoreLoaded = true` when done
   - Initializes `ExtensionManager`
   - Initializes `ClipboardManager`
   - Initializes `DictionaryManager`
   - Starts observing `prefs.speekez.floatingWidgetEnabled`:
     - If enabled AND overlay permission granted: Starts `FloatingWidgetService`
     - Otherwise: Stops service

3. Lazy-initialized managers (created on first access):
   - `cacheManager`, `clipboardManager`, `editorInstance`, `extensionManager`
   - `glideTypingManager`, `keyboardManager`, `nlpManager`, `subtypeManager`, `themeManager`
   - `voiceManager` (with `hapticEnabledProvider` configured from prefs)

### Code References
- `/tmp/speekez-repo/app/src/main/kotlin/dev/patrickgold/florisboard/FlorisApplication.kt:66-203` -- Full application class
- `/tmp/speekez-repo/app/src/main/kotlin/dev/patrickgold/florisboard/FlorisApplication.kt:89-96` -- VoiceManager creation with haptic provider
- `/tmp/speekez-repo/app/src/main/kotlin/dev/patrickgold/florisboard/FlorisApplication.kt:141-155` -- Floating widget service management

### Edge Cases
- **Direct boot:** Init deferred until `ACTION_USER_UNLOCKED` broadcast
- **Native library load failure:** `System.loadLibrary("fl_native")` caught silently (line 70-72)
- **PreferenceStore init failure:** `Result` logged but no error handling (line 134)

### Verification Checklist
- [x] Entry point exists and is reachable (application onCreate)
- [x] All transitions have corresponding code
- [x] Error states are handled (crash utility, try/catch)
- [x] State is preserved across config changes (Application lifecycle)
- [x] Direct boot handled

---

## Flow 19: Permission Request Flow

### Entry Point
`PermissionActivity` launched via `PermissionActivity.createIntent(context)`.

### Screens/States
1. **Check existing permission:** If already granted, saves result and finishes immediately
2. **Check rationale:** If `shouldShowRequestPermissionRationale()` returns true:
   - Shows `AlertDialog` with title "Microphone Permission" and explanation
   - OK button: Launches permission request
   - Cancel button: Saves denied result and finishes
3. **System permission dialog:** Standard Android permission dialog
4. **Result callback:** `PermissionUtils.saveMicPermissionResult(context, isGranted)` saves to `SharedPreferences("speekez_permissions", "speekez_mic_permission_granted")`

### Code References
- `/tmp/speekez-repo/voice/src/main/kotlin/com/speekez/voice/PermissionActivity.kt:38-101` -- Full activity
- `/tmp/speekez-repo/voice/src/main/kotlin/com/speekez/voice/PermissionUtils.kt:27-65` -- Permission check and save utilities

### Edge Cases
- **Permission previously denied:** `shouldShowRequestPermissionRationale()` returns true, rationale dialog shown
- **Permanently denied:** System dialog may not show; `isGranted` will be false in callback
- **Dialog cancelled:** `setOnCancelListener` saves denied result and finishes

### Connections to Other Flows
- Required by: [Flow 5/6/7/8/9] -- VoiceManager checks permission before recording
- Note: VoiceManager does NOT launch PermissionActivity -- it just checks and reports error. The permission must be granted via onboarding or direct activity launch.

### Verification Checklist
- [x] Entry point exists and is reachable (createIntent factory)
- [x] All transitions have corresponding code
- [x] Error states are handled (denied, cancelled)
- [x] Back navigation works (dialog cancel handling)
- [ ] State is preserved across config changes (activity may recreate during dialog)

---

## Flow 20: Dashboard Statistics

### Entry Point
Bottom nav > "Dashboard" tab (default start destination) in the main SpeekEZ app.

### Screens/States
1. **Loading state:** Empty `Box` while `allStats == null`
2. **Empty state:** If no stats exist, shows `EmptyState` with chart icon: "Start dictating to see your stats"
3. **Stats view:** `LazyColumn` with:
   - **Hero section:** Large "Total Time Saved" number (calculated as `wordCount / 75.0` minutes)
   - **Stat cards row:** Avg WPM (from `transcriptionDao.getOverallAvgWpm()`) and Total Words
   - **Today's Stats card:** Recordings count, Words count, Time Saved for today
   - **Weekly Activity card:** `WeeklyTrendChart` component showing Mon-Sun word counts

### Data Sources
- `dailyStatsDao.getAllStats()` -- Flow of all DailyStats entries
- `dailyStatsDao.getTotalWordCount()` -- Flow of sum of all word counts
- `dailyStatsDao.getWeeklyStats(monday, sunday)` -- Flow of stats for current week
- `transcriptionDao.getOverallAvgWpm()` -- Flow of average WPM across all transcriptions

### Code References
- `/tmp/speekez-repo/app/src/main/kotlin/com/speekez/app/screens/DashboardScreen.kt:29-212` -- Full dashboard screen
- `/tmp/speekez-repo/app/src/main/kotlin/com/speekez/app/screens/DashboardScreen.kt:204-211` -- `calculateTimeSaved()`: `wordCount / 75.0` minutes -> hours+minutes format
- `/tmp/speekez-repo/app/src/main/kotlin/com/speekez/app/components/WeeklyTrendChart.kt` -- Chart component (not read in detail)
- `/tmp/speekez-repo/data/src/main/kotlin/com/speekez/data/dao/DailyStatsDao.kt:1-29` -- DailyStatsDao queries
- `/tmp/speekez-repo/voice/src/main/kotlin/com/speekez/voice/VoiceManager.kt:255-286` -- `updateDailyStats()`: Rolling average WPM calculation

### Edge Cases
- **No data:** Shows empty state with BarChart icon
- **Data loading:** Shows empty Box (no loading indicator -- potential UX gap)
- **Week calculation:** Uses `TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY)` to find week start
- **Time saved formula:** `wordCount * 60 / 75.0` seconds (assumes 75 WPM typing speed as baseline)
- **Rolling average WPM:** `(existingAvgWpm * existingCount + currentWpm) / newCount`

### Connections to Other Flows
- Fed by: [Flow 17] Voice pipeline saves DailyStats and Transcriptions
- Start destination for main screen navigation

### Verification Checklist
- [x] Entry point exists and is reachable (bottom nav, start destination)
- [x] All transitions have corresponding code
- [ ] Error states are handled (no loading indicator during data fetch)
- [x] Back navigation works (bottom nav)
- [x] State is preserved across config changes (Compose + Flow)

---

## Global Architecture Notes

### Module Structure
| Module | Purpose |
|--------|---------|
| `app` | Main application, activities, UI screens, FlorisBoard fork code |
| `voice` | Voice recording pipeline (VoiceManager, AudioHandler, StateMachine, Permissions) |
| `api` | API clients (OpenAI Whisper, OpenRouter Audio, Anthropic Claude, OpenRouter Claude) |
| `data` | Room database, entities, DAOs, seeder |
| `security` | Encrypted SharedPreferences for API keys |
| `core` | Shared enums (ApiMode, ModelTier), utilities (NetworkUtils, AccessibilityUtils) |
| `widget` | Home screen widgets (1x1, 2x1, floating service) |

### Two Activity Systems
The app has TWO main activities that serve different purposes:
1. **`SpeekEZActivity`** -- Main SpeekEZ app (Dashboard, History, Settings). Launched from app icon (MAIN/LAUNCHER)
2. **`FlorisAppActivity`** -- FlorisBoard settings/setup. Launched from keyboard settings or deep links

These are independent navigation stacks with different theme systems and routing.

### Preference Systems
The app uses THREE different preference/storage systems:
1. **JetPref DataStore** (`FlorisPreferenceStore` / `florisboard-app-prefs`): SpeekEZ toggles, active preset, Smartbar layout, all FlorisBoard prefs
2. **EncryptedSharedPreferences** (`speekez_secure_prefs`): API keys, API mode, model tier, custom models
3. **Regular SharedPreferences** (multiple files): Widget configurations, permission state, floating widget position

### Database
- Room database: `speekez_database` (version 2)
- Tables: `presets`, `transcriptions`, `daily_stats`
- Migration 1->2: Added `default_input_language`, renamed `output_language` to `output_languages`, added `default_output_language`

### Accessibility
- `AccessibilityUtils.announce()` called for:
  - "Recording started"
  - "Recording stopped, transcribing"
  - "Transcription complete, N words"
  - "Error: [message]"
- Semantic content descriptions on Smartbar mic button and preset chips

### Known Gaps / Issues Discovered
1. **No back button in onboarding steps** -- user cannot return to previous step
2. **API test is simulated** -- `performApiTest()` has `delay(1500)` and returns true without actually testing
3. **AudioHandler failure is silent** -- if MediaRecorder fails to start, no error propagated to user
4. **Empty name/icon saves silently fail** -- PresetEditForm validates but shows no error message
5. **Two 1x1 widget config activities** -- potential SharedPreferences desync between widget module and app module configs
6. **No loading indicator on Dashboard** -- empty Box shown during data fetch, looks like blank screen
7. **PermissionActivity not auto-launched** -- VoiceManager checks permission but doesn't launch the permission activity; user must grant it some other way
8. **PresetSeeder call site unclear** -- `PresetSeeder.seedDefaultPresetsIfEmpty()` is defined but the call site was not found in the read files (possibly in a database callback or initial activity)
9. **VoiceManager singleton per app** -- shared across keyboard, widgets, floating service. Concurrent recording from different entry points prevented by state machine guard but no explicit locking
10. **Floating widget FLAG_NOT_FOCUSABLE** -- may cause touch issues with preset list in expanded state
