package br.tec.lew.vibeboard

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class EditorEnterTest {

    @Test
    fun searchAction_returnsSearch() {
        assertEquals(
            IME_ACTION_SEARCH,
            resolveEnterEditorAction(IME_ACTION_SEARCH)
        )
    }

    @Test
    fun doneAction_returnsDone() {
        assertEquals(
            IME_ACTION_DONE,
            resolveEnterEditorAction(IME_ACTION_DONE)
        )
    }

    @Test
    fun sendAction_returnsSend() {
        assertEquals(
            IME_ACTION_SEND,
            resolveEnterEditorAction(IME_ACTION_SEND)
        )
    }

    @Test
    fun nextAction_returnsNext() {
        assertEquals(
            IME_ACTION_NEXT,
            resolveEnterEditorAction(IME_ACTION_NEXT)
        )
    }

    @Test
    fun goAction_returnsGo() {
        assertEquals(
            IME_ACTION_GO,
            resolveEnterEditorAction(IME_ACTION_GO)
        )
    }

    @Test
    fun unspecified_returnsNull() {
        assertNull(resolveEnterEditorAction(IME_ACTION_UNSPECIFIED))
    }

    @Test
    fun none_returnsNull() {
        assertNull(resolveEnterEditorAction(IME_ACTION_NONE))
    }

    @Test
    fun noEnterActionFlag_returnsNullEvenWithDone() {
        assertNull(
            resolveEnterEditorAction(IME_ACTION_DONE or IME_FLAG_NO_ENTER_ACTION)
        )
    }

    @Test
    fun explicitActionId_overridesImeOptionsMask() {
        // Options say DONE; label/actionId says SEARCH
        assertEquals(
            IME_ACTION_SEARCH,
            resolveEnterEditorAction(IME_ACTION_DONE, actionId = IME_ACTION_SEARCH)
        )
    }

    @Test
    fun highBitsInImeOptions_doNotPolluteAction() {
        // IME_FLAG_NO_FULLSCREEN and friends live above the action mask
        val imeFlagNoFullscreen = 0x2000000
        assertEquals(
            IME_ACTION_DONE,
            resolveEnterEditorAction(IME_ACTION_DONE or imeFlagNoFullscreen)
        )
    }
}
