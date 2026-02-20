# WhisperDroid User Experience Flow

---

## First-Time User Journey

### 1. **App Installation**
```
Play Store → Install WhisperDroid → Open
```

### 2. **Welcome & Setup (3 screens)**

**Screen 1: Welcome**
```
┌─────────────────────────────────┐
│                                 │
│         🎤                      │
│     WhisperDroid                │
│                                 │
│  Voice-to-text powered by AI    │
│                                 │
│  • Whisper transcription        │
│  • Claude refinement            │
│  • Full history & analytics     │
│                                 │
│     [Get Started →]             │
│                                 │
└─────────────────────────────────┘
```

**Screen 2: Enable Keyboard**
```
┌─────────────────────────────────┐
│  Enable WhisperDroid            │
│                                 │
│  [Visual: Settings icon]        │
│                                 │
│  1. Tap "Enable Keyboard"       │
│  2. Find WhisperDroid           │
│  3. Toggle ON                   │
│  4. Return here                 │
│                                 │
│  [Enable Keyboard]   [Skip]    │
└─────────────────────────────────┘
Taps [Enable Keyboard] →
Opens Android Settings →
Returns when done
```

**Screen 3: API Keys**
```
┌─────────────────────────────────┐
│  Configure API Keys             │
│                                 │
│  Groq (Audio)                   │
│  [                           ]  │
│  Get key: groq.com              │
│                                 │
│  OpenRouter (Text)              │
│  [                           ]  │
│  Get key: openrouter.ai         │
│                                 │
│  [Test Keys]      [Continue]   │
└─────────────────────────────────┘
```

### 3. **Set as Default Keyboard**
```
Android Settings prompt →
"Change keyboard to WhisperDroid?"
[Yes] → WhisperDroid active
```

---

## Daily Usage Flow

### Recording Flow

```
User taps input field
    ↓
WhisperDroid keyboard appears
    ↓
┌─────────────────────────────────┐
│ ⚙️  History(23)  🎤  [1.2k]   │ ← Top bar
│                                 │
│       E     T     A             │
│       O     I     N             │ ← 3x3 grid
│       S     H     R             │
│                                 │
│   [Space] [⌫] [↵]               │
└─────────────────────────────────┘
    ↓
User taps 🎤 button
    ↓
Permission check
    ├─ Not granted → Show permission dialog
    └─ Granted → Start recording
    ↓
┌─────────────────────────────────┐
│ 🔴 Recording...      [Stop]    │
│                                 │
│       152 WPM                   │
│                                 │
│   ████░░░░  00:14               │
│                                 │
│ "testing the voice..."          │ ← Live transcript
│                                 │
│ 21 words · $0.003               │
└─────────────────────────────────┘
    ↓
User taps [Stop]
    ↓
Show processing state
┌─────────────────────────────────┐
│ ⏳ Transcribing...              │
│                                 │
│   [●●●●●○○○○○] 50%             │
│                                 │
│   Sending to Whisper API...     │
└─────────────────────────────────┘
    ↓
Whisper transcription complete
    ↓
If auto-refine enabled:
┌─────────────────────────────────┐
│ ⏳ Refining with Claude...      │
│                                 │
│   [●●●●●●●○○○] 70%             │
│                                 │
│   Improving clarity...          │
└─────────────────────────────────┘
    ↓
Text inserted into input field
    ↓
Save to history database
    ↓
Update token counter
    ↓
Return to keyboard
```

### Quick Actions Flow

```
While on keyboard:
├─ Swipe down on Settings → Open settings panel
├─ Tap History → View transcription history
├─ Tap Token badge → View usage breakdown
└─ Tap 🎤 button → Start recording
```

---

## Power User Flows

### 1. **Viewing History**

```
Tap History(23)
    ↓
┌─────────────────────────────────┐
│ 📝 History         [Search 🔍] │
├─────────────────────────────────┤
│ Today (8)                       │
│                                 │
│ 🕐 10:23 AM  42 words  $0.01  │
│ "Can you send me the..."        │
│ [Copy] [Edit] [Delete]          │
│ ───────────────────────────── │
│                                 │
│ 🕐 09:15 AM  156 words $0.04  │
│ "I think we should..."          │
│ [Copy] [Edit] [Delete]          │
└─────────────────────────────────┘
    ↓
Actions:
├─ [Copy] → Text copied to clipboard
├─ [Edit] → Open edit dialog → Save
├─ [Delete] → Confirm → Remove from DB
└─ [Search] → Filter by keywords
```

### 2. **Analytics Dashboard**

```
Settings → Analytics
    ↓
┌─────────────────────────────────┐
│ 📊 Analytics     [This Week ▼] │
├─────────────────────────────────┤
│ Words Transcribed               │
│ ███████░░░ 2,847 / 5,000       │
│                                 │
│ ⏱️ Avg WPM: 148                │
│ 💰 Cost: $8.42                 │
│                                 │
│ Daily Breakdown                 │
│ ┌─────────────────────────┐   │
│ │ 📈                       │   │
│ │  █  ██ █ ██            │   │
│ │ Mon Tue Wed Thu Fri     │   │
│ └─────────────────────────┘   │
│                                 │
│ Top Words                       │
│ 1. project (47×)                │
│ 2. meeting (38×)                │
│ 3. deadline (29×)               │
└─────────────────────────────────┘
    ↓
Actions:
├─ Change timeframe → Refresh data
├─ Export CSV → Save to storage
└─ View top words → See frequency
```

### 3. **Managing Presets**

