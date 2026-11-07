package com.droidpad.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.Icon
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp

@Composable
fun Dpad(
    modifier: Modifier = Modifier,
    onDirectionChange: (up: Boolean, down: Boolean, left: Boolean, right: Boolean) -> Unit
) {
    var up by remember { mutableStateOf(false) }
    var down by remember { mutableStateOf(false) }
    var left by remember { mutableStateOf(false) }
    var right by remember { mutableStateOf(false) }

    fun notify() {
        onDirectionChange(up, down, left, right)
    }

    Box(
        modifier = modifier.size(130.dp),
        contentAlignment = Alignment.Center
    ) {
        // UP
        DpadButton(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .size(42.dp),
            isPressed = up,
            onPressChange = {
                up = it
                notify()
            }
        ) {
            Icon(Icons.Default.KeyboardArrowUp, contentDescription = "Up", tint = Color.White)
        }

        // DOWN
        DpadButton(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .size(42.dp),
            isPressed = down,
            onPressChange = {
                down = it
                notify()
            }
        ) {
            Icon(Icons.Default.KeyboardArrowDown, contentDescription = "Down", tint = Color.White)
        }

        // LEFT
        DpadButton(
            modifier = Modifier
                .align(Alignment.CenterStart)
                .size(42.dp),
            isPressed = left,
            onPressChange = {
                left = it
                notify()
            }
        ) {
            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Left", tint = Color.White)
        }

        // RIGHT
        DpadButton(
            modifier = Modifier
                .align(Alignment.CenterEnd)
                .size(42.dp),
            isPressed = right,
            onPressChange = {
                right = it
                notify()
            }
        ) {
            Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = "Right", tint = Color.White)
        }
    }
}

@Composable
private fun DpadButton(
    modifier: Modifier = Modifier,
    isPressed: Boolean,
    onPressChange: (Boolean) -> Unit,
    content: @Composable () -> Unit
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .background(if (isPressed) Color(0xFF107C10) else Color(0xFF2D2D2D))
            .pointerInput(Unit) {
                detectTapGestures(
                    onPress = {
                        onPressChange(true)
                        tryAwaitRelease()
                        onPressChange(false)
                    }
                )
            },
        contentAlignment = Alignment.Center
    ) {
        content()
    }
}
