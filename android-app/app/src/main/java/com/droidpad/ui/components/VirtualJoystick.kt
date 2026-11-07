package com.droidpad.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

@Composable
fun VirtualJoystick(
    modifier: Modifier = Modifier,
    size: Dp = 140.dp,
    stickColor: Color = Color(0xFF107C10),
    baseColor: Color = Color(0xFF2A2A2A),
    onMove: (x: Float, y: Float) -> Unit
) {
    var thumbOffset by remember { mutableStateOf(Offset.Zero) }

    Box(
        modifier = modifier
            .size(size)
            .pointerInput(Unit) {
                val radius = (size.toPx() / 2f)
                val maxThumbDistance = radius * 0.7f

                detectDragGestures(
                    onDragStart = { offset ->
                        val center = Offset(radius, radius)
                        val delta = offset - center
                        val dist = sqrt(delta.x * delta.x + delta.y * delta.y)
                        val clampedOffset = if (dist > maxThumbDistance) {
                            val angle = atan2(delta.y, delta.x)
                            Offset(cos(angle) * maxThumbDistance, sin(angle) * maxThumbDistance)
                        } else {
                            delta
                        }
                        thumbOffset = clampedOffset
                        onMove(clampedOffset.x / maxThumbDistance, clampedOffset.y / maxThumbDistance)
                    },
                    onDrag = { change, dragAmount ->
                        change.consume()
                        val newOffset = thumbOffset + dragAmount
                        val dist = sqrt(newOffset.x * newOffset.x + newOffset.y * newOffset.y)
                        val clampedOffset = if (dist > maxThumbDistance) {
                            val angle = atan2(newOffset.y, newOffset.x)
                            Offset(cos(angle) * maxThumbDistance, sin(angle) * maxThumbDistance)
                        } else {
                            newOffset
                        }
                        thumbOffset = clampedOffset
                        onMove(clampedOffset.x / maxThumbDistance, clampedOffset.y / maxThumbDistance)
                    },
                    onDragEnd = {
                        thumbOffset = Offset.Zero
                        onMove(0f, 0f)
                    },
                    onDragCancel = {
                        thumbOffset = Offset.Zero
                        onMove(0f, 0f)
                    }
                )
            }
    ) {
        Canvas(modifier = Modifier.matchParentSize()) {
            val center = Offset(size.toPx() / 2f, size.toPx() / 2f)
            val baseRadius = size.toPx() / 2f
            val thumbRadius = baseRadius * 0.35f

            // Outer circle base
            drawCircle(
                color = baseColor,
                radius = baseRadius,
                center = center
            )

            // Inner stick thumb
            drawCircle(
                color = stickColor,
                radius = thumbRadius,
                center = center + thumbOffset
            )
        }
    }
}
