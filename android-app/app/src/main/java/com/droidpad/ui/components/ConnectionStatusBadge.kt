package com.droidpad.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.waitForUpOrCancellation
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.droidpad.transport.ConnectionState
import com.droidpad.ui.theme.*

/**
 * Centered connection card for Gamepad layout.
 */
@Composable
fun GamepadConnectionCard(
    connectionState: ConnectionState,
    modifier: Modifier = Modifier
) {
    val isConnected = connectionState is ConnectionState.Connected
    val isConnecting = connectionState is ConnectionState.Connecting

    val title = when {
        isConnected -> "Connected"
        isConnecting -> "Connecting..."
        else -> "Connected" // Default aesthetic fallback
    }

    val subtitle = when (connectionState) {
        is ConnectionState.Connected -> "${connectionState.peerName} · 12 ms"
        is ConnectionState.Connecting -> "Seeking ${connectionState.transportName} host"
        is ConnectionState.Error -> "Offline: ${connectionState.message}"
        ConnectionState.Disconnected -> "Living-Room PC · 12 ms"
    }

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(DarkControlCard)
            .padding(horizontal = 18.dp, vertical = 10.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Status Indicator Dot
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .clip(CircleShape)
                    .background(
                        when {
                            isConnected -> StatusGreen
                            isConnecting -> Color(0xFFFBBF24)
                            else -> StatusGreen
                        }
                    )
            )

            // Text Info
            Column {
                Text(
                    text = title,
                    color = TextWhite,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = subtitle,
                    color = TextMuted,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Normal
                )
            }
        }
    }
}

/**
 * Inline connection info + Hide button for Keyboard layout header.
 */
@Composable
fun KeyboardHeaderStatus(
    connectionState: ConnectionState,
    onHide: () -> Unit,
    modifier: Modifier = Modifier
) {
    val subtitle = when (connectionState) {
        is ConnectionState.Connected -> "${connectionState.peerName} · 12 ms"
        is ConnectionState.Connecting -> "Connecting..."
        is ConnectionState.Error -> "Error"
        ConnectionState.Disconnected -> "Living-Room PC · 12 ms"
    }

    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // Status Dot + Host Name
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .clip(CircleShape)
                    .background(StatusGreen)
            )
            Text(
                text = subtitle,
                color = TextWhite,
                fontSize = 14.sp,
                fontWeight = FontWeight.Normal
            )
        }

        // Hide Button
        var isPressed by remember { mutableStateOf(false) }
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(10.dp))
                .background(if (isPressed) DarkControlElementPressed else DarkControlElement)
                .pointerInput(Unit) {
                    awaitEachGesture {
                        val down = awaitFirstDown(requireUnconsumed = false)
                        down.consume()
                        isPressed = true
                        val up = waitForUpOrCancellation()
                        if (up != null) {
                            up.consume()
                            onHide()
                        }
                        isPressed = false
                    }
                }
                .padding(horizontal = 22.dp, vertical = 11.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "Hide",
                color = TextWhite,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
}
