# R4: Voice Keyboard UX Analysis

## 1. Existing Voice Keyboard Patterns

### Wispr Flow (iOS) - Reference Implementation
Wispr Flow is the current gold standard for voice-first input. Key differentiators include:
- **Low Latency**: Near-instant transcription creates a "flow" state where the user feels they are talking directly into the document.
- **Natural Language Command Support**: Users can say "send this to John" or "make it sound more professional," and the system handles the intent.
- **Punctuation & Formatting**: Exceptional automatic handling of pauses, tone, and sentence structure.
- **Push-to-Talk**: Minimal friction; usually a single long-press or a very prominent toggle.

### Gboard Voice Input
- **Standard Industry Pattern**: The microphone icon in the top right of the toolbar.
- **Visual Feedback**: Pulsating dots or a microphone icon that "fills" with color based on volume levels.
- **Streaming Transcription**: Text appears in gray (interim) and turns black (final) as the model gains confidence.
- **Context Awareness**: Suggestions appear above the keyboard while recording.

### SwiftKey Voice
- **Dictation Bar**: Often opens a dedicated sub-panel that takes over the suggestion bar area.
- **Auto-stop**: Aggressive silence detection to stop recording when the user pauses.

---

## 2. Voice Button Design

### Icon Design
- **Microphone**: Standard and universally understood. For Speekez, we should use a clean, Material 3-compliant microphone icon.
- **Custom Branding**: A subtle "S" or "Speekez" logo could be used for the recording state to reinforce brand identity.

### Placement
- **Primary**: Right side of the Smartbar (toolbar). This is the most common location and easily reachable by thumb.
- **Alternative**: Long-press on the Spacebar. This provides a larger touch target but might conflict with language switching or cursor control.

### Visual States
| State | Description | Visual Representation |
|-------|-------------|-----------------------|
| **Idle** | Ready to record | Outline microphone icon, muted color. |
| **Active/Recording** | Capturing audio | Filled icon, accent color (e.g., Speekez Blue), pulsating ring. |
| **Processing** | API call in progress | Rotating spinner or "shimmer" effect over the icon. |
| **Success** | Text committed | Brief green glow or checkmark animation. |
| **Error** | API/Network failure | Red icon or warning triangle. |

---

## 3. Recording Feedback

### Waveform Visualization
- **Live Meter**: A small horizontal waveform in the Smartbar that reacts to the microphone's `maxAmplitude`.
- **Center-aligned**: Grows from the center outwards to maintain symmetry.

### Level Meter (VU Style)
- **Bar dots**: 3-5 vertical dots that light up based on volume levels.
- **Color Gradients**: Green -> Yellow -> Red for clipping (though rarely needed for speech).

### Timer Display
- **Elapsed Time**: Shown next to the waveform (e.g., `0:12`).
- **Maximum Limit**: Subtle countdown if approaching the 60s limit.

### Haptic Patterns
- **Start**: Single "Tick" (heavy impact) to confirm recording has begun.
- **Stop**: Double "Tick" (light impact) to confirm processing has started.
- **Error**: Long vibration.

---

## 4. Processing States

### Feedback Messages
- **"Transcribing..."**: While waiting for Whisper API response.
- **"Refining..."**: If the Claude refinement step is enabled.
- **"Correcting..."**: If auto-punctuation or grammar checks are running.

### Cancel Button
- **Availability**: A "Cancel" or "X" button must be visible to stop the process and discard the audio/request.

---

## 5. Result Display

### Insertion Pattern
- **Direct Insert**: Text is inserted at the cursor position immediately after processing.
- **Multi-line handling**: Ensure `\n` characters are correctly handled for longer dictations.

### Error Messages (User-friendly)
- **Toast-style**: Show brief, actionable messages like "Could not hear you. Please try again." or "Check your internet connection."
- **Inline hints**: Instead of just a red icon, show "API Key Missing" if the error is configuration-based.

### Edit-before-insert (Optional)
- **Review Panel**: For very long dictations, show the text in a temporary overlay with "Confirm" or "Discard" buttons.
- **Highlighting**: Highlight words with low confidence scores (if provided by the API).

---

## 6. Model Selection UI

### Tier Display
- **Efficiency (Tiny/Base)**: "Fast & Lite" - Low cost, high speed.
- **Optimal (Small/Medium)**: "Balanced" - Best for general use.
- **Quality (Large/Turbo)**: "Professional" - Highest accuracy, higher cost/latency.

### Cost Visibility
- **Usage Stats**: Display "Estimated cost: $0.01" or "Free" (if using local/free tier) in the settings.
- **Balance Warning**: If the user's API key is low on funds.

---

## 7. First-Run Experience (Onboarding)

### Setup Wizard
1. **Welcome**: Introduction to Speekez.
2. **Permissions**: Request `RECORD_AUDIO` with a clear explanation of *why*.
3. **API Setup**: Input Whisper/Claude API keys with "Get Key" links.
4. **Preset Selection**: Let the user choose their preferred speed/quality tier.
5. **Interactive Demo**: A "Try it now" text field within the setup screen.

---

## 8. Edge Cases

### Duration Limits
- **Short (<2s)**: Discard and show "Too short" hint.
- **Long (>60s)**: Auto-stop and process, or warn the user.

### Silence Detection
- **Auto-pause**: If 3-5 seconds of silence are detected, stop recording automatically.

### Background Noise
- **Warning**: "Too noisy to record" if the noise floor is too high.

### Multi-language Support
- **Auto-detection**: If enabled, use Whisper's language detection to identify the spoken language.
- **Manual Toggle**: Show a small flag or language code (e.g., "EN", "ES") next to the record button if multi-language is active.

---

## 9. Accessibility

- **Screen Readers**: Every state change (Recording, Processing, Done) must trigger an `AccessibilityEvent`.
- **High Contrast**: Ensure the microphone active state has at least 4.5:1 contrast ratio against the Smartbar background.
- **Color Blindness**: 
    - Don't rely solely on color (e.g., Green/Red). Use distinct icons (Checkmark vs Warning Triangle) for Success/Error states.
    - Use a pulsating animation or "active" ring to indicate recording state, which is visible regardless of color perception.
- **Touch Targets**: Minimum 48dp x 48dp for the voice button.

---

## 10. Florisboard Integration (Implementation Specs)

### Smartbar Integration
- Add `VOICE_RECORD` to `QuickAction.kt`.
- Use `SnyggBox` in `Smartbar.kt` to wrap the voice button for theming.

### Compose UI Patterns
- Use `AnimatedContent` for switching between the microphone icon and the processing spinner.
- Implement the waveform using a custom `Canvas` drawing based on a `Flow<Float>` of audio levels.

### Theming Compatibility
- **New Snygg Elements**:
    - `smartbar-voice-button`: The container for the voice action.
    - `smartbar-voice-button[state=recording]`: Style applied when recording is active.
    - `voice-waveform`: The canvas element for audio visualization.
- **Dynamic Colors**: Support Material 3 dynamic colors via `MaterialKolor` to ensure the recording state (usually a vibrant blue or red) matches the system theme.
- **Animation**: Use a 300ms `tween` for transition between idle and recording states to provide a smooth "breathing" effect.
