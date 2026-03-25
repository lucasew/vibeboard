# Agents Guidelines

## Context

This project is a simple Android application providing a voice-based keyboard (IME).
It uses Jetpack Compose for the UI.

## Operational Memory (Where To Find Things)

- `app/src/main/java/br/tec/lew/vibeboard/MainActivity.kt` -> The main entry point and setup orchestrator for onboarding.
- `app/src/main/java/br/tec/lew/vibeboard/VibeboardService.kt` -> The core InputMethodService containing the lifecycle and integration of the keyboard.
- `app/src/main/java/br/tec/lew/vibeboard/ErrorHandler.kt` -> Centralized error reporting and telemetry interface.
- `app/src/main/java/br/tec/lew/vibeboard/ui/VibeboardKeyboard.kt` -> The Compose UI representing the visual layout and gestures of the keyboard.

## Rules

- No silent failures. Any unexpected error in a catch block or error handler must use `reportError`.
- Since we do not have a centralized `reportError`, you must create it if it doesn't exist and use it.
