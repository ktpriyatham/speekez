# WhisperDroid API Architecture
**OpenRouter + Groq Integration**

---

## API Keys Setup

### **Recommended: 2-Key Architecture** 🏆

```
┌─────────────────────────────────────────┐
│ API Key 1: Groq                         │
│ Purpose: Audio transcription (Whisper)  │
│ Endpoint: /audio/transcriptions         │
│ Cost: $0.0006/min (10x cheaper!)       │
└─────────────────────────────────────────┘

┌─────────────────────────────────────────┐
│ API Key 2: OpenRouter                   │
│ Purpose: Text refinement (Claude, etc.) │
│ Endpoint: /chat/completions             │
│ Cost: Varies by model                   │
└─────────────────────────────────────────┘
```

**Why Groq?**
- ⚡ **10x faster** than OpenAI Whisper
- 💰 **10x cheaper** ($0.0006/min vs $0.006/min)
- 🔄 **OpenAI-compatible** (drop-in replacement)
- 🎯 **Dedicated transcription endpoint** (more efficient)

---

## Whisper Prompt Support

### Available Parameters

```kotlin
data class WhisperRequest(
    val file: File,               // Audio file
    val model: String,            // "whisper-large-v3-turbo"
    val prompt: String? = null,   // ✅ CUSTOM PROMPT (up to 224 tokens)
    val language: String? = null, // "en", "es", etc.
    val temperature: Float? = null // 0.0-1.0
)
```

### Prompt Use Cases

1. **Vocabulary Hints**
   ```
   "OpenAI, ChatGPT, Claude, Anthropic, LLM, RAG, embeddings"
   ```
   - Helps with technical terms
   - Names, products, acronyms

2. **Style Guidance**
   ```
   "Use formal business tone. Always capitalize proper nouns."
   ```
   - Controls output style
   - Punctuation preferences

3. **Context Continuation**
   ```
   "In this meeting we discussed the Q4 roadmap..."
   ```
   - Helps with multi-segment audio
   - Maintains context

4. **Speaker Tracking**
   ```
   " - Good morning. - Hi there, how are you?"
   ```
   - Basic speaker turn detection
   - Format: " - Speaker A. - Speaker B."

---

## Model Selection

### Groq Whisper Models

```kotlin
// Fetch from Groq API
GET https://api.groq.com/openai/v1/models

Response:
{
  "data": [
    {
      "id": "whisper-large-v3",
      "object": "model",
      "created": 1699894400,
      "owned_by": "groq"
    },
    {
      "id": "whisper-large-v3-turbo",  // ← RECOMMENDED (faster)
      "object": "model",
      "created": 1735257600,
      "owned_by": "groq"
    },
    {
      "id": "distil-whisper-large-v3-en",  // English only, faster
      "object": "model",
      "created": 1699894400,
      "owned_by": "groq"
    }
  ]
}
```

### OpenRouter Text Models

```kotlin
// Fetch from OpenRouter API
GET https://openrouter.ai/api/v1/models

Response:
{
  "data": [
    {
      "id": "anthropic/claude-3.5-sonnet",
      "name": "Claude 3.5 Sonnet",
      "pricing": {
        "prompt": "0.000003",  // per token
        "completion": "0.000015"
      }
    },
    {
      "id": "anthropic/claude-3-opus",
      "name": "Claude 3 Opus",
      "pricing": {
        "prompt": "0.000015",
        "completion": "0.000075"
      }
    },
    {
      "id": "openai/gpt-4-turbo",
      "name": "GPT-4 Turbo",
      "pricing": {
        "prompt": "0.00001",
        "completion": "0.00003"
      }
    }
  ]
}
```

---

## Preset Prompts System

### Database Schema

