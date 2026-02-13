# Theming System (Snygg)

Florisboard uses a custom, highly flexible theming engine called **Snygg**.

## Snygg Architecture

Snygg (Swedish for "neat" or "handsome") allows for element-based styling similar to CSS.
- **Rules**: Styles are defined using selectors like `smartbar` or `key[code=32]`.
- **Properties**: Each rule can have properties like `background`, `foreground`, `font-size`, `border`, etc.
- **Dynamic**: Themes can be changed at runtime without restarting the service.

## Key Components

- **`FlorisImeUi.kt`**: This enum defines all themeable elements. Any new UI element we add must be registered here if we want it to be themeable.
- **`lib/snygg`**: The core implementation of the styling engine.
- **`SnyggBox`**: A Compose component that automatically applies styles based on an element name.

## Adding a Themed UI Element

If we add a voice recording indicator:
1.  Add `VoiceRecordingIndicator` to `FlorisImeUi` enum.
2.  In the Compose UI:
    ```kotlin
    SnyggBox(elementName = FlorisImeUi.VoiceRecordingIndicator.elementName) {
        // Content
    }
    ```
3.  The indicator will now automatically inherit styles from the active theme.

## Dark Mode & Material Design

- **Dark Mode**: Snygg handles dark mode by switching stylesheets.
- **Material 3**: Recent versions of Florisboard have moved towards Material 3 compliance in the settings and some IME components, using the `MaterialKolor` library for dynamic color schemes.

## Styling Gotchas

- **Selector Specificity**: Like CSS, Snygg has rules for which style wins if multiple rules match.
- **Performance**: While efficient, avoid deeply nested `SnyggBox`es in the main key rendering path. For the Smartbar, it's perfectly fine.
- **Default Styles**: Always provide sensible default styles in code for when a theme doesn't define a specific element.
