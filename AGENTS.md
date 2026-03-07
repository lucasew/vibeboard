# Agents Guidelines

## Context

This project is a simple Android application providing a voice-based keyboard (IME).
It uses Jetpack Compose for the UI.

## Structure

- `app/src/main/java/br/tec/lew/vibeboard/`
  - `MainActivity.kt`: The main entry point and setup screen.
  - `VibeboardService.kt`: The core InputMethodService containing the logic and UI of the keyboard.

## Rules

- No silent failures. Any unexpected error in a catch block or error handler must use `reportError`.
- Since we do not have a centralized `reportError`, you must create it if it doesn't exist and use it.