```sql
CREATE TABLE prompt_presets (
    id INTEGER PRIMARY KEY,
    name TEXT NOT NULL,
    api_type TEXT NOT NULL,  -- 'whisper' or 'claude'
    prompt_text TEXT NOT NULL,
    is_default BOOLEAN DEFAULT FALSE,
    created_at INTEGER NOT NULL
);

-- Example presets
INSERT INTO prompt_presets VALUES
(1, 'Technical Vocabulary', 'whisper', 'API, SDK, OAuth, JWT, REST, GraphQL, microservices', FALSE, 1707782400),
(2, 'Formal Business', 'claude', 'Refine this text to formal business English. Fix grammar, improve clarity, maintain professional tone.', TRUE, 1707782400),
(3, 'Casual Chat', 'claude', 'Make this more conversational and friendly. Keep it natural and casual.', FALSE, 1707782400),
(4, 'Code Documentation', 'claude', 'Format as technical documentation. Use clear, concise language suitable for developer docs.', FALSE, 1707782400);
```

### UI Design - Preset Manager

```
┌─────────────────────────────────────────┐
│ 📝 Prompt Presets             [+ New]  │
├─────────────────────────────────────────┤
│ Whisper Prompts (5)                     │
│                                         │
│ ⭐ Technical Vocabulary         [Edit] │
│ "API, SDK, OAuth, JWT..."               │
│ ─────────────────────────────────────  │
│ Meeting Notes                   [Edit] │
│ " - Facilitator. - Team member..."      │
│ ─────────────────────────────────────  │
│                                         │
│ Claude Prompts (8)                      │
│                                         │
│ ⭐ Formal Business (default)    [Edit] │
│ "Refine to formal business English..." │
│ ─────────────────────────────────────  │
│ Casual Chat                     [Edit] │
│ "Make more conversational..."           │
│                                         │
│ [Import] [Export] [Reset to Defaults]  │
└─────────────────────────────────────────┘
```

---

## Implementation

### Groq Whisper Client

```kotlin
class GroqWhisperClient(private val apiKey: String) {
    private val baseUrl = "https://api.groq.com/openai/v1"
    
    suspend fun transcribe(
        audioFile: File,
        model: String = "whisper-large-v3-turbo",
        prompt: String? = null,
        language: String? = null
    ): TranscriptionResponse {
        val request = MultipartBody.Builder()
            .setType(MultipartBody.FORM)
            .addFormDataPart("file", audioFile.name,
                audioFile.asRequestBody("audio/*".toMediaType()))
            .addFormDataPart("model", model)
            .apply {
                prompt?.let { addFormDataPart("prompt", it) }
                language?.let { addFormDataPart("language", it) }
            }
            .build()
            
        val response = httpClient.post("$baseUrl/audio/transcriptions") {
            header("Authorization", "Bearer $apiKey")
            setBody(request)
        }
        
        return response.body()
    }
    
    suspend fun getModels(): List<WhisperModel> {
        val response = httpClient.get("$baseUrl/models") {
            header("Authorization", "Bearer $apiKey")
        }
        return response.body<ModelsResponse>().data
            .filter { it.id.startsWith("whisper") }
    }
}
```

### OpenRouter Text Client

```kotlin
class OpenRouterClient(private val apiKey: String) {
    private val baseUrl = "https://openrouter.ai/api/v1"
    
    suspend fun refineText(
        text: String,
        model: String = "anthropic/claude-3.5-sonnet",
        systemPrompt: String? = null
    ): String {
        val messages = buildList {
            systemPrompt?.let { 
                add(Message("system", it)) 
            }
            add(Message("user", text))
        }
        
        val request = ChatRequest(
            model = model,
            messages = messages
        )
        
        val response = httpClient.post("$baseUrl/chat/completions") {
            header("Authorization", "Bearer $apiKey")
            header("HTTP-Referer", "https://github.com/whisperdroid")
            contentType(ContentType.Application.Json)
            setBody(request)
        }
        
        return response.body<ChatResponse>().choices[0].message.content
    }
    
    suspend fun getModels(): List<TextModel> {
        val response = httpClient.get("$baseUrl/models") {
            header("Authorization", "Bearer $apiKey")
        }
        return response.body<ModelsResponse>().data
    }
}
```

