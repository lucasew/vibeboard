# Agents Guidelines

## Context

This project is a simple Android application providing a voice-based keyboard (IME).
It uses Jetpack Compose for the UI.

## Operational Memory (Where To Find Things)

- `app/src/main/java/br/tec/lew/vibeboard/MainActivity.kt` -> The main entry point, onboarding, and setup screen orchestrator.
- `app/src/main/java/br/tec/lew/vibeboard/VibeboardService.kt` -> The core InputMethodService managing active keyboard lifecycle, logic, and UI.
- `app/src/main/java/br/tec/lew/vibeboard/ErrorHandler.kt` -> Centralized error reporting utility (`reportError`) that routes errors to observability sinks.
- `app/src/main/java/br/tec/lew/vibeboard/ui/VibeboardKeyboard.kt` -> The main Compose UI view for the keyboard surface.

## Rules

- No silent failures. Any unexpected error in a catch block or error handler must use `reportError` from `ErrorHandler.kt`.
