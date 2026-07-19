package br.tec.lew.vibeboard

import android.content.Context
import android.content.Intent
import android.graphics.Rect
import android.graphics.Region
import android.graphics.drawable.ColorDrawable
import android.inputmethodservice.InputMethodService
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.view.KeyEvent
import android.view.View
import android.view.inputmethod.InputMethodManager
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.layout.positionInWindow
import androidx.compose.ui.platform.ComposeView
import androidx.core.view.WindowCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LifecycleRegistry
import androidx.lifecycle.ViewModelStore
import androidx.lifecycle.ViewModelStoreOwner
import androidx.lifecycle.setViewTreeLifecycleOwner
import androidx.lifecycle.setViewTreeViewModelStoreOwner
import androidx.savedstate.SavedStateRegistry
import androidx.savedstate.SavedStateRegistryController
import androidx.savedstate.SavedStateRegistryOwner
import androidx.savedstate.setViewTreeSavedStateRegistryOwner
import br.tec.lew.vibeboard.ui.VibeboardKeyboard
import br.tec.lew.vibeboard.ui.theme.VibeboardTheme
import java.util.Locale
import kotlin.math.roundToInt

class VibeboardService : InputMethodService(), LifecycleOwner, ViewModelStoreOwner, SavedStateRegistryOwner {

    private var speechRecognizer: SpeechRecognizer? = null
    private var isListening by mutableStateOf(false)
    private var onDeviceRecognitionAvailable by mutableStateOf(false)
    private val touchableRegion = Region()

    private val lifecycleRegistry = LifecycleRegistry(this)
    override val lifecycle: Lifecycle get() = lifecycleRegistry

    private val store = ViewModelStore()
    override val viewModelStore: ViewModelStore get() = store

    private val savedStateRegistryController = SavedStateRegistryController.create(this)
    override val savedStateRegistry: SavedStateRegistry get() = savedStateRegistryController.savedStateRegistry

