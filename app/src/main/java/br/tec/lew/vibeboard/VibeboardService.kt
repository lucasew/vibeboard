package br.tec.lew.vibeboard

import android.content.Intent
import android.inputmethodservice.InputMethodService
import android.os.Bundle
import android.provider.Settings
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.view.KeyEvent
import android.view.View
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Backspace
import androidx.compose.material.icons.automirrored.filled.KeyboardReturn
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
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
import br.tec.lew.vibeboard.ui.theme.VibeboardTheme
import kotlinx.coroutines.delay
import java.util.Locale

class VibeboardService : InputMethodService(), LifecycleOwner, ViewModelStoreOwner, SavedStateRegistryOwner {

    private var speechRecognizer: SpeechRecognizer? = null
    private var isListening by mutableStateOf(false)
    private var onDeviceRecognitionAvailable by mutableStateOf(false)

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
        speechRecognizer = if (onDeviceRecognitionAvailable) {
            SpeechRecognizer.createOnDeviceSpeechRecognizer(this)
        } else {
            SpeechRecognizer.createSpeechRecognizer(this)
        }

        speechRecognizer?.setRecognitionListener(object : RecognitionListener {
            override fun onReadyForSpeech(params: Bundle?) {}
            override fun onBeginningOfSpeech() {}
            override fun onRmsChanged(rmsdB: Float) {}
            override fun onBufferReceived(buffer: ByteArray?) {}
            override fun onEndOfSpeech() { isListening = false }
            override fun onError(error: Int) { 
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

    override fun onCreateInputView(): View {
        return ComposeView(this).apply {
            window?.window?.decorView?.let { decor ->
                decor.setViewTreeLifecycleOwner(this@VibeboardService)
                decor.setViewTreeViewModelStoreOwner(this@VibeboardService)
                decor.setViewTreeSavedStateRegistryOwner(this@VibeboardService)
            }

            setContent {
                VibeboardTheme {
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(60.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant
                    ) {
                        if (onDeviceRecognitionAvailable) {
                            Row(
                                modifier = Modifier.fillMaxSize().padding(horizontal = 4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                var isPressingBackspace by remember { mutableStateOf(false) }
                                LaunchedEffect(isPressingBackspace) {
                                    if (isPressingBackspace) {
                                        delay(500)
                                        while (isPressingBackspace) {
                                            currentInputConnection?.deleteSurroundingText(1, 0)
                                            delay(50)
                                        }
                                    }
                                }

                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .pointerInput(Unit) {
                                            detectTapGestures(
                                                onPress = {
                                                    isPressingBackspace = true
                                                    try { awaitRelease() } finally { isPressingBackspace = false }
                                                },
                                                onTap = { 
                                                    currentInputConnection?.finishComposingText()
                                                    currentInputConnection?.deleteSurroundingText(1, 0) 
                                                }
                                            )
                                        },
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.AutoMirrored.Filled.Backspace,
                                        contentDescription = "Backspace",
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }

                                var accumulatedDrag by remember { mutableFloatStateOf(0f) }
                                val dragThreshold = 40f

                                Box(
                                    modifier = Modifier
                                        .weight(2f)
                                        .pointerInput(Unit) {
                                            detectHorizontalDragGestures(
                                                onDragEnd = { accumulatedDrag = 0f },
                                                onDragCancel = { accumulatedDrag = 0f }
                                            ) { change, dragAmount ->
                                                change.consume()
                                                accumulatedDrag += dragAmount
                                                if (accumulatedDrag > dragThreshold) {
                                                    moveCursor(1)
                                                    accumulatedDrag = 0f
                                                } else if (accumulatedDrag < -dragThreshold) {
                                                    moveCursor(-1)
                                                    accumulatedDrag = 0f
                                                }
                                            }
                                        },
                                    contentAlignment = Alignment.Center
                                ) {
                                    IconButton(onClick = { toggleListening() }) {
                                        Icon(
                                            imageVector = Icons.Default.Mic,
                                            contentDescription = "Record",
                                            tint = if (isListening) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
                                        )
                                    }
                                }

                                IconButton(
                                    onClick = {
                                        currentInputConnection?.finishComposingText()
                                        currentInputConnection?.sendKeyEvent(KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_ENTER))
                                        currentInputConnection?.sendKeyEvent(KeyEvent(KeyEvent.ACTION_UP, KeyEvent.KEYCODE_ENTER))
                                    },
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Icon(
                                        imageVector = Icons.AutoMirrored.Filled.KeyboardReturn,
                                        contentDescription = "Enter",
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        } else {
                            Box(
                                modifier = Modifier.fillMaxSize().clickable {
                                    val intent = Intent(Settings.ACTION_VOICE_INPUT_SETTINGS).apply {
                                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                    }
                                    startActivity(intent)
                                },
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "Offline voice package needed",
                                    style = MaterialTheme.typography.labelMedium
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    private fun moveCursor(direction: Int) {
        val keyEvent = if (direction > 0) KeyEvent.KEYCODE_DPAD_RIGHT else KeyEvent.KEYCODE_DPAD_LEFT
        currentInputConnection?.sendKeyEvent(KeyEvent(KeyEvent.ACTION_DOWN, keyEvent))
        currentInputConnection?.sendKeyEvent(KeyEvent(KeyEvent.ACTION_UP, keyEvent))
    }

    private fun toggleListening() {
        if (isListening) {
            speechRecognizer?.stopListening()
            isListening = false
        } else {
            val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault())
                putExtra(RecognizerIntent.EXTRA_PREFER_OFFLINE, true)
                putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
                putExtra("android.speech.extra.ENABLE_FORMATTING", "punctuation")
            }
            speechRecognizer?.startListening(intent)
            isListening = true
        }
    }

    override fun onWindowShown() {
        super.onWindowShown()
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_START)
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_RESUME)
    }

    override fun onWindowHidden() {
        super.onWindowHidden()
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_PAUSE)
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_STOP)
    }

    override fun onDestroy() {
        super.onDestroy()
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_DESTROY)
        speechRecognizer?.destroy()
    }
}
