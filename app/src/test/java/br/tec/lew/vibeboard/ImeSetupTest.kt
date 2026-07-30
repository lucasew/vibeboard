package br.tec.lew.vibeboard

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ImeSetupTest {

    @Test
    fun inputMethodIdMatchesPackage() {
        assertTrue(
            isInputMethodIdForPackage(
                "br.tec.lew.vibeboard/.VibeboardService",
                "br.tec.lew.vibeboard"
            )
        )
    }

    @Test
    fun inputMethodIdRejectsOtherPackage() {
        assertFalse(
            isInputMethodIdForPackage(
                "com.android.inputmethod.latin/.LatinIME",
                "br.tec.lew.vibeboard"
            )
        )
    }

    @Test
    fun inputMethodIdRejectsPackagePrefixCollision() {
        // "br.tec.lew.vibeboard.evil/" must not match package "br.tec.lew.vibeboard"
        assertFalse(
            isInputMethodIdForPackage(
                "br.tec.lew.vibeboard.evil/.EvilIme",
                "br.tec.lew.vibeboard"
            )
        )
    }

    @Test
    fun inputMethodIdRejectsNullOrEmpty() {
        assertFalse(isInputMethodIdForPackage(null, "br.tec.lew.vibeboard"))
        assertFalse(isInputMethodIdForPackage("", "br.tec.lew.vibeboard"))
        assertFalse(isInputMethodIdForPackage("br.tec.lew.vibeboard/.VibeboardService", ""))
    }
}
