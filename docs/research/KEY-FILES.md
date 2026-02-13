# Key Files Analysis

This document identifies the most important files in the Florisboard/Speekez codebase for understanding and extending the keyboard with voice features.

## Core IME Service & Entry Points

1.  **`FlorisImeService.kt`**
    - **Purpose**: Main `InputMethodService` implementation.
    - **Interaction**: Manages the keyboard lifecycle and orchestrates managers.
2.  **`FlorisApplication.kt`**
    - **Purpose**: Application class handling global initialization and manual DI.
    - **Interaction**: Entry point for initializing our voice-to-text components.
3.  **`ImeRootView.kt`**
    - **Purpose**: The bridge between Android Views and Jetpack Compose.
    - **Interaction**: Root of the UI tree.
4.  **`ImeWindowController.kt`**
    - **Purpose**: Manages the window lifecycle and layout insets.
    - **Interaction**: Controls how the keyboard window is positioned and sized.

## State & Logic Managers

5.  **`KeyboardManager.kt`**
    - **Purpose**: Core logic for handling key events and managing keyboard layouts.
    - **Interaction**: We'll modify this to handle the voice recording toggle.
5.  **`InputEventDispatcher.kt`**
    - **Purpose**: Dispatches low-level touch/key events.
    - **Interaction**: Understand how events flow from UI to logic.
6.  **`EditorInstance.kt`**
    - **Purpose**: Manages communication with the target app's `InputConnection`.
    - **Interaction**: Used to "type" the transcribed text into the editor.
8.  **`ObservableKeyboardState.kt`**
    - **Purpose**: Centralized state for the IME UI.
    - **Interaction**: Add fields for voice recording state (e.g., `isRecording`).
9.  **`InputFeedbackController.kt`**
    - **Purpose**: Handles haptic and audio feedback for key presses.
    - **Interaction**: Can be used to provide haptic feedback when starting/stopping recording.
10. **`ComputingEvaluator.kt`**
    - **Purpose**: Evaluates key states and labels based on the current context.
    - **Interaction**: Used to determine if the voice button should be visible.
11. **`LayoutManager.kt`**
    - **Purpose**: Loads and manages keyboard layout definitions.
    - **Interaction**: Defines where keys are placed.

## UI & Layout

12. **`ImeWindow.kt`**
    - **Purpose**: Main UI container for the keyboard.
    - **Interaction**: High-level UI structure.
13. **`TextInputLayout.kt`**
    - **Purpose**: Layout for the text keyboard, including Smartbar.
    - **Interaction**: Where we'll see the voice button integrated.
14. **`Smartbar.kt`**
    - **Purpose**: The toolbar above the keyboard.
    - **Interaction**: Main integration point for the voice button.
15. **`QuickAction.kt`**
    - **Purpose**: Defines actions that can appear in the Smartbar.
    - **Interaction**: Define a new `VOICE_RECORD` action.
16. **`QuickActionsOverflowPanel.kt`**
    - **Purpose**: Panel for actions that don't fit in the main Smartbar row.
    - **Interaction**: The voice button can also reside here.
17. **`TextKeyboardLayout.kt`**
    - **Purpose**: Renders the actual keys.
    - **Interaction**: If we want a dedicated voice key on the main layout.

## Configuration & Resources

18. **`FlorisPreferenceStore.kt`**
    - **Purpose**: Type-safe preference management.
    - **Interaction**: Store Whisper/Claude API keys and settings.
19. **`SubtypeManager.kt`**
    - **Purpose**: Manages languages and keyboard layouts.
    - **Interaction**: Handle language selection for transcription.
20. **`FlorisImeUi.kt`**
    - **Purpose**: Defines themed elements for the Snygg engine.
    - **Interaction**: Add themed elements for the recording indicator.
21. **`NlpManager.kt`**
    - **Purpose**: Manages suggestions and autocorrection.
    - **Interaction**: Could potentially integrate with transcription for better accuracy.

## Theming & Styling

22. **`Snygg.kt`**
    - **Purpose**: Core of the theming engine.
    - **Interaction**: Understand how styles are applied.
23. **`FlorisImeTheme.kt`**
    - **Purpose**: Applies themes to the Compose UI.
    - **Interaction**: Ensure our new UI elements are correctly themed.

## Build & Dependencies

24. **`libs.versions.toml`**
    - **Purpose**: Centralized dependency management.
    - **Interaction**: Add dependencies for OkHttp, Whisper client, etc.
25. **`build.gradle.kts` (App & Root)**
    - **Purpose**: Build configuration.
    - **Interaction**: Configure build flags and module dependencies.

## Key Integration Files for Voice
- **`FlorisImeService.kt`**: To handle service-level lifecycle for recording.
- **`KeyboardManager.kt`**: To toggle recording state.
- **`Smartbar.kt`**: To display the microphone icon.
- **`EditorInstance.kt`**: To commit transcribed text.
- **`FlorisPreferenceStore.kt`**: To manage API credentials.