### Preset Prompt Manager

```kotlin
class PromptPresetManager(private val db: Database) {
    
    fun getPresets(apiType: String): List<PromptPreset> {
        return db.query(
            "SELECT * FROM prompt_presets WHERE api_type = ? ORDER BY is_default DESC, name ASC",
            apiType
        )
    }
    
    fun getDefaultPreset(apiType: String): PromptPreset? {
        return db.querySingle(
            "SELECT * FROM prompt_presets WHERE api_type = ? AND is_default = TRUE",
            apiType
        )
    }
    
    fun savePreset(preset: PromptPreset) {
        if (preset.isDefault) {
            // Clear other defaults
            db.execute(
                "UPDATE prompt_presets SET is_default = FALSE WHERE api_type = ?",
                preset.apiType
            )
        }
        
        db.insert("prompt_presets", preset)
    }
    
    fun deletePreset(id: Int) {
        db.execute("DELETE FROM prompt_presets WHERE id = ?", id)
    }
}
```

---

## Settings UI Design

```
┌─────────────────────────────────────────┐
│ ⚙️ API Configuration             [✕]  │
├─────────────────────────────────────────┤
│ Audio Transcription                     │
│                                         │
│ Provider:  [Groq ▼]                    │
│ API Key:   sk-...xYz [Edit] [Test]    │
│ Model:     [whisper-large-v3-turbo ▼] │
│ ✅ Validated - Last tested 2m ago      │
│                                         │
│ Whisper Preset Prompt                   │
│ [Technical Vocabulary ▼] [Manage]      │
│ "API, SDK, OAuth, JWT, REST..."         │
│ [ ] Use custom prompt per recording     │
│                                         │
│ ─────────────────────────────────────  │
│                                         │
│ Text Refinement                         │
│                                         │
│ Provider:  [OpenRouter ▼]              │
│ API Key:   sk-...pXw [Edit] [Test]    │
│ Model:     [claude-3.5-sonnet ▼]      │
│ ✅ Validated - Last tested 1h ago      │
│                                         │
│ Refinement Preset Prompt                │
│ [Formal Business ▼] [Manage]           │
│ "Refine to formal business English..." │
│ [✓] Auto-refine all transcriptions     │
│                                         │
│ ─────────────────────────────────────  │
│                                         │
│ Alternative Providers                   │
│ Audio:  [ ] OpenAI  [ ] Deepgram       │
│ Text:   [ ] Direct Anthropic           │
│                                         │
│ [Save] [Reset to Defaults]              │
└─────────────────────────────────────────┘
```

---

## Cost Comparison

| Provider | Transcription | Per Minute | Per Hour |
|----------|---------------|------------|----------|
| **Groq** | Whisper v3 Turbo | **$0.0006** | **$0.036** |
| OpenAI | Whisper | $0.006 | $0.36 |
| Deepgram | Nova-2 | $0.0043 | $0.26 |

| Provider | Text Model | Per 1K Tokens | 100 Refinements |
|----------|------------|---------------|-----------------|
| **OpenRouter** | Claude Sonnet | **$0.003** | **$0.60** |
| OpenRouter | Claude Opus | $0.015 | $3.00 |
| OpenRouter | GPT-4 Turbo | $0.01 | $2.00 |

**Total per 100 recordings (30s each):**
- Groq Whisper: $0.03
- Claude Sonnet refinement: $0.60
- **Combined: $0.63**

---

## Next Steps

1. **Review API choice** - Groq + OpenRouter vs all OpenRouter?
2. **Design approval** - Preset prompts system?
3. **Fork keyboard** - Thumb-Key or other?
4. **Submit to Jules** - Implement API integration?

Your call on:
- Use 2 API keys (Groq + OpenRouter) for best performance?
- Or 1 API key (OpenRouter only) for simplicity?
