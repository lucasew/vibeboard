# Agents Guidelines

## Context

This project is a simple Android application providing a voice-based keyboard (IME).
It uses Jetpack Compose for the UI.

## Where To Find Things (Operational Memory)

- `app/src/main/java/br/tec/lew/vibeboard/MainActivity.kt` -> Main entry point and setup screen.
- `app/src/main/java/br/tec/lew/vibeboard/VibeboardService.kt` -> Core InputMethodService containing the logic of the keyboard.
- `app/src/main/java/br/tec/lew/vibeboard/ui/VibeboardKeyboard.kt` -> The Compose UI for the keyboard.
- `app/src/main/java/br/tec/lew/vibeboard/ErrorHandler.kt` -> Centralized error reporting function (`reportError`).
- `mise.toml` -> Configures the `mise` environment, tools, and executable tasks like `ci` and `test`.

## Rules

- No silent failures. Any unexpected error in a catch block or error handler must use `reportError`.
- Since we do not have a centralized `reportError`, you must create it if it doesn't exist and use it.
