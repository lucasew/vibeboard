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

private const val DRAG_THRESHOLD = 40f

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

                                if (accumulatedDragX > DRAG_THRESHOLD) {
                                    onMoveCursor(1)
                                    accumulatedDragX = 0f
                                } else if (accumulatedDragX < -DRAG_THRESHOLD) {
                                    onMoveCursor(-1)
                                    accumulatedDragX = 0f
                                }

                                if (accumulatedDragY < -DRAG_THRESHOLD) {
                                    onSwitchKeyboard()
                                    accumulatedDragY = 0f
                                } else if (accumulatedDragY > DRAG_THRESHOLD * 1.5f) {
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
