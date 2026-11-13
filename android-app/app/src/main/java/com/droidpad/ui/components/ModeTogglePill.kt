package com.droidpad.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.droidpad.ui.theme.*

enum class ControllerMode {
    GAMEPAD,
    KEYBOARD
}

/**
 * Segmented toggle pill matching the design header:
 * [ Gamepad | Keyboard ]
 */
@Composable
fun ModeTogglePill(
    selectedMode: ControllerMode,
    onModeChange: (ControllerMode) -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(DarkControlCard)
            .padding(4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Gamepad Tab
        val isGamepad = selectedMode == ControllerMode.GAMEPAD
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(9.dp))
                .background(if (isGamepad) AccentMintGreen else DarkControlCard)
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null
                ) { onModeChange(ControllerMode.GAMEPAD) }
                .padding(horizontal = 20.dp, vertical = 9.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "Gamepad",
                color = if (isGamepad) TextDark else TextWhite,
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold
            )
        }

        // Keyboard Tab
        val isKeyboard = selectedMode == ControllerMode.KEYBOARD
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(9.dp))
                .background(if (isKeyboard) AccentMintGreen else DarkControlCard)
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null
                ) { onModeChange(ControllerMode.KEYBOARD) }
                .padding(horizontal = 20.dp, vertical = 9.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "Keyboard",
                color = if (isKeyboard) TextDark else TextWhite,
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
}
