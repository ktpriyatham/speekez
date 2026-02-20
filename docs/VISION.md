# SpeekEZ Vision & Design v2
**Voice-to-Text Keyboard with Clipboard History & Analytics**

> Originally developed under the name "WhisperDroid", renamed to **SpeekEZ** (capital S, E, Z).

---

## New Features (v2)

### 1. **Transcription History** (like Wispr Flow Notes)
- Every dictation saved automatically
- Search through past transcriptions
- Re-use or edit previous dictations
- Sync-ready (local first, cloud optional)

### 2. **Analytics Dashboard**
- **Words transcribed** (total + daily/weekly/monthly)
- **Words per minute (WPM)** - live + average
- **Recording time** - total minutes dictated
- **Most used words/phrases** - top 20
- **API costs** - per recording + totals
- **Usage trends** - charts + graphs

### 3. **Token Counter** (enhanced)
- Live token usage (Whisper + Claude)
- Cost estimates in real-time
- Daily/monthly budgets
- Alert when approaching limits

---

## Screen Designs

### Main Keyboard View
```
┌──────────────────────────────────────────┐
│ ⚙️  History(23)   🎤 Recording  [1.2k] │ ← Top bar
├──────────────────────────────────────────┤
│                                          │
│           [E]    [T]    [A]              │
│           [O]    [I]    [N]              │  3x3 Grid
│           [S]    [H]    [R]              │  (Thumb-Key)
│                                          │
│       [Space]   [⌫]    [↵]               │
└──────────────────────────────────────────┘
```

### Recording with WPM Counter
```
┌──────────────────────────────────────────┐
│ 🔴 Recording...  152 WPM     [Stop]     │
├──────────────────────────────────────────┤
│                                          │
│      ████████░░░░░░░░░                  │ ← Waveform
│      ░░░░░░░░████████                   │
│                                          │
│   "testing voice to text feature..."    │ ← Live transcript
│                                          │
│   Duration: 00:14  |  21 words          │
└──────────────────────────────────────────┘
```

### Transcription History
```
┌──────────────────────────────────────────┐
│ 📝 Transcription History      [Search]   │
├──────────────────────────────────────────┤
│ Today (8)                                │
│                                          │
│ 🕐 10:23 AM  |  42 words  |  $0.01     │
│ "Can you send me the meeting notes..."   │
│ [Copy] [Edit] [Delete]                   │
│ ─────────────────────────────────────── │
│ 🕐 09:15 AM  |  156 words  |  $0.04    │
│ "I think we should prioritize the..."    │
│ [Copy] [Edit] [Delete]                   │
│ ─────────────────────────────────────── │
│                                          │
│ Yesterday (12)                           │
│ This Week (45)                           │
│ This Month (234)                         │
│                                          │
│ [Export All] [Clear History]             │
└──────────────────────────────────────────┘
```

### Analytics Dashboard
```
┌──────────────────────────────────────────┐
│ 📊 Analytics                      [v]   │
├──────────────────────────────────────────┤
│ Overview - This Week                     │
│                                          │
│ ┌─────────────────────────────────────┐ │
│ │  Words Transcribed                  │ │
│ │  ███████████░░░░░ 2,847 / 5,000    │ │
│ └─────────────────────────────────────┘ │
│                                          │
│ ⏱️  Average WPM: 148                    │
│ 🎤 Total Recording Time: 3h 42m         │
│ 💰 Total Cost: $8.42                    │
│                                          │
│ Daily Breakdown                          │
│ ┌─────────────────────────────────────┐ │
│ │     📈 Words per Day                │ │
│ │  500│     ┌──┐                      │ │
│ │  400│  ┌──┤  ├──┐                   │ │
│ │  300│──┤  │  │  ├──┐               │ │
│ │  200└──┴──┴──┴──┴──┴──             │ │
│ │      Mon Tue Wed Thu Fri Sat Sun    │ │
│ └─────────────────────────────────────┘ │
│                                          │
│ Top Words (This Week)                    │
│ 1. project (47×)                         │
│ 2. meeting (38×)                         │
│ 3. deadline (29×)                        │
│ 4. update (24×)                          │
│ 5. schedule (21×)                        │
│ [View All →]                             │
└──────────────────────────────────────────┘
```