```
Settings → Preset Prompts
    ↓
┌─────────────────────────────────┐
│ 📝 Prompt Presets      [+ New] │
├─────────────────────────────────┤
│ Whisper Prompts (5)             │
│                                 │
│ ⭐ Technical Vocabulary         │
│ "API, SDK, OAuth..."            │
│ [Edit] [Delete]                 │
│ ───────────────────────────── │
│                                 │
│ Meeting Notes                   │
│ " - Facilitator. - Team..."     │
│ [Edit] [Delete] [Set Default]   │
└─────────────────────────────────┘
    ↓
Tap [+ New]
    ↓
┌─────────────────────────────────┐
│ Create Preset                   │
│                                 │
│ Name:                           │
│ [                            ]  │
│                                 │
│ Type: [Whisper ▼]              │
│                                 │
│ Prompt:                         │
│ [                            ]  │
│ [                            ]  │
│ [                            ]  │
│                                 │
│ [✓] Set as default              │
│                                 │
│ [Cancel]           [Save]      │
└─────────────────────────────────┘
    ↓
Tap [Save] → Add to database → Return
```

---

## Error Handling Flows

### 1. **No API Key**
```
User taps 🎤
    ↓
Check API keys
    ↓
Missing → Show alert
┌─────────────────────────────────┐
│ ⚠️ API Keys Required           │
│                                 │
│ Please configure your API keys  │
│ in Settings before recording.   │
│                                 │
│ [Cancel]    [Go to Settings]   │
└─────────────────────────────────┘
```

### 2. **No Internet**
```
Recording stopped
    ↓
Send to Whisper API → Network error
    ↓
┌─────────────────────────────────┐
│ ❌ No Internet Connection      │
│                                 │
│ Audio saved locally. Will retry │
│ when connection is restored.    │
│                                 │
│ [View Queue]          [OK]     │
└─────────────────────────────────┘
    ↓
Save to pending queue
    ↓
Background service monitors connection
    ↓
When online → Auto-retry
```

### 3. **API Error**
```
Whisper request → 401 Unauthorized
    ↓
┌─────────────────────────────────┐
│ ❌ API Key Invalid             │
│                                 │
│ Your Groq API key is invalid    │
│ or expired. Please update.      │
│                                 │
│ [Cancel]    [Update Key]       │
└─────────────────────────────────┘
```

### 4. **Budget Exceeded**
```
Check token counter → Over daily limit
    ↓
Show warning before recording
┌─────────────────────────────────┐
│ ⚠️ Budget Alert                │
│                                 │
│ You've reached 90% of your      │
│ daily budget ($4.50 / $5.00).   │
│                                 │
│ Continue anyway?                │
│                                 │
│ [Cancel]          [Continue]   │
└─────────────────────────────────┘
```

---

## Settings Management Flow

```
Tap ⚙️ (Settings)
    ↓
┌─────────────────────────────────┐
│ ⚙️ Settings              [✕]  │
├─────────────────────────────────┤
│ • API Configuration             │
│ • Voice Behavior                │
│ • Token Management              │
│ • History & Privacy             │
│ • Analytics                     │
│ • Preset Prompts                │
│ • About                         │
└─────────────────────────────────┘
    ↓
Each section expands/navigates:
    ├─ API Config → Edit keys, test, select models
    ├─ Voice Behavior → Auto-refine, language, etc.
    ├─ Token Management → Limits, alerts, history
    ├─ History & Privacy → Retention, export, clear
    ├─ Analytics → Dashboard (see above)
    ├─ Preset Prompts → Manage (see above)
    └─ About → Version, licenses, support
```

---

## Gesture Shortcuts

### On Keyboard
```
🎤 button:
├─ Tap → Start/stop recording
├─ Long press → Continuous recording mode
└─ Swipe up → Voice settings

History:
├─ Tap → Open history
└─ Long press → Quick search

Token badge:
├─ Tap → Usage breakdown
└─ Long press → Budget settings

⚙️ Settings:
├─ Tap → Full settings
└─ Swipe down from top bar → Quick settings panel
```

---

## Notification Patterns

### During Recording
```
Status: "🎤 Recording... (00:14)"
Action: [Stop]
```

### Processing
```
Status: "⏳ Transcribing audio..."
Progress: 50%
```

### Completion
```
Status: "✅ Transcribed 42 words"
Action: [View] [Dismiss]
```

### Error
```
Status: "❌ Transcription failed"
Action: [Retry] [Dismiss]
```

### Budget Alert
```
Status: "⚠️ 90% of daily budget used"
Action: [View Details] [Dismiss]
```

---

## State Persistence

### What Gets Saved
```
- API keys (encrypted)
- Model selections
- Preset prompts
- Transcription history (30 days)
- Token usage stats
- User preferences
- Default settings
```

### What Gets Synced (Optional)
```
- Preset prompts (cloud backup)
- Settings (cross-device)
- History (encrypted backup)
```

---

## Accessibility

### Voice Feedback
```
- "Recording started"
- "Recording stopped"
- "Transcription complete"
- "X words transcribed"
```

### Screen Reader Support
```
- All buttons labeled
- Current state announced
- Progress updates
- Error descriptions
```

### Haptic Feedback
```
- Recording start: Strong pulse
- Recording stop: Light tap
- Success: Double tap
- Error: Long vibration
```

---

## Performance Targets

| Metric | Target | Notes |
|--------|--------|-------|
| Keyboard load | < 500ms | First appearance |
| Recording start | < 100ms | Tap to mic active |
| Transcription | < 3s | 30s audio |
| Refinement | < 2s | 100 tokens |
| History load | < 200ms | Last 50 items |
| Analytics render | < 500ms | All charts |

---

## Next: Interactive Dashboard

See `DASHBOARD.html` for live, interactive design mockups.
