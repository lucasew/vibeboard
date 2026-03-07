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

private object KeyboardDimensions {
    val Height = 80.dp
    val PaddingBottom = 0.dp
    val CornerRadius = 28.dp
    val ShadowElevation = 10.dp
    val RowSpacing = 12.dp
    val RowPaddingHorizontal = 6.dp
    val RowPaddingVertical = 2.dp
    val KeySize = 45.dp
    val IconSize = 21.dp
    val MicKeyWidth = 75.dp
    val MicIconSize = 27.dp
    const val DragThreshold = 40f
    const val DragHideThresholdMultiplier = 1.5f
}

private object KeyboardTiming {
    const val BackspaceRepeatDelay = 500L
    const val BackspaceRepeatInterval = 50L
}

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
            .height(KeyboardDimensions.Height)
            .background(Color.Transparent),
        contentAlignment = Alignment.BottomCenter
    ) {
        Surface(
            modifier = Modifier
                .padding(bottom = KeyboardDimensions.PaddingBottom)
                .onGloballyPositioned { coords ->
                    onLayoutCoordinates(coords)
                },
            // Formato de "Notch" invertido: arredondado em cima, reto embaixo
            shape = RoundedCornerShape(
                topStart = KeyboardDimensions.CornerRadius,
                topEnd = KeyboardDimensions.CornerRadius,
                bottomStart = 0.dp,
                bottomEnd = 0.dp
            ),
            color = MaterialTheme.colorScheme.primaryContainer,
            shadowElevation = KeyboardDimensions.ShadowElevation
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(KeyboardDimensions.RowSpacing),
                modifier = Modifier.padding(
                    horizontal = KeyboardDimensions.RowPaddingHorizontal,
                    vertical = KeyboardDimensions.RowPaddingVertical
                )
            ) {
                // Backspace
                var isPressingBackspace by remember { mutableStateOf(false) }
                LaunchedEffect(isPressingBackspace) {
                    if (isPressingBackspace) {
                        delay(KeyboardTiming.BackspaceRepeatDelay)
                        while (isPressingBackspace) {
                            onBackspaceRepeat()
                            delay(KeyboardTiming.BackspaceRepeatInterval)
                        }
                    }
                }

                Box(
                    modifier = Modifier
                        .size(KeyboardDimensions.KeySize)
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
                        modifier = Modifier.size(KeyboardDimensions.IconSize)
                    )
                }

                // Microfone (Gesto Inteligente de Troca)
                var accumulatedDragX by remember { mutableFloatStateOf(0f) }
                var accumulatedDragY by remember { mutableFloatStateOf(0f) }

                Box(
                    modifier = Modifier
                        .height(KeyboardDimensions.KeySize)
                        .width(KeyboardDimensions.MicKeyWidth)
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

                                if (accumulatedDragX > KeyboardDimensions.DragThreshold) {
                                    onMoveCursor(1)
                                    accumulatedDragX = 0f
                                } else if (accumulatedDragX < -KeyboardDimensions.DragThreshold) {
                                    onMoveCursor(-1)
                                    accumulatedDragX = 0f
                                }

                                if (accumulatedDragY < -KeyboardDimensions.DragThreshold) {
                                    onSwitchKeyboard()
                                    accumulatedDragY = 0f
                                } else if (accumulatedDragY > KeyboardDimensions.DragThreshold * KeyboardDimensions.DragHideThresholdMultiplier) {
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
                        modifier = Modifier.size(KeyboardDimensions.MicIconSize)
                    )
                }

                // Enter
                Box(
                    modifier = Modifier
                        .size(KeyboardDimensions.KeySize)
                        .clickable {
                            onEnterClick()
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.KeyboardReturn,
                        contentDescription = "Enter",
                        tint = MaterialTheme.colorScheme.onPrimaryContainer,
                        modifier = Modifier.size(KeyboardDimensions.IconSize)
                    )
                }
            }
        }
    }
}
