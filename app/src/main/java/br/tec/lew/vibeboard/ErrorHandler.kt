package br.tec.lew.vibeboard

import android.util.Log

/**
 * Centralized error reporting boundary for the entire application.
 *
 * This function serves as the single observability sink for all unhandled exceptions and
 * non-recoverable expected errors across the application. By enforcing this funnel, we ensure
 * consistent error enrichment (e.g., adding user state, device metadata) before emitting logs
 * or sending payloads to external crash reporters like Sentry.
 *
 * It is strictly forbidden to use `Log.e` or swallow exceptions directly in `catch` blocks.
 * If an error is caught but cannot be completely resolved, it must be reported here.
 *
 * @param error The root cause exception or throwable that occurred.
 * @param context Optional structured metadata to attach to the crash report (e.g., {"speech_code": 3, "state": "listening"}).
 *                Useful for adding contextual clues without polluting the error message.
 */
fun reportError(error: Throwable, context: Map<String, Any> = emptyMap()) {
    // In the future, this should report to Sentry or another crash reporting tool to persist and aggregate reports remotely.
    val contextString = if (context.isNotEmpty()) " Context: $context" else ""
    Log.e("VibeboardError", "An error occurred: $error$contextString", error)
}
