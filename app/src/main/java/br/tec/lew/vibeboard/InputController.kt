package br.tec.lew.vibeboard

import android.view.KeyEvent
import android.view.inputmethod.InputConnection

/**
 * Handles interactions with the InputConnection.
 *
 * Rationale: Single Responsibility Principle (Martin) & Command Pattern (GoF).
 * Delegating text manipulation and key event interactions (delete text, commit text,
 * send key events) to a specific handler clarifies the InputMethodService's role
 * as a mediator and encapsulates keyboard output logic.
 */
class InputController(private val inputConnectionProvider: () -> InputConnection?) {

    private val currentInputConnection: InputConnection?
        get() = inputConnectionProvider()

    fun commitText(text: String, newCursorPosition: Int) {
        currentInputConnection?.commitText(text, newCursorPosition)
    }

    fun setComposingText(text: String, newCursorPosition: Int) {
        currentInputConnection?.setComposingText(text, newCursorPosition)
    }

    fun finishComposingText() {
        currentInputConnection?.finishComposingText()
    }

    fun deleteSurroundingText(beforeLength: Int, afterLength: Int) {
        currentInputConnection?.deleteSurroundingText(beforeLength, afterLength)
    }

    fun sendEnterEvent() {
        finishComposingText()
        currentInputConnection?.sendKeyEvent(KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_ENTER))
        currentInputConnection?.sendKeyEvent(KeyEvent(KeyEvent.ACTION_UP, KeyEvent.KEYCODE_ENTER))
    }

    fun moveCursor(direction: Int) {
        val keyEvent = if (direction > 0) KeyEvent.KEYCODE_DPAD_RIGHT else KeyEvent.KEYCODE_DPAD_LEFT
        currentInputConnection?.sendKeyEvent(KeyEvent(KeyEvent.ACTION_DOWN, keyEvent))
        currentInputConnection?.sendKeyEvent(KeyEvent(KeyEvent.ACTION_UP, keyEvent))
    }
}
