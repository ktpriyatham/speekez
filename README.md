# SpeekEZ

Voice-to-text Android keyboard with AI refinement. Built as a [FlorisBoard](https://github.com/florisboard/florisboard) fork.

SpeekEZ adds voice recording, transcription (via Whisper-compatible APIs), and AI text refinement (via Claude/OpenRouter) directly into the keyboard experience. Users can configure presets for different contexts (work, personal, AI mode) with per-preset language, tone, and API settings.

## Features

- Voice-to-text recording with real-time waveform visualization
- AI text refinement using configurable LLM providers
- Multiple presets with custom system prompts, languages, and API routing
- Home screen widgets (1x1 quick-record, 2x1 preset selector)
- Floating overlay widget for system-wide voice input
- Haptic feedback with distinct patterns for recording events
- Transcription history with search, filters, and favorites
- Material 3 theming with light/dark/system modes

## Architecture

```
app/          Main application — activities, screens (Compose), IME service
api/          API clients — Whisper, Claude, OpenRouter
core/         Shared utilities
data/         Room database, entities, DAOs, seeders
security/     EncryptedSharedPreferences manager
voice/        VoiceManager, audio recording, haptic feedback
widget/       Home screen widgets, floating widget service
```

## Build

### Prerequisites

- Android Studio Ladybug or later
- JDK 11+
- Android SDK 36

### Debug Build

```bash
./gradlew :app:assembleDebug
```

The debug APK will be at `app/build/outputs/apk/debug/`.

### Release Build

1. Copy `signing.properties.template` to `signing.properties`
2. Fill in your keystore details
3. Add the following to your `gradle.properties` (or `~/.gradle/gradle.properties`):

```properties
RELEASE_STORE_FILE=path/to/keystore.jks
RELEASE_STORE_PASSWORD=your_password
RELEASE_KEY_ALIAS=your_alias
RELEASE_KEY_PASSWORD=your_key_password
```

4. Build:

```bash
./gradlew :app:assembleRelease
```

The release APK will be at `app/build/outputs/apk/release/`.

## Based on FlorisBoard

SpeekEZ is built on [FlorisBoard](https://github.com/florisboard/florisboard), an open-source Android keyboard. The core keyboard functionality, theming engine, and clipboard manager come from FlorisBoard. SpeekEZ adds the voice recording, transcription, AI refinement, preset system, and widget layers on top.

## License

```
Copyright (C) 2021-2025 The FlorisBoard Contributors
Copyright (C) 2025-2026 SpeekEZ Contributors

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

    http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
```
