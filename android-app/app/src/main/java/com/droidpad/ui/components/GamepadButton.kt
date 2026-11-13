package com.droidpad.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.waitForUpOrCancellation
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.droidpad.ui.theme.*

/**
 * Controller button with independent multi-touch pointer tracking.
 * Supports arbitrary shapes (rounded rect or circle), colors, and dimensions.
 */
@Composable
fun GamepadButton(
    label: String,
    modifier: Modifier = Modifier,
    buttonColor: Color = DarkControlElement,
    pressedColor: Color? = null,
    textColor: Color = TextWhite,
    fontSize: TextUnit = 14.sp,
    fontWeight: FontWeight = FontWeight.Bold,
    shape: Shape = RoundedCornerShape(10.dp),
    onStateChange: (pressed: Boolean) -> Unit
) {
    var isPressed by remember { mutableStateOf(false) }
    val effectivePressedColor = pressedColor
        ?: if (buttonColor == AccentMintGreen) AccentMintGreenPressed else DarkControlElementPressed

    Box(
        modifier = modifier
            .clip(shape)
            .background(if (isPressed) effectivePressedColor else buttonColor)
            .pointerInput(Unit) {
                awaitEachGesture {
                    val down = awaitFirstDown(requireUnconsumed = false)
                    down.consume()
                    isPressed = true
                    onStateChange(true)

                    val up = waitForUpOrCancellation()
                    if (up != null) {
                        up.consume()
                    }
                    isPressed = false
                    onStateChange(false)
                }
            },
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label,
            color = textColor,
            fontSize = fontSize,
            fontWeight = fontWeight
        )
    }
}
