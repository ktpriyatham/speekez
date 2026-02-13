# R5: Testing Setup Research (Maestro)

This document outlines the testing strategy for Speekez, focusing on UI automation and end-to-end (E2E) testing using Maestro.

## 1. Maestro Overview

### What is Maestro?
[Maestro](https://maestro.mobile.dev/) is a modern mobile UI testing framework designed for simplicity and reliability. Unlike older frameworks that require complex setup and flaky code-based tests, Maestro uses a simple YAML-based syntax to define user flows. It is built on top of accessibility services, allowing it to interact with any element on the screen, regardless of the app's internal structure.

### Why Maestro for Keyboards?
Testing Input Method Editors (IMEs) like Speekez presents unique challenges:
- **Process Boundaries**: Keyboards run in their own process, separate from the application they are typing into. Standard testing frameworks like Espresso are often limited to a single app process.
- **System Interactions**: Keyboards interact heavily with the system (e.g., showing/hiding, handling hardware events).
- **Global Context**: Maestro can see the entire screen, including the keyboard and the target app simultaneously, making it ideal for verifying that typing on the keyboard actually results in text in the target field.

### Installation and Setup
Maestro can be installed on macOS, Linux, and Windows (via WSL). For our "Fort" (Mac Mini) setup:

```bash
# Install Maestro
curl -Ls "https://get.maestro.mobile.dev" | bash

# Verify installation
maestro --version
```

### Basic Flow Syntax
Tests are defined in `.yaml` files:

```yaml
appId: dev.patrickgold.florisboard
---
- launchApp
- tapOn: "Voice"
- assertVisible: "Recording..."
```

## 2. Android Keyboard Testing Challenges

- **IME Lifecycle Complexity**: The system controls when the keyboard is created, shown, and hidden. Tests must account for animation times and system-level triggers.
- **Keyboard Visibility Detection**: Standard `View` assertions often fail to find the keyboard because it's in a different window. Maestro's accessibility-based approach bypasses this.
- **Text Input Verification**: Ensuring that the `commitText` calls from the keyboard correctly update the `EditText` in the target app requires a framework that can "see" both apps.
- **Permission Testing**: Speekez requires microphone permissions. Handling system permission dialogs is notoriously difficult in Espresso but trivial in Maestro (`tapOn: "Allow"`).
- **API Mocking**: Speekez relies on external LLM and Whisper APIs. We must mock these to ensure deterministic tests and avoid high costs during CI.

## 3. Test Scenarios for Speekez

### Core Recording Tests
- **Basic Record**: Launch keyboard -> tap voice button -> record 5s -> verify audio file created in internal storage.
- **Cancel Recording**: Start recording -> tap cancel -> verify no file is left behind.
- **Offline Error**: Disable networking -> attempt record -> verify "Offline" error message appears.
- **No API Key**: Clear settings -> attempt record -> verify redirect to setup prompt.

### API Integration Tests
- **Successful Transcription**: Mock Whisper API to return "Hello world" -> verify "Hello world" is committed to the text field.
- **API Error Handling**: Mock 500 error from OpenRouter -> verify error message is shown to user.
- **Loading States**: Mock a 3-second delay -> verify "Processing..." spinner is visible.
- **Refinement Flow**: Mock Whisper with raw text -> Mock Claude with refined text -> verify final output.

### UI State Tests
- **Button Visibility**: Ensure the voice button appears in the Smartbar when enabled.
- **Animations**: Verify recording waves or progress bars are visible during active sessions.
- **Result Preview**: Verify that transcribed text appears in the preview area before commitment.

### Settings Tests
- **Model Selection**: Navigate to Speekez settings -> change model preset -> return to keyboard -> verify change persisted.
- **API Key Validation**: Enter invalid key -> verify validation failure UI.

### Permission Tests
- **First Run**: Trigger voice -> handle "While using the app" permission request -> verify success.
- **Permission Denied**: Deny permission -> verify "Microphone permission required" fallback UI.

## 4. Test Infrastructure

### Maestro Cloud vs. Local Execution
- **Local (Fort Mac)**: We will run an Android Emulator on the Fort Mac Mini. Tests will be executed locally for rapid iteration.
- **Maestro Cloud**: Can be used for testing on a wide variety of real devices if needed, but primary development will use local emulators.

### CI/CD Integration
- GitHub Actions will trigger tests on every Pull Request.
- The workflow will build the APK, start an emulator, install the APK, and run `maestro test`.

### Test Data Management
- Pre-recorded `.wav` files will be pushed to the emulator using `adb push` to simulate various voice inputs (clear speech, noisy environment, different languages).

## 5. Mocking Strategy

### API Mocking
We will use a local HTTP mock server (e.g., **MockWebServer** or **WireMock**) running on the same machine as the emulator.
- The app will be configured to point to `10.0.2.2` (the host machine's IP from the Android emulator) during test builds.
- Maestro can execute shell commands to swap mock configurations.

#### API Mocking Example
To mock API responses, we can use a combination of:
1. **Product Flavors**: Create a `mock` flavor of the app that uses a different base URL for API calls.
2. **Local Mock Server**: Use WireMock or a simple Node.js/Python server running on the host machine.
3. **Maestro Scripts**: Use `- exec: ...` to send commands to the mock server to change its state/responses between test steps.

Example of changing a mock response via Maestro:
```yaml
- exec: "curl -X POST http://localhost:8080/__admin/mappings -d '{\"request\": {\"method\": \"POST\", \"url\": \"/v1/audio/transcriptions\"}, \"response\": {\"status\": 200, \"body\": \"{\\\"text\\\": \\\"Hello world\\\"}\"}}'"
- tapOn: {id: "voice_record_button"}
```

### Audio Simulation
Since an emulator cannot "speak" into its own microphone easily:
- We will use `adb shell` commands to inject audio files into the system's microphone input if possible, or
- Implement a "Debug Audio Source" in Speekez that reads from a file instead of the microphone when a specific flag is set.

## 6. Dual Testing Protocol

1. **Automated (Jules)**: I will write and maintain the Maestro flows. These will run in CI and must pass before any PR is merged.
2. **Manual (Human)**: You (the developer) will run the same Maestro flows on the "Fort" Mac Mini to verify visual fidelity and feel.
3. **Requirement**: Both automated results and manual verification must be green for sprint completion.

## 7. Code Examples

### Basic Voice Recording Flow
```yaml
appId: com.android.messaging # Launch an app with a text field
---
- launchApp
- tapOn: "Start chat" # Or any text field to bring up the keyboard
- tapOn: {id: "dev.patrickgold.florisboard:id/voice_record_button"}
- delay: 5000 # Record for 5 seconds
- tapOn: {id: "dev.patrickgold.florisboard:id/voice_record_button"} # Stop recording
- assertVisible: "Transcribing..."
- assertVisible: "Hello world" # Mocked response in the input field
```

### Permission Testing
```yaml
- tapOn: {id: "dev.patrickgold.florisboard:id/voice_record_button"}
- tapOn: "While using the app" # Android system permission dialog
- assertVisible: {id: "dev.patrickgold.florisboard:id/recording_indicator"}
```

## 8. Success Criteria Definition

- **Tests Pass**: 100% of "Core" and "Permission" flows must pass.
- **Coverage**: All UI components in the Speekez module must be touched by at least one Maestro flow.
- **Performance Benchmarks**:
    - Keyboard appearance: < 300ms.
    - Transcription to commit (mocked): < 1.5s.
- **Crash-Free Rate**: 99.9% in automated test runs.

## References
- [Maestro Documentation](https://maestro.mobile.dev/)
- [Florisboard Architecture (R1)](ARCHITECTURE.md)
- [Speekez Audio Recording (R2)](AUDIO-RECORDING.md)
