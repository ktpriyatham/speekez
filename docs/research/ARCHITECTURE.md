# Florisboard Architecture Overview

This document describes the high-level architecture of Florisboard, which serves as the foundation for Speekez.

## High-Level Architecture

Florisboard follows a modular architecture with a focus on Jetpack Compose for the UI and a custom management layer for IME logic.

### 1. Multi-Module Structure
The project is divided into several Gradle modules:
- `:app`: The main application module containing the `InputMethodService` and core IME logic.
- `:lib:snygg`: The custom theming engine.
- `:lib:compose`: Shared Compose UI components.
- `:lib:native`: JNI bridge for performance-critical tasks (e.g., suggestions engine).
- `:lib:kotlin`, `:lib:android`: Base utility libraries.

### 2. Dependency Injection (DI)
Florisboard uses a **Manual Dependency Injection** pattern. Key managers are instantiated lazily within the `FlorisApplication` class and accessed via `Context` extension functions:
- `Context.keyboardManager()`
- `Context.editorInstance()`
- `Context.subtypeManager()`
- `Context.themeManager()`

This avoids the overhead of complex DI frameworks while maintaining a single source of truth for core components.

### 3. State Management
The keyboard state is centralized in `ObservableKeyboardState`. It uses a "snapshot and update" pattern:
- Managers update the state using `batchEdit { ... }` to ensure atomicity and minimize recompositions.
- Composables observe the state via `StateFlow` or Compose `State`.

### 4. Data Flow
1. **User Input**: Hardware key events or touch events on the virtual keyboard.
2. **Dispatching**: Events are sent to `InputEventDispatcher`.
3. **Processing**: `KeyboardManager` receives events and decides the action (e.g., commit character, handle backspace, toggle mode).
4. **Execution**: `EditorInstance` interacts with the system's `InputConnection` to modify text in the target app.
5. **Rendering**: UI components (Compose) react to state changes in `KeyboardManager` and `ObservableKeyboardState`.

## Key Design Patterns

- **Observer Pattern**: Extensively used via Kotlin Flows and Compose States for UI reactivity.
- **Bridge Pattern**: `ImeRootView` acts as a bridge between the Android `View` system (required by `InputMethodService`) and Jetpack Compose.
- **Strategy Pattern**: Different `ComputingEvaluator` implementations handle how keys are rendered and behaved based on the current context.
- **Lazy Initialization**: Most managers and resources are loaded only when needed to keep the service startup fast.

## Design Decisions for Speekez
- **Voice-to-Text Integration**: We will follow the existing manager pattern by adding a `VoiceManager` or similar to handle audio recording and Whisper API calls.
- **Lifecycle Management**: Since `FlorisImeService` is a `LifecycleInputMethodService`, we can safely use `lifecycleScope` for coroutines and Whisper/Claude API interactions.
