package br.tec.lew.vibeboard

/**
 * Resolves which editor action Enter should trigger for the focused field.
 *
 * Returns an [android.view.inputmethod.EditorInfo] action id for
 * [android.view.inputmethod.InputConnection.performEditorAction], or null when
 * a raw [android.view.KeyEvent.KEYCODE_ENTER] is the correct fallback
 * (no action, unspecified, or [android.view.inputmethod.EditorInfo.IME_FLAG_NO_ENTER_ACTION]).
 *
 * Bit values match [android.view.inputmethod.EditorInfo] so this stays pure-JVM testable.
 */
fun resolveEnterEditorAction(imeOptions: Int, actionId: Int = 0): Int? {
    // EditorInfo.IME_FLAG_NO_ENTER_ACTION — field wants newline, not Done/Search/etc.
    if (imeOptions and IME_FLAG_NO_ENTER_ACTION != 0) return null

    val action = if (actionId != IME_ACTION_UNSPECIFIED) {
        actionId
    } else {
        imeOptions and IME_MASK_ACTION
    }

    return when (action) {
        IME_ACTION_UNSPECIFIED, IME_ACTION_NONE -> null
        else -> action
    }
}

// Keep in sync with android.view.inputmethod.EditorInfo
internal const val IME_MASK_ACTION = 0x000000ff
internal const val IME_ACTION_UNSPECIFIED = 0
internal const val IME_ACTION_NONE = 1
internal const val IME_ACTION_GO = 2
internal const val IME_ACTION_SEARCH = 3
internal const val IME_ACTION_SEND = 4
internal const val IME_ACTION_NEXT = 5
internal const val IME_ACTION_DONE = 6
internal const val IME_FLAG_NO_ENTER_ACTION = 0x40000000
