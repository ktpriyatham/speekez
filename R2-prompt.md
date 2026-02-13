# R2: Audio Recording Patterns Research

## Context
This is R2 of the research phase for Speekez, a voice-to-text keyboard for Android (Florisboard fork). R1 produced comprehensive architecture documentation showing how Florisboard works.

## Objective
Research and document best practices for audio recording in Android IME (Input Method Editor) context.

## Deliverables

Create `docs/research/AUDIO-RECORDING.md` covering:

### 1. Android Audio Recording APIs
- MediaRecorder vs AudioRecord (which is better for keyboard use case?)
- Required permissions and runtime permission handling
- Audio format considerations (PCM, WAV, Opus, etc.)
- Sample rate and encoding recommendations for speech

### 2. IME-Specific Constraints
- Lifecycle considerations (IME can be killed anytime)
- Background recording limitations
- Storage location (app-private vs external)
- Memory constraints

### 3. Voice Button Integration
- Where to add voice button in Florisboard UI (keyboard layout)
- Visual feedback during recording (animations, icons)
- Start/stop recording UX patterns
- Cancel vs submit patterns

### 4. Audio Quality vs File Size
- What quality is needed for Whisper API?
- Compression options (lossy vs lossless)
- File size estimates for 10s, 30s, 60s recordings
- Network transmission considerations

### 5. Code Examples
- Minimal recording setup code snippet
- Permission handling pattern
- Audio file management (creation, cleanup)
- Error handling patterns

### 6. Florisboard Integration Points
- Where to hook into existing keyboard UI
- Manager pattern integration (follow R1 architecture)
- State management for recording status
- Compose integration patterns

## Success Criteria
- Clear recommendations for audio API choice
- Specific integration points identified in Florisboard
- Code patterns ready for implementation
- File size/quality trade-offs documented

## References
- R1 architecture documentation in `docs/research/ARCHITECTURE.md`
- R1 extension points in `docs/research/EXTENSION-POINTS.md`
- Florisboard source code (already in repo fork)
