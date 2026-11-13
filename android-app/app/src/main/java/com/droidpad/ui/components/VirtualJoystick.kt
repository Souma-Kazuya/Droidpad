package com.droidpad.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.droidpad.ui.theme.*
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

/**
 * High-performance virtual analog joystick matching the design in the mockups.
 * Features an outer ring boundary, tactile thumbstick knob, and isolated
 * multi-touch pointer tracking that never interferes with adjacent buttons.
 */
@Composable
fun VirtualJoystick(
    modifier: Modifier = Modifier,
    size: Dp = 135.dp,
    onMove: (x: Float, y: Float) -> Unit
) {
    var thumbOffset by remember { mutableStateOf(Offset.Zero) }

    Box(
        modifier = modifier
            .size(size)
            .pointerInput(size) {
                val radius = size.toPx() / 2f
                val maxThumbDistance = radius * 0.65f
                val center = Offset(radius, radius)

                awaitEachGesture {
                    val down = awaitFirstDown(requireUnconsumed = false)
                    val initialDelta = down.position - center
                    val initialDist = sqrt(initialDelta.x * initialDelta.x + initialDelta.y * initialDelta.y)

                    // Only capture if touch originated within joystick base boundary
                    if (initialDist <= radius) {
                        down.consume()
                        val pointerId = down.id

                        val clamped = if (initialDist > maxThumbDistance) {
                            val angle = atan2(initialDelta.y, initialDelta.x)
                            Offset(cos(angle) * maxThumbDistance, sin(angle) * maxThumbDistance)
                        } else {
                            initialDelta
                        }
                        thumbOffset = clamped
                        onMove(clamped.x / maxThumbDistance, clamped.y / maxThumbDistance)

                        // Track continuous movement for this specific pointer
                        while (true) {
                            val event = awaitPointerEvent()
                            val pointerChange = event.changes.find { it.id == pointerId }

                            if (pointerChange == null || !pointerChange.pressed) {
                                // Pointer lifted or canceled
                                thumbOffset = Offset.Zero
                                onMove(0f, 0f)
                                break
                            }

                            pointerChange.consume()
                            val currentDelta = pointerChange.position - center
                            val currentDist = sqrt(currentDelta.x * currentDelta.x + currentDelta.y * currentDelta.y)

                            val newClamped = if (currentDist > maxThumbDistance) {
                                val angle = atan2(currentDelta.y, currentDelta.x)
                                Offset(cos(angle) * maxThumbDistance, sin(angle) * maxThumbDistance)
                            } else {
                                currentDelta
                            }
                            thumbOffset = newClamped
                            onMove(newClamped.x / maxThumbDistance, newClamped.y / maxThumbDistance)
                        }
                    }
                }
            }
    ) {
        Canvas(modifier = Modifier.matchParentSize()) {
            val center = Offset(size.toPx() / 2f, size.toPx() / 2f)
            val baseRadius = size.toPx() / 2f
            val thumbRadius = baseRadius * 0.38f

            // 1. Base translucent fill
            drawCircle(
                color = JoystickBaseColor,
                radius = baseRadius - 2.dp.toPx(),
                center = center
            )

            // 2. Outer ring border
            drawCircle(
                color = JoystickOutline,
                radius = baseRadius - 2.dp.toPx(),
                center = center,
                style = Stroke(width = 2.dp.toPx())
            )

            // 3. Thumbstick knob outer shadow/border
            val thumbCenter = center + thumbOffset
            drawCircle(
                color = JoystickThumbBorder,
                radius = thumbRadius,
                center = thumbCenter,
                style = Stroke(width = 2.dp.toPx())
            )

            // 4. Thumbstick knob solid body
            drawCircle(
                color = JoystickThumbColor,
                radius = thumbRadius - 1.dp.toPx(),
                center = thumbCenter
            )

            // 5. Thumbstick inner grip contour ring
            drawCircle(
                color = JoystickOutline,
                radius = thumbRadius * 0.5f,
                center = thumbCenter,
                style = Stroke(width = 1.5.dp.toPx())
            )
        }
    }
}
