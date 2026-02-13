# R3: Whisper + OpenRouter API Integration Research

## Context
This is R3 of the research phase for Speekez. R1 documented Florisboard architecture, R2 will cover audio recording patterns.

## Objective
Research and document integration patterns for Whisper (transcription) and Claude/LLM (refinement) APIs via OpenRouter.

## Deliverables

Create `docs/research/API-INTEGRATION.md` covering:

### 1. Whisper API Integration
- OpenAI Whisper API endpoint and authentication
- Groq Whisper (faster, free tier) as alternative
- Request format (multipart form data for audio files)
- Response format and parsing
- Rate limits and error handling
- Timeout considerations (don't block UI)

### 2. OpenRouter Integration
- Why OpenRouter (single API key for multiple models)
- Supported models for text refinement (Haiku 3.5, Sonnet 4, Sonnet 4.5)
- Request format for chat completions
- Cost tracking patterns
- Error handling and fallbacks

### 3. Network Layer Architecture
- OkHttp vs Retrofit (recommendation?)
- Coroutines for async API calls
- Progress feedback during API calls
- Cancellation support (if user dismisses keyboard)

### 4. API Key Management
- Secure storage (EncryptedSharedPreferences)
- First-run setup flow
- Validation patterns
- Multiple key support (Whisper + OpenRouter OR custom keys)

### 5. Model Selection System
- Preset tiers (cheap/optimal/best)
- Cost calculations per tier
- Custom model picker for advanced users
- Default recommendations

### 6. Offline Detection
- Network availability checking
- User feedback when offline
- Graceful degradation

### 7. Code Examples
- Whisper API call pattern
- OpenRouter API call pattern
- Error handling with user-friendly messages
- Progress UI integration

### 8. Florisboard Integration
- Where to add API client classes (manager pattern from R1)
- Lifecycle-aware API calls (IME can be killed)
- State management for API results
- UI feedback patterns

## Success Criteria
- Clear API integration patterns defined
- Cost-effective model selection documented
- Error handling patterns specified
- Ready for implementation phase

## References
- R1 architecture docs
- R2 audio recording patterns
- OpenRouter documentation
- OpenAI Whisper API docs
