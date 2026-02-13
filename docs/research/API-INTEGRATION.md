# API Integration Research: Whisper & OpenRouter

This document outlines the integration patterns for Whisper (transcription) and Claude/LLM (refinement) APIs via OpenRouter for the Speekez project.

## 1. Whisper API Integration

Whisper is used to convert recorded audio into raw text. We support both OpenAI's official API and Groq's high-performance Whisper implementation.

### Endpoints and Authentication
- **OpenAI Whisper**: `https://api.openai.com/v1/audio/transcriptions`
- **Groq Whisper**: `https://api.groq.com/openai/v1/audio/transcriptions`
- **Authentication**: Bearer Token in the `Authorization` header.

### Request Format (Multipart Form Data)
Both APIs use the standard OpenAI format:
- `file`: The audio file (recommended: `.m4a`, `.wav`, or `.flac`).
- `model`: `whisper-1` (OpenAI) or `whisper-large-v3-turbo` (Groq).
- `language`: (Optional) ISO-639-1 code (e.g., `en`, `fr`).
- `response_format`: `json` (default) or `verbose_json` (for timestamps and probabilities).

### Response Format
```json
{
  "text": "The transcribed text is here.",
  "task": "transcribe",
  "language": "english",
  "duration": 4.5,
  "segments": [...] 
}
```

### Considerations
- **Rate Limits**: Groq has a generous free tier but strict rate limits. OpenAI is pay-as-you-go.
- **Error Handling**: Implement exponential backoff for `429 Too Many Requests`. Handle `413 Payload Too Large` by ensuring recordings stay under 25MB.
- **Timeouts**: Audio uploads can take time. Set a generous timeout (e.g., 30-60s) and ensure it doesn't block the UI thread.

## 2. OpenRouter Integration

OpenRouter is used as a gateway to access various LLMs (primarily Claude) for text refinement (correcting grammar, formatting, etc.) using a single API key.

### Supported Models
- **Haiku 3.5** (`anthropic/claude-3.5-haiku`): Best for fast, cheap refinement.
- **Sonnet 3.5** (`anthropic/claude-3.5-sonnet`): High quality, slightly slower and more expensive.
- **Sonnet 4.5** (Future/Preview): For the most complex reasoning tasks.

### Request Format
```json
{
  "model": "anthropic/claude-3.5-haiku",
  "messages": [
    {
      "role": "system",
      "content": "You are a helpful assistant that refines transcribed text. Correct grammar and punctuation while keeping the meaning intact."
    },
    {
      "role": "user",
      "content": "the transcription is here"
    }
  ],
  "headers": {
    "HTTP-Referer": "https://github.com/patrickgold/florisboard",
    "X-Title": "Speekez"
  }
}
```

### Cost Tracking
- OpenRouter provides `usage` data in the response.
- We should log estimated costs per request to show the user their spending.

## 3. Network Layer Architecture

### OkHttp + Retrofit
- **Retrofit**: Recommended for its clean interface definition and Coroutine support.
- **OkHttp**: Used as the underlying client. Essential for handling multipart requests and adding interceptors for logging/auth.

### Async Execution
- Use **Kotlin Coroutines** with `Dispatchers.IO`.
- Use `Flow` or `StateFlow` to emit progress states.
- **Progress States**:
  1. `Idle`: Waiting for user input.
  2. `Recording`: Capturing local audio.
  3. `Uploading`: Sending audio to Whisper (show progress % if possible via custom `RequestBody`).
  4. `Transcribing`: Waiting for Whisper response.
  5. `Refining`: Waiting for OpenRouter LLM response.
  6. `Success`: Text committed to editor.
  7. `Error`: Showing failure message.

### Cancellation
- API calls must be tied to a `CoroutineScope` (e.g., `lifecycleScope` in IME).
- If the user dismisses the keyboard or cancels the operation, the job must be cancelled immediately to save battery and data.

## 4. API Key Management

### Secure Storage
- Use **EncryptedSharedPreferences** for storing API keys.
- Do not store keys in plain text `SharedPreferences` or hardcoded in the app.

### Validation
- Perform a "ping" request (e.g., `GET /models`) to validate the key before saving.
- Show clear error messages if the key is invalid or has insufficient balance.

## 5. Model Selection System

We will implement "Tiers" to simplify the experience for users:
1. **Speed (Haiku 3.5)**: Lowest cost, fastest response.
2. **Balanced (Sonnet 3.5)**: Medium cost, high accuracy.
3. **Accuracy (Sonnet 3.5/4.5)**: Best possible results.

### Custom Picker
Allow advanced users to enter any OpenRouter model ID manually.

## 6. Offline Detection

- Use `ConnectivityManager` to check for an active internet connection.
- **Graceful Degradation**: 
  - If offline, disable the voice button or show a "No Internet" toast.
  - Inform the user that cloud-based transcription requires a connection.

## 7. Code Examples

### Whisper API Call (Retrofit)
```kotlin
interface WhisperService {
    @Multipart
    @POST("v1/audio/transcriptions")
    suspend fun transcribe(
        @Part file: MultipartBody.Part,
        @Part("model") model: RequestBody
    ): TranscriptionResponse
}
```

### OpenRouter API Call (Retrofit)
```kotlin
interface OpenRouterService {
    @POST("api/v1/chat/completions")
    suspend fun complete(
        @Header("Authorization") auth: String,
        @Body request: ChatRequest
    ): ChatResponse
}
```

## 8. Florisboard Integration

### VoiceManager
Create a `VoiceManager` singleton (lazy-loaded in `FlorisApplication`) following the existing manager pattern:
- Responsibility: Orchestrate recording -> Whisper -> OpenRouter.
- Access: `context.voiceManager()`.

### Lifecycle-Aware Calls
- Use `lifecycleScope` from `FlorisImeService`.
- Monitor `onDeactivate` or `onDestroy` to clean up active network requests.

### UI Feedback
- Update `Smartbar` or a dedicated overlay with the current status (e.g., "Transcribing...").
- Use the `ObservableKeyboardState` to trigger UI updates in Compose.
