# R5: Testing Setup Research (Maestro)

## Context
This is R5 (final research task) for Speekez. R1-R4 covered architecture, audio, API, and UX.

## Objective
Research and document testing strategy using Maestro for UI automation and end-to-end testing of the voice keyboard.

## Deliverables

Create `docs/research/TESTING-SETUP.md` covering:

### 1. Maestro Overview
- What is Maestro? (mobile UI testing framework)
- Why Maestro for keyboards? (better than Espresso for complex interactions)
- Installation and setup (can run on Fort Mac for Android emulator)
- Basic flow syntax

### 2. Android Keyboard Testing Challenges
- IME lifecycle complexity
- Keyboard visibility detection
- Text input verification
- Permission testing
- API mocking strategies

### 3. Test Scenarios for Speekez

#### Core Recording Tests
- Launch keyboard → tap voice button → record 5s → verify audio file created
- Start recording → cancel → verify no file created
- Record while offline → verify error message
- Record with no API key → verify setup prompt

#### API Integration Tests
- Mock Whisper API response → verify text appears
- Mock API error → verify error message
- Mock slow API → verify loading state
- Mock refinement API → verify refined output

#### UI State Tests
- Voice button visibility
- Recording animation
- Processing state display
- Result preview
- Error display

#### Settings Tests
- Navigate to settings
- Change model preset
- Enter API keys
- Validate keys
- Save and verify persistence

#### Permission Tests
- First launch → request microphone permission
- Deny permission → verify fallback behavior
- Grant permission → verify recording works

### 4. Test Infrastructure
- Maestro Cloud vs local execution
- Running on Fort (Mac Mini) with Android emulator
- CI/CD integration patterns
- Test data management (mock API responses)

### 5. Mocking Strategy
- How to mock Whisper API (local HTTP mock server?)
- How to mock OpenRouter API
- Test audio files (pre-recorded samples)
- Network condition simulation

### 6. Dual Testing Protocol
- Jules runs tests first (automated in CI)
- I verify on Fort (manual verification)
- Both must pass for sprint completion

### 7. Code Examples
- Basic Maestro flow for voice recording
- API mocking setup
- Permission testing flow
- Multi-step integration test

### 8. Success Criteria Definition
- What does "tests pass" mean?
- Required coverage percentage
- Performance benchmarks
- Crash-free rate target

## Success Criteria
- Complete testing strategy documented
- Maestro setup steps clear
- Test scenarios comprehensive
- Mocking strategy defined
- Dual testing protocol specified

## References
- R1 architecture (testing requirements)
- R2 audio recording (what to test)
- R3 API integration (mocking needs)
- R4 UX patterns (UI testing scenarios)
- Maestro documentation
- Fort node capabilities