    override fun onCreate() {
        super.onCreate()
        savedStateRegistryController.performRestore(null)
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_CREATE)
        setupSpeechRecognizer()
    }

    private fun setupSpeechRecognizer() {
        onDeviceRecognitionAvailable = SpeechRecognizer.isOnDeviceRecognitionAvailable(this)
        try {
            speechRecognizer = if (onDeviceRecognitionAvailable) {
                SpeechRecognizer.createOnDeviceSpeechRecognizer(this)
            } else {
                SpeechRecognizer.createSpeechRecognizer(this)
            }
        } catch (e: Exception) {
            reportError(e, mapOf("action" to "createSpeechRecognizer", "onDevice" to onDeviceRecognitionAvailable))
            return
        }

        speechRecognizer?.setRecognitionListener(object : RecognitionListener {
            override fun onReadyForSpeech(params: Bundle?) {}
            override fun onBeginningOfSpeech() {}
            override fun onRmsChanged(rmsdB: Float) {}
            override fun onBufferReceived(buffer: ByteArray?) {}
            override fun onEndOfSpeech() { isListening = false }
            override fun onError(error: Int) {
                // Client / no-match / timeout / cancelled are expected recognition outcomes.
                if (error !in EXPECTED_SPEECH_ERRORS) {
                    reportError(
                        Exception("SpeechRecognizer error: $error"),
                        mapOf("errorCode" to error)
                    )
                }
                isListening = false
                currentInputConnection?.finishComposingText()
            }
            override fun onResults(results: Bundle?) {
                results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)?.firstOrNull()?.let {
                    currentInputConnection?.commitText("$it ", 1)
                }
                isListening = false
            }
            override fun onPartialResults(partialResults: Bundle?) {
                partialResults?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)?.firstOrNull()?.let {
                    currentInputConnection?.setComposingText(it, 1)
                }
            }
            override fun onEvent(eventType: Int, params: Bundle?) {}
        })
    }

    override fun onEvaluateFullscreenMode(): Boolean = false

    override fun onComputeInsets(outInsets: Insets?) {
        super.onComputeInsets(outInsets)
        if (outInsets != null) {
            val height = window?.window?.decorView?.height ?: 0
            outInsets.contentTopInsets = height
            outInsets.visibleTopInsets = height
            outInsets.touchableInsets = Insets.TOUCHABLE_INSETS_REGION
            outInsets.touchableRegion.set(touchableRegion)
        }
    }

    override fun onCreateInputView(): View {
        return ComposeView(this).apply {
            window?.window?.decorView?.let { decor ->
                decor.setViewTreeLifecycleOwner(this@VibeboardService)
                decor.setViewTreeViewModelStoreOwner(this@VibeboardService)
                decor.setViewTreeSavedStateRegistryOwner(this@VibeboardService)
            }

            setContent {
                VibeboardTheme {
                    VibeboardKeyboard(
                        isListening = isListening,
                        onBackspaceRepeat = {
                            currentInputConnection?.deleteSurroundingText(1, 0)
                        },
                        onBackspaceTap = {
                            currentInputConnection?.finishComposingText()
                            currentInputConnection?.deleteSurroundingText(1, 0)
                        },
                        onToggleListening = { toggleListening() },
                        onEnterClick = { performEnter() },
                        onMoveCursor = { moveCursor(it) },
                        onSwitchKeyboard = {
                            try {
                                val imm = getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
                                // minSdk 28+: prefer non-deprecated IME API (token-based switch is deprecated).
                                if (!switchToPreviousInputMethod()) {
                                    imm.showInputMethodPicker()
                                }
                            } catch (e: Exception) {
                                reportError(e, mapOf("action" to "switchKeyboard"))
                            }
                        },
                        onHideKeyboard = { requestHideSelf(0) },
                        onLayoutCoordinates = { coords ->
                            val pos = coords.positionInWindow()
                            val rect = Rect(
                                pos.x.roundToInt(),
                                pos.y.roundToInt(),
                                (pos.x + coords.size.width).roundToInt(),
                                (pos.y + coords.size.height).roundToInt()
                            )
                            touchableRegion.set(rect)
                            window?.window?.decorView?.requestLayout()
                        }
                    )
                }
            }
        }
    }

    private fun moveCursor(direction: Int) {
        val keyEvent = if (direction > 0) KeyEvent.KEYCODE_DPAD_RIGHT else KeyEvent.KEYCODE_DPAD_LEFT
        currentInputConnection?.sendKeyEvent(KeyEvent(KeyEvent.ACTION_DOWN, keyEvent))
        currentInputConnection?.sendKeyEvent(KeyEvent(KeyEvent.ACTION_UP, keyEvent))
    }

    /**
     * Honor the focused field's IME action (Search, Go, Send, Done, Next) when present.
     * Raw KEYCODE_ENTER alone often does nothing useful on single-line action fields.
     */
    private fun performEnter() {
        val ic = currentInputConnection ?: return
        ic.finishComposingText()
        val info = currentInputEditorInfo
        val action = if (info != null) {
            resolveEnterEditorAction(info.imeOptions, info.actionId)
        } else {
            null
        }
        if (action != null && ic.performEditorAction(action)) {
            return
        }
        // Multiline / no action / performEditorAction declined → physical Enter.
        ic.sendKeyEvent(KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_ENTER))
        ic.sendKeyEvent(KeyEvent(KeyEvent.ACTION_UP, KeyEvent.KEYCODE_ENTER))
    }

    private fun toggleListening() {
        if (isListening) {
            try {
                speechRecognizer?.stopListening()
            } catch (e: Exception) {
                reportError(e, mapOf("action" to "stopListening"))
            }
            isListening = false
        } else {
            val recognizer = speechRecognizer
            if (recognizer == null) {
                // createSpeechRecognizer failed earlier; do not flip UI into a stuck "listening" state.
                reportError(
                    IllegalStateException("SpeechRecognizer not available"),
                    mapOf("action" to "startListening", "onDevice" to onDeviceRecognitionAvailable)
                )
                return
            }
            val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault())
                putExtra(RecognizerIntent.EXTRA_PREFER_OFFLINE, true)
                putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
                putExtra("android.speech.extra.ENABLE_FORMATTING", "punctuation")
            }
            try {
                recognizer.startListening(intent)
                isListening = true
            } catch (e: Exception) {
                reportError(e, mapOf("action" to "startListening"))
                isListening = false
            }
        }
    }

    /**
     * Drop an in-flight recognition session without waiting for final results.
     * Used when the IME is no longer visible so partial text is not committed late.
     */
    private fun abandonSpeechIfListening(reason: String) {
        if (!isListening) return
        try {
            speechRecognizer?.cancel()
        } catch (e: Exception) {
            reportError(e, mapOf("action" to "cancelSpeech", "reason" to reason))
        }
        isListening = false
        currentInputConnection?.finishComposingText()
    }

    override fun onWindowShown() {
        super.onWindowShown()
        window?.window?.let { win ->
            win.setBackgroundDrawable(ColorDrawable(android.graphics.Color.TRANSPARENT))
            win.setDimAmount(0f)
            @Suppress("DEPRECATION")
            win.navigationBarColor = android.graphics.Color.TRANSPARENT
            WindowCompat.setDecorFitsSystemWindows(win, false)
        }
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_START)
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_RESUME)
    }

    override fun onWindowHidden() {
        super.onWindowHidden()
        // Hide/switch-away must stop the mic; otherwise recognition can keep running and
        // commit composing text after the user has left the field.
        abandonSpeechIfListening("onWindowHidden")
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_PAUSE)
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_STOP)
    }

    override fun onDestroy() {
        abandonSpeechIfListening("onDestroy")
        super.onDestroy()
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_DESTROY)
        store.clear()
        try {
            speechRecognizer?.destroy()
        } catch (e: Exception) {
            reportError(e, mapOf("action" to "destroySpeechRecognizer"))
        }
        speechRecognizer = null
    }

    companion object {
        /** Recognition outcomes that are normal for voice input, not unexpected failures. */
        private val EXPECTED_SPEECH_ERRORS = setOf(
            SpeechRecognizer.ERROR_NO_MATCH,
            SpeechRecognizer.ERROR_SPEECH_TIMEOUT,
            SpeechRecognizer.ERROR_CLIENT,
        )
    }
}
