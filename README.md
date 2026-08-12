# vibeboard

A voice-based keyboard that types what you say.

- All processing happens locally
- The keyboard only uses space that would be covered by the navbar anyway
- Only the essential: backspace, record, and enter
- Swipes on the record button
  - Up: switches to the previous input method (IME picker if none)
  - Down: hides the keyboard
  - Left and Right: move the cursor

## Setup

Vibeboard is an input method. After install, open the app and complete:

1. **Grant microphone permission** — required for speech recognition.
2. **Enable Vibeboard** — system *Settings → System → Languages & input → On-screen keyboard* (wording varies by OEM), enable Vibeboard.
3. **Select Vibeboard** — choose it as the current input method (or switch to it from the IME picker while typing).

Until those steps are done, the keyboard will not appear as a system IME and voice input will not run.

![](./screenshot.png)
