package br.tec.lew.vibeboard.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Backspace
import androidx.compose.material.icons.automirrored.filled.KeyboardReturn
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay

/**
 * Renders the main overlay UI for the voice keyboard.
 *
 * This component handles the visual presentation (the "notch" shape that sits at the
 * bottom of the screen) and captures specialized gestures:
 * - Backspace supports single tap for single deletion, and long press for rapid repeated deletion.
 * - The Microphone button can be dragged to perform quick actions: left/right to move the text cursor,
 *   down to switch keyboards, and far down to dismiss the keyboard entirely.
 *
 * It uses a transparent background to ensure that the system navigation bar doesn't obstruct
 * the underlying application content.
 *
 * @param isListening Indicates whether the microphone is actively recording.
 * @param onBackspaceRepeat Triggered repeatedly when the backspace button is held down.
 * @param onBackspaceTap Triggered on a simple tap of the backspace button.
 * @param onToggleListening Triggered to start/stop the voice recognition.
 * @param onEnterClick Triggered when the enter button is pressed.
 * @param onMoveCursor Triggered with direction offset (1/-1) when a swipe is registered over the mic.
 * @param onSwitchKeyboard Triggered on a short downward swipe on the mic to invoke the system keyboard picker.
 * @param onHideKeyboard Triggered on a long downward swipe on the mic to close the overlay.
 * @param onLayoutCoordinates Emits layout position updates so the host service can constrain touches just to the visible bounds.
 */
@Composable
fun VibeboardKeyboard(
    isListening: Boolean,
    onBackspaceRepeat: () -> Unit,
    onBackspaceTap: () -> Unit,
    onToggleListening: () -> Unit,
    onEnterClick: () -> Unit,
    onMoveCursor: (Int) -> Unit,
    onSwitchKeyboard: () -> Unit,
    onHideKeyboard: () -> Unit,
    onLayoutCoordinates: (LayoutCoordinates) -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(80.dp)
            .background(Color.Transparent),
        contentAlignment = Alignment.BottomCenter
    ) {
        Surface(
            modifier = Modifier
                .padding(bottom = 0.dp)
                .onGloballyPositioned { coords ->
                    onLayoutCoordinates(coords)
                },
            // Formato de "Notch" invertido: arredondado em cima, reto embaixo
            shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp, bottomStart = 0.dp, bottomEnd = 0.dp),
            color = MaterialTheme.colorScheme.primaryContainer,
            shadowElevation = 10.dp
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
            ) {
                // Backspace
                var isPressingBackspace by remember { mutableStateOf(false) }
                LaunchedEffect(isPressingBackspace) {
                    if (isPressingBackspace) {
                        delay(500)
                        while (isPressingBackspace) {
                            onBackspaceRepeat()
                            delay(50)
                        }
                    }
                }

                Box(
                    modifier = Modifier
                        .size(45.dp)
                        .pointerInput(Unit) {
                            detectTapGestures(
                                onPress = {
                                    isPressingBackspace = true
                                    try { awaitRelease() } finally { isPressingBackspace = false }
                                },
                                onTap = {
                                    onBackspaceTap()
                                }
                            )
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.Backspace,
                        contentDescription = "Backspace",
                        tint = MaterialTheme.colorScheme.onPrimaryContainer,
                        modifier = Modifier.size(21.dp)
                    )
                }

                // Microfone (Gesto Inteligente de Troca)
                var accumulatedDragX by remember { mutableFloatStateOf(0f) }
                var accumulatedDragY by remember { mutableFloatStateOf(0f) }
                val dragThreshold = 40f

                Box(
                    modifier = Modifier
                        .height(45.dp)
                        .width(75.dp)
                        .clickable { onToggleListening() }
                        .pointerInput(Unit) {
                            detectDragGestures(
                                onDragEnd = {
                                    accumulatedDragX = 0f
                                    accumulatedDragY = 0f
                                },
                                onDragCancel = {
                                    accumulatedDragX = 0f
                                    accumulatedDragY = 0f
                                }
                            ) { change, dragAmount ->
                                change.consume()
                                accumulatedDragX += dragAmount.x
                                accumulatedDragY += dragAmount.y

                                if (accumulatedDragX > dragThreshold) {
                                    onMoveCursor(1)
                                    accumulatedDragX = 0f
                                } else if (accumulatedDragX < -dragThreshold) {
                                    onMoveCursor(-1)
                                    accumulatedDragX = 0f
                                }

                                if (accumulatedDragY < -dragThreshold) {
                                    onSwitchKeyboard()
                                    accumulatedDragY = 0f
                                } else if (accumulatedDragY > dragThreshold * 1.5f) {
                                    onHideKeyboard()
                                    accumulatedDragY = 0f
                                }
                            }
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Mic,
                        contentDescription = "Record",
                        tint = if (isListening) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onPrimaryContainer,
                        modifier = Modifier.size(27.dp)
                    )
                }

                // Enter
                Box(
                    modifier = Modifier
                        .size(45.dp)
                        .clickable {
                            onEnterClick()
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.KeyboardReturn,
                        contentDescription = "Enter",
                        tint = MaterialTheme.colorScheme.onPrimaryContainer,
                        modifier = Modifier.size(21.dp)
                    )
                }
            }
        }
    }
}
