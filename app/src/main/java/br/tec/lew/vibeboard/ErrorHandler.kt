package br.tec.lew.vibeboard

import android.util.Log

/**
 * Centralized error reporting function.
 * All code paths that handle unexpected errors MUST funnel through this function.
 */
fun reportError(error: Throwable, context: Map<String, Any> = emptyMap()) {
    // In the future, this should report to Sentry or another crash reporting tool.
    val contextString = if (context.isNotEmpty()) " Context: $context" else ""
    Log.e("VibeboardError", "An error occurred: $error$contextString", error)
}