### Settings Panel (Expanded)
```
┌──────────────────────────────────────────┐
│ ⚙️ Settings                        [✕]  │
├──────────────────────────────────────────┤
│ API Configuration                        │
│   Whisper API: sk-...wAA [Edit] [Test] │
│   Claude API:  sk-...pXw [Edit] [Test] │
│   ✅ Both keys validated                │
│                                          │
│ Voice Behavior                           │
│   Auto-refine:        [✓] ON            │
│   Whisper model:      whisper-1 [v]     │
│   Claude model:       sonnet-4 [v]      │
│   Language:           English [v]       │
│                                          │
│ Token Management                         │
│   Daily limit:        5,000 tokens      │
│   Budget alert at:    80% usage         │
│   Monthly budget:     $50               │
│                                          │
│ History & Privacy                        │
│   Auto-save transcripts: [✓] ON         │
│   Keep history for:      30 days [v]    │
│   Backup to cloud:       [ ] OFF        │
│                                          │
│ Analytics                                │
│   Track word usage:      [✓] ON         │
│   Calculate WPM:         [✓] ON         │
│   Export data:           [Export CSV]   │
│                                          │
│ [View Full Analytics Dashboard →]        │
└──────────────────────────────────────────┘
```

---

## Database Schema

### Tables

**transcriptions**
```sql
CREATE TABLE transcriptions (
    id INTEGER PRIMARY KEY,
    timestamp INTEGER NOT NULL,
    audio_duration_ms INTEGER,
    raw_text TEXT,
    refined_text TEXT,
    word_count INTEGER,
    wpm INTEGER,
    whisper_tokens INTEGER,
    claude_tokens INTEGER,
    whisper_cost REAL,
    claude_cost REAL,
    language TEXT,
    app_name TEXT
);
```

**words**
```sql
CREATE TABLE words (
    word TEXT PRIMARY KEY,
    count INTEGER DEFAULT 1,
    first_seen INTEGER,
    last_seen INTEGER
);
```

**daily_stats**
```sql
CREATE TABLE daily_stats (
    date TEXT PRIMARY KEY,
    total_words INTEGER,
    total_recordings INTEGER,
    total_duration_ms INTEGER,
    avg_wpm INTEGER,
    total_cost REAL
);
```

---

## Analytics Calculations

### Words Per Minute (WPM)
```kotlin
fun calculateWPM(wordCount: Int, durationMs: Long): Int {
    val minutes = durationMs / 60000.0
    return (wordCount / minutes).roundToInt()
}

// Live WPM (updates during recording)
fun calculateLiveWPM(currentWords: Int, elapsedMs: Long): Int {
    if (elapsedMs < 1000) return 0 // Wait 1 second
    return calculateWPM(currentWords, elapsedMs)
}
```

### Cost Tracking
```kotlin
// Whisper: $0.006 per minute
fun calculateWhisperCost(durationMs: Long): Double {
    val minutes = durationMs / 60000.0
    return minutes * 0.006
}

// Claude: $3 per 1M input tokens
fun calculateClaudeCost(tokens: Int): Double {
    return (tokens / 1_000_000.0) * 3.0
}
```

### Top Words
```kotlin
fun getTopWords(limit: Int = 20): List<Pair<String, Int>> {
    return db.query("SELECT word, count FROM words ORDER BY count DESC LIMIT ?", limit)
}
```

---

## Features Comparison

| Feature | Wispr Flow | WhisperDroid |
|---------|-----------|--------------|
| Voice dictation | ✅ | ✅ |
| AI refinement | ✅ | ✅ |
| Multi-language | ✅ 100+ | ✅ 100+ |
| **Transcription history** | ✅ Notes | ✅ Full history |
| **Analytics dashboard** | ❌ | ✅ WPM, costs, trends |
| **Token counter** | ❌ | ✅ Live + budgets |
| **Word frequency** | ❌ | ✅ Top words |
| Keyboard integration | iOS only | ✅ Android |
| Desktop support | ✅ | ❌ (Android only) |
| Cost | $8/month | Pay-per-use API |

---

## Cost Estimates

**Per Recording (30 seconds, ~75 words):**
- Whisper: $0.003
- Claude: $0.0006
- **Total: ~$0.0036**

**Daily Usage (50 recordings):**
- Total cost: ~$0.18/day
- Monthly: ~$5.40

**Heavy Usage (200 recordings/day):**
- Total cost: ~$0.72/day
- Monthly: ~$21.60

---

## Implementation Phases

### Phase 1: Core Keyboard ✅
- Fork Thumb-Key
- Basic Compose IME

### Phase 2: Voice Features
- Microphone button
- Whisper API integration
- Claude refinement
- Live WPM counter

### Phase 3: History & Storage
- Transcription database
- History screen
- Search functionality
- Export/import

### Phase 4: Analytics
- Daily stats tracking
- WPM calculations
- Word frequency analysis
- Charts/graphs

### Phase 5: Token Management
- Token counter UI
- Cost tracking
- Budget alerts
- Usage reports

### Phase 6: Polish
- Animations
- Error handling
- Offline mode
- Unit tests

---

## Next: HTML Mockups

See `DESIGN-MOCKUPS.html` for interactive visual designs.
