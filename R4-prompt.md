# R4: Voice Keyboard UX Analysis

## Context
This is R4 of the research phase for Speekez. R1-R3 covered architecture, audio recording, and API integration.

## Objective
Research and document UX patterns for voice-to-text keyboards, focusing on user experience, visual feedback, and interaction patterns.

## Deliverables

Create `docs/research/VOICE-UX.md` covering:

### 1. Existing Voice Keyboard Patterns
- Wispr Flow (iOS) — reference implementation, what makes it great?
- Gboard voice input — industry standard patterns
- SwiftKey voice — Microsoft's approach
- Other voice keyboards — what works, what doesn't

### 2. Voice Button Design
- Icon design (microphone vs custom icon)
- Button placement (where on keyboard layout?)
- Size and accessibility
- Visual states (idle, recording, processing, success, error)

### 3. Recording Feedback
- Waveform visualization patterns
- Level meter (VU meter style)
- Timer display (show recording duration)
- Visual breathing effects
- Haptic feedback patterns

### 4. Processing States
- "Transcribing..." feedback
- "Refining..." feedback
- Progress indicators (spinner vs custom)
- Estimated time display
- Cancel button availability

### 5. Result Display
- Where to show transcribed text before inserting?
- Edit-before-insert patterns
- Multi-line handling
- Error messages (user-friendly)

### 6. Model Selection UI
- Preset tier display (cheap/optimal/best)
- Cost visibility (per use or monthly estimate)
- Settings location (keyboard toolbar vs system settings)
- Quick switcher patterns

### 7. First-Run Experience
- Setup wizard flow
- API key input
- Model preset selection
- Permissions explanation
- "Try it" demo

### 8. Edge Cases
- Very long recordings (>60s)
- Very short recordings (<2s)
- Silence detection
- Background noise handling
- Multi-language support

### 9. Accessibility
- Screen reader support
- High contrast mode
- Large touch targets
- Color blindness considerations

### 10. Florisboard Integration
- How to fit voice button into existing layouts
- Theme compatibility (Snygg theming engine from R1)
- Compose UI patterns
- Animation system integration

## Success Criteria
- Comprehensive UX patterns documented
- Wispr Flow analysis complete
- Visual mockups or descriptions for key states
- Accessibility considerations addressed
- Implementation-ready design specs

## References
- R1 architecture and theming docs
- R2 audio recording patterns
- R3 API integration patterns
- Wispr Flow (iOS) as reference
- Florisboard existing UI patterns
