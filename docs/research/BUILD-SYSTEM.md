# Build System Overview

Florisboard uses a modern Gradle setup with Kotlin DSL and Version Catalogs.

## Gradle Setup

- **Kotlin DSL**: All build files use `.gradle.kts`.
- **Version Catalogs**: Dependencies are managed in `gradle/libs.versions.toml`. This makes it easy to add new dependencies like OkHttp or Retrofit for API calls.
- **Multi-module**: Each major component is a separate module, promoting separation of concerns.

## Dependency Management

To add a new dependency (e.g., OkHttp for Whisper API):
1.  Open `gradle/libs.versions.toml`.
2.  Add the version in `[versions]`.
3.  Add the library in `[libraries]`.
4.  Include it in `app/build.gradle.kts` under `dependencies`.

## Build Variants

Florisboard defines several build types in `app/build.gradle.kts`:
- **`debug`**: Standard development build. Includes a `.debug` suffix to the application ID.
- **`beta`**: Pre-release build with minification enabled.
- **`release`**: Production build.
- **`benchmark`**: Used for performance profiling.

## Signing Configuration

Signing configs are managed via Gradle properties. For development, the standard Android debug key is used. For release, you'll need to configure `storeFile`, `storePassword`, etc., in a `local.properties` or environment variables (patterns are usually kept out of VCS).

## Key Build Flags

Found in `gradle.properties`:
- `projectMinSdk`: 26 (Android 8.0)
- `projectTargetSdk`: 36 (Android 15 / 16 Preview)
- `projectCompileSdk`: 36

## Recommended Additions for Speekez

- **Network Client**: Add `okhttp` or `retrofit` for communication with Whisper and Claude APIs.
- **Audio Processing**: Might need a library for audio encoding (e.g., Ogg/Opus) if Whisper requires a specific format.
- **Concurrency**: `kotlinx-coroutines-android` is already included and should be used for all background work.
