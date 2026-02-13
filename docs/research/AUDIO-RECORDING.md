# Audio Recording Patterns for Speekez

This document outlines the research and best practices for implementing audio recording in the Speekez voice-to-text keyboard, focusing on the Whisper API and integration with Florisboard.

## 1. Android Audio Recording APIs

### MediaRecorder vs. AudioRecord
For the Speekez use case, we have two primary options for capturing audio:

| Feature | MediaRecorder | AudioRecord |
| --- | --- | --- |
| **Complexity** | Low (High-level API) | High (Low-level API) |
| **Output** | Compressed file (AAC, Ogg, etc.) | Raw PCM data buffer |
| **Control** | Limited | Full control over audio pipeline |
| **Use Case** | Record to file and upload | Real-time processing, low-latency |

**Recommendation:**
**MediaRecorder** is recommended for the initial implementation. It simplifies the process of recording directly to a compressed file format (like Ogg/Opus), which is ideal for uploading to the Whisper API. It also handles audio encoding and multiplexing automatically.

### Required Permissions
- `android.permission.RECORD_AUDIO`: Mandatory for capturing audio.
- `android.permission.INTERNET`: Mandatory for sending audio to the Whisper API.
- `android.permission.FOREGROUND_SERVICE_MICROPHONE`: Required if recording continues while the keyboard is partially obscured (Android 14+).

## 2. IME-Specific Constraints

### Lifecycle Considerations
IMEs can be killed or hidden at any time.
- **Stop on Hide**: Recording should automatically stop and cleanup if `onWindowHidden()` or `onDeactivate()` is called.
- **Service-bound**: Recording logic should be managed by a `VoiceManager` tied to the `FlorisImeService` lifecycle.

### Permission Handling in IMEs
IMEs cannot request permissions directly using `ActivityCompat.requestPermissions()`. 
- **Pattern**: Launch a transparent "PermissionActivity" or use the existing Florisboard Settings/Setup activity to request the `RECORD_AUDIO` permission.
- **UX**: If permission is missing, show a "Permission Required" toast or a button in the Smartbar that redirects the user to the setup screen.

### Background Recording
Android restricts background microphone access. However, as long as the keyboard (IME) is visible and has input focus, it is considered in the foreground.

## 3. Voice Button Integration

### Smartbar Integration
The best location for the voice button is the **Smartbar** (Florisboard's toolbar).
- **Icon**: A standard microphone icon (`R.drawable.ic_mic`).
- **Action**: A `QuickAction` that toggles the recording state.

### Visual Feedback Patterns
1. **Idle State**: Microphone icon in the Smartbar.
2. **Recording State**: 
   - Change icon color to red.
   - Show a "Recording..." label or a pulsating animation.
   - (Optional) Show a real-time volume meter in the Smartbar.
3. **Processing State**:
   - Show a spinning progress indicator while the audio is being transcribed.

## 4. Audio Quality vs. File Size (Whisper API)

### Quality Settings
Whisper performs best with 16kHz mono audio.
- **Sample Rate**: 16,000 Hz
- **Channels**: 1 (Mono)
- **Encoding**: Ogg/Opus (preferred) or AAC.

### File Size Estimates
Using Ogg/Opus at ~24kbps:
- **10 seconds**: ~30 KB
- **30 seconds**: ~90 KB
- **60 seconds**: ~180 KB

**Transmission Consideration:** Small file sizes are crucial for minimizing latency between finishing a recording and receiving the transcription.

## 5. Code Examples

### Minimal MediaRecorder Setup
```kotlin
val recorder = MediaRecorder().apply {
    setAudioSource(MediaRecorder.AudioSource.MIC)
    setOutputFormat(MediaRecorder.OutputFormat.OGG)
    setAudioEncoder(MediaRecorder.AudioEncoder.OPUS)
    setAudioSamplingRate(16000)
    setAudioChannels(1)
    setOutputFile(tempFile.absolutePath)
    prepare()
}

// Start recording
recorder.start()

// Stop recording
recorder.stop()
recorder.release()
```

### Permission Helper Pattern
```kotlin
fun requestMicPermission(context: Context) {
    val intent = Intent(context, PermissionActivity::class.java).apply {
        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    }
    context.startActivity(intent)
}
```

## 6. Florisboard Integration Points

### Manager Pattern
Create a `VoiceManager` class and register it in `FlorisApplication`.
```kotlin
class VoiceManager(private val context: Context) {
    private var recorder: MediaRecorder? = null
    
    fun startRecording() { ... }
    fun stopRecording(callback: (String) -> Unit) { ... }
}
```

### UI Hook (Smartbar)
In `Smartbar.kt` or the corresponding Compose component:
```kotlin
if (state.isRecording) {
    RecordingIndicator()
} else {
    QuickActionButton(icon = Icons.Default.Mic, onClick = { voiceManager.startRecording() })
}
```

### Text Commitment
Use `EditorInstance` to insert the final text:
```kotlin
voiceManager.stopRecording { transcription ->
    editorInstance.commitText(transcription)
}
```

### State Management
Add `isRecording: Boolean` to `ObservableKeyboardState.kt` to allow UI components to react to recording changes.
