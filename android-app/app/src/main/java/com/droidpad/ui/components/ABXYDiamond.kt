package com.droidpad.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.waitForUpOrCancellation
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.droidpad.ui.theme.*

/**
 * Xbox-style ABXY diamond cluster matching Image 1.
 * Features 4 circular action buttons with authentic colors and independent
 * multi-touch gesture detection to allow chords and rapid taps without interference.
 */
@Composable
fun ABXYDiamond(
    modifier: Modifier = Modifier,
    size: Dp = 140.dp,
    onButtonPress: (button: String, pressed: Boolean) -> Unit
) {
    val buttonSize = (size.value * 0.32f).dp

    Box(
        modifier = modifier.size(size),
        contentAlignment = Alignment.Center
    ) {
        // Y Button (Top, Yellow)
        DiamondCircleButton(
            letter = "Y",
            color = ButtonColorY,
            pressedColor = Color(0xFFE5B82A),
            modifier = Modifier
                .align(Alignment.TopCenter)
                .size(buttonSize),
            onPressChange = { onButtonPress("Y", it) }
        )

        // X Button (Left, Blue)
        DiamondCircleButton(
            letter = "X",
            color = ButtonColorX,
            pressedColor = Color(0xFF3B82F6),
            modifier = Modifier
                .align(Alignment.CenterStart)
                .size(buttonSize),
            onPressChange = { onButtonPress("X", it) }
        )

        // B Button (Right, Red)
        DiamondCircleButton(
            letter = "B",
            color = ButtonColorB,
            pressedColor = Color(0xFFEF4444),
            modifier = Modifier
                .align(Alignment.CenterEnd)
                .size(buttonSize),
            onPressChange = { onButtonPress("B", it) }
        )

        // A Button (Bottom, Green)
        DiamondCircleButton(
            letter = "A",
            color = ButtonColorA,
            pressedColor = AccentMintGreenPressed,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .size(buttonSize),
            onPressChange = { onButtonPress("A", it) }
        )
    }
}

@Composable
private fun DiamondCircleButton(
    letter: String,
    color: Color,
    pressedColor: Color,
    modifier: Modifier = Modifier,
    onPressChange: (Boolean) -> Unit
) {
    var isPressed by remember { mutableStateOf(false) }

    Box(
        modifier = modifier
            .clip(CircleShape)
            .background(if (isPressed) pressedColor else color)
            .pointerInput(Unit) {
                awaitEachGesture {
                    val down = awaitFirstDown(requireUnconsumed = false)
                    down.consume()
                    isPressed = true
                    onPressChange(true)

                    val up = waitForUpOrCancellation()
                    if (up != null) {
                        up.consume()
                    }
                    isPressed = false
                    onPressChange(false)
                }
            },
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = letter,
            color = TextDark,
            fontSize = 17.sp,
            fontWeight = FontWeight.Bold
        )
    }
}
