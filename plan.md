1. **Analyze VibeboardService.kt:** The service class is currently doing everything: UI, lifecycle management, speech recognition, and input method logic.
2. **Apply Single Responsibility Principle (SRP):** Extract speech recognition logic to a `SpeechRecognizerHelper` or similar class.
3. **Refactor UI code:** Move Compose UI into separate composable files/functions, outside of the main service file.
4. **Implement Global Error Handling:** Add `reportError` centralized handling.
