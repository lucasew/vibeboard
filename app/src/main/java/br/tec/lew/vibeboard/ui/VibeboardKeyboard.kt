package br.tec.lew.vibeboard.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/** Delay before hold-to-repeat starts; short presses only fire a single delete. */
private const val BACKSPACE_REPEAT_INITIAL_DELAY_MS = 500L
/** Interval between repeated deletes while backspace is held. */
private const val BACKSPACE_REPEAT_INTERVAL_MS = 50L

/**
 * Drag distance (dp) on the mic control before cursor / switch-keyboard actions fire.
 * Kept in dp so sensitivity is stable across screen densities (raw px was too tight on xxxhdpi).
 */
private val MIC_DRAG_THRESHOLD_DP = 40.dp

/**
 * Downward hide uses a higher threshold than left/right/up so accidental hides are less common.
 */
private const val MIC_HIDE_DRAG_MULTIPLIER = 1.5f

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
    val density = LocalDensity.current
    val dragThresholdPx = with(density) { MIC_DRAG_THRESHOLD_DP.toPx() }
    val hideDragThresholdPx = dragThresholdPx * MIC_HIDE_DRAG_MULTIPLIER

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
            // Inverted notch: rounded top, flat bottom against the nav bar.
            shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp, bottomStart = 0.dp, bottomEnd = 0.dp),
            color = MaterialTheme.colorScheme.primaryContainer,
            shadowElevation = 10.dp
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
            ) {
                // Backspace: short press deletes once; hold repeats after initial delay.
                // Must not also fire onTap after a hold — that caused an extra delete on release.
                Box(
                    modifier = Modifier
                        .size(45.dp)
                        .pointerInput(onBackspaceTap, onBackspaceRepeat) {
                            detectTapGestures(
                                onPress = {
                                    // coroutineScope gives a CoroutineScope inside PressGestureScope.
                                    coroutineScope {
                                        var repeated = false
                                        val repeatJob = launch {
                                            delay(BACKSPACE_REPEAT_INITIAL_DELAY_MS)
                                            repeated = true
                                            while (true) {
                                                onBackspaceRepeat()
                                                delay(BACKSPACE_REPEAT_INTERVAL_MS)
                                            }
                                        }
                                        try {
                                            val released = tryAwaitRelease()
                                            if (released && !repeated) {
                                                onBackspaceTap()
                                            }
                                        } finally {
                                            repeatJob.cancel()
                                        }
                                    }
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

                // Mic: tap toggles listening; drag left/right moves cursor, up switches IME, down hides.
                // Single pointer handler so drag and tap cannot both fire (clickable + detectDragGestures did).
                Box(
                    modifier = Modifier
                        .height(45.dp)
                        .width(75.dp)
                        .pointerInput(
                            dragThresholdPx,
                            hideDragThresholdPx,
                            onToggleListening,
                            onMoveCursor,
                            onSwitchKeyboard,
                            onHideKeyboard
                        ) {
                            awaitEachGesture {
                                val down = awaitFirstDown()
                                var accumulatedDragX = 0f
                                var accumulatedDragY = 0f
                                var didSwipeAction = false

                                while (true) {
                                    val event = awaitPointerEvent()
                                    val change = event.changes.firstOrNull { it.id == down.id } ?: break

                                    if (!change.pressed) {
                                        if (!didSwipeAction) {
                                            onToggleListening()
                                        }
                                        break
                                    }

                                    val dragAmount = change.position - change.previousPosition
                                    change.consume()
                                    accumulatedDragX += dragAmount.x
                                    accumulatedDragY += dragAmount.y

                                    if (accumulatedDragX > dragThresholdPx) {
                                        onMoveCursor(1)
                                        accumulatedDragX = 0f
                                        didSwipeAction = true
                                    } else if (accumulatedDragX < -dragThresholdPx) {
                                        onMoveCursor(-1)
                                        accumulatedDragX = 0f
                                        didSwipeAction = true
                                    }

                                    if (accumulatedDragY < -dragThresholdPx) {
                                        onSwitchKeyboard()
                                        accumulatedDragY = 0f
                                        didSwipeAction = true
                                    } else if (accumulatedDragY > hideDragThresholdPx) {
                                        onHideKeyboard()
                                        accumulatedDragY = 0f
                                        didSwipeAction = true
                                    }
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
