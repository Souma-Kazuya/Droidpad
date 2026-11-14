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
import com.droidpad.ui.layout.AbxyStyle
import com.droidpad.ui.theme.*

/**
 * ABXY diamond cluster with configurable layouts (Xbox, Nintendo Switch, or PlayStation symbols).
 * Features 4 circular action buttons with authentic colors and independent
 * multi-touch gesture detection to allow chords and rapid taps without interference.
 */
@Composable
fun ABXYDiamond(
    modifier: Modifier = Modifier,
    size: Dp = 140.dp,
    style: AbxyStyle = AbxyStyle.XBOX,
    onButtonPress: (button: String, pressed: Boolean) -> Unit
) {
    val buttonSize = (size.value * 0.32f).dp

    // Determine layout letters and physical Xbox mapping:
    // (topLetter, topMap, topColor, topPressedColor, ...)
    val (topLetter, topMap, topColor, topPressed) = when (style) {
        AbxyStyle.XBOX -> Quad("Y", "Y", ButtonColorY, Color(0xFFE5B82A))
        AbxyStyle.NINTENDO -> Quad("X", "X", ButtonColorX, Color(0xFF3B82F6))
        AbxyStyle.PLAYSTATION -> Quad("▲", "Y", ButtonColorA, AccentMintGreenPressed)
    }

    val (leftLetter, leftMap, leftColor, leftPressed) = when (style) {
        AbxyStyle.XBOX -> Quad("X", "X", ButtonColorX, Color(0xFF3B82F6))
        AbxyStyle.NINTENDO -> Quad("Y", "Y", ButtonColorY, Color(0xFFE5B82A))
        AbxyStyle.PLAYSTATION -> Quad("■", "X", Color(0xFFF472B6), Color(0xFFEC4899))
    }

    val (rightLetter, rightMap, rightColor, rightPressed) = when (style) {
        AbxyStyle.XBOX -> Quad("B", "B", ButtonColorB, Color(0xFFEF4444))
        AbxyStyle.NINTENDO -> Quad("A", "B", ButtonColorA, AccentMintGreenPressed)
        AbxyStyle.PLAYSTATION -> Quad("●", "B", ButtonColorB, Color(0xFFEF4444))
    }

    val (bottomLetter, bottomMap, bottomColor, bottomPressed) = when (style) {
        AbxyStyle.XBOX -> Quad("A", "A", ButtonColorA, AccentMintGreenPressed)
        AbxyStyle.NINTENDO -> Quad("B", "A", ButtonColorB, Color(0xFFEF4444))
        AbxyStyle.PLAYSTATION -> Quad("✖", "A", ButtonColorX, Color(0xFF3B82F6))
    }

    Box(
        modifier = modifier.size(size),
        contentAlignment = Alignment.Center
    ) {
        // Top Button
        DiamondCircleButton(
            letter = topLetter,
            color = topColor,
            pressedColor = topPressed,
            modifier = Modifier
                .align(Alignment.TopCenter)
                .size(buttonSize),
            onPressChange = { onButtonPress(topMap, it) }
        )

        // Left Button
        DiamondCircleButton(
            letter = leftLetter,
            color = leftColor,
            pressedColor = leftPressed,
            modifier = Modifier
                .align(Alignment.CenterStart)
                .size(buttonSize),
            onPressChange = { onButtonPress(leftMap, it) }
        )

        // Right Button
        DiamondCircleButton(
            letter = rightLetter,
            color = rightColor,
            pressedColor = rightPressed,
            modifier = Modifier
                .align(Alignment.CenterEnd)
                .size(buttonSize),
            onPressChange = { onButtonPress(rightMap, it) }
        )

        // Bottom Button
        DiamondCircleButton(
            letter = bottomLetter,
            color = bottomColor,
            pressedColor = bottomPressed,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .size(buttonSize),
            onPressChange = { onButtonPress(bottomMap, it) }
        )
    }
}

private data class Quad<A, B, C, D>(val first: A, val second: B, val third: C, val fourth: D)

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
