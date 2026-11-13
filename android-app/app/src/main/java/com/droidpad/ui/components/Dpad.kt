package com.droidpad.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.waitForUpOrCancellation
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.droidpad.ui.theme.*

/**
 * 4-Way D-Pad matching the design mockup in Image 1.
 * Features 4 separated tactile rounded directional buttons with arrow glyphs.
 */
@Composable
fun Dpad(
    modifier: Modifier = Modifier,
    size: Dp = 125.dp,
    onDirectionChange: (up: Boolean, down: Boolean, left: Boolean, right: Boolean) -> Unit
) {
    var up by remember { mutableStateOf(false) }
    var down by remember { mutableStateOf(false) }
    var left by remember { mutableStateOf(false) }
    var right by remember { mutableStateOf(false) }

    fun notify() {
        onDirectionChange(up, down, left, right)
    }

    val buttonSize = (size.value * 0.31f).dp

    Box(
        modifier = modifier.size(size),
        contentAlignment = Alignment.Center
    ) {
        // UP Direction Button
        DpadDirectionCap(
            glyph = "▲",
            isPressed = up,
            modifier = Modifier
                .align(Alignment.TopCenter)
                .size(buttonSize),
            onPressChange = {
                up = it
                notify()
            }
        )

        // DOWN Direction Button
        DpadDirectionCap(
            glyph = "▼",
            isPressed = down,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .size(buttonSize),
            onPressChange = {
                down = it
                notify()
            }
        )

        // LEFT Direction Button
        DpadDirectionCap(
            glyph = "◀",
            isPressed = left,
            modifier = Modifier
                .align(Alignment.CenterStart)
                .size(buttonSize),
            onPressChange = {
                left = it
                notify()
            }
        )

        // RIGHT Direction Button
        DpadDirectionCap(
            glyph = "▶",
            isPressed = right,
            modifier = Modifier
                .align(Alignment.CenterEnd)
                .size(buttonSize),
            onPressChange = {
                right = it
                notify()
            }
        )
    }
}

@Composable
private fun DpadDirectionCap(
    glyph: String,
    isPressed: Boolean,
    modifier: Modifier = Modifier,
    onPressChange: (Boolean) -> Unit
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .background(if (isPressed) DarkControlElementPressed else DarkControlElement)
            .pointerInput(Unit) {
                awaitEachGesture {
                    val down = awaitFirstDown(requireUnconsumed = false)
                    down.consume()
                    onPressChange(true)

                    val up = waitForUpOrCancellation()
                    if (up != null) {
                        up.consume()
                    }
                    onPressChange(false)
                }
            },
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = glyph,
            color = if (isPressed) AccentMintGreen else TextWhite,
            fontSize = 15.sp,
            fontWeight = FontWeight.Bold
        )
    }
}
