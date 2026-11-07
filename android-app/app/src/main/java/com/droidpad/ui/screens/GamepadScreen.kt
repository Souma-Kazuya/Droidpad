package com.droidpad.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.droidpad.hid.GamepadEncoder
import com.droidpad.service.DroidPadService
import com.droidpad.ui.components.*
import com.droidpad.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GamepadScreen(
    service: DroidPadService?,
    onNavigateBack: () -> Unit
) {
    val encoder = remember { service?.gamepadEncoder ?: GamepadEncoder() }

    fun sendButton(mask: Short, pressed: Boolean) {
        val pkt = encoder.setButton(mask, pressed)
        service?.transportManager?.sendPacket(pkt)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Xbox Gamepad") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface)
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .background(Color(0xFF141414))
                .padding(16.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // ── Top Row: Bumpers & Triggers ──────────────────────────
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Left Trigger & Bumper
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                    TriggerSlider(label = "LT") { v ->
                        service?.transportManager?.sendPacket(encoder.updateLeftTrigger(v))
                    }
                    GamepadButton(
                        label = "LB",
                        buttonColor = Color(0xFF333333),
                        size = 54.dp
                    ) { pressed -> sendButton(GamepadEncoder.BTN_LB, pressed) }
                }

                // Center: Select, Guide, Start
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
                    GamepadButton(
                        label = "⧉",
                        buttonColor = Color(0xFF242424),
                        size = 40.dp
                    ) { pressed -> sendButton(GamepadEncoder.BTN_SELECT, pressed) }

                    GamepadButton(
                        label = "X",
                        buttonColor = XboxGreen,
                        size = 48.dp
                    ) { pressed -> sendButton(GamepadEncoder.BTN_GUIDE, pressed) }

                    GamepadButton(
                        label = "☰",
                        buttonColor = Color(0xFF242424),
                        size = 40.dp
                    ) { pressed -> sendButton(GamepadEncoder.BTN_START, pressed) }
                }

                // Right Bumper & Trigger
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                    GamepadButton(
                        label = "RB",
                        buttonColor = Color(0xFF333333),
                        size = 54.dp
                    ) { pressed -> sendButton(GamepadEncoder.BTN_RB, pressed) }
                    TriggerSlider(label = "RT") { v ->
                        service?.transportManager?.sendPacket(encoder.updateRightTrigger(v))
                    }
                }
            }

            // ── Middle / Main Controls: Left Stick, D-pad, Right Stick, ABXY ─────
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Left Column: Left Stick & D-pad
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    VirtualJoystick(
                        size = 130.dp,
                        onMove = { x, y ->
                            service?.transportManager?.sendPacket(encoder.updateLeftStick(x, y))
                        }
                    )

                    Dpad { up, down, left, right ->
                        encoder.setButton(GamepadEncoder.DPAD_UP, up)
                        encoder.setButton(GamepadEncoder.DPAD_DOWN, down)
                        encoder.setButton(GamepadEncoder.DPAD_LEFT, left)
                        val pkt = encoder.setButton(GamepadEncoder.DPAD_RIGHT, right)
                        service?.transportManager?.sendPacket(pkt)
                    }
                }

                // Right Column: ABXY Diamond & Right Stick
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // ABXY Diamond Cluster
                    Box(modifier = Modifier.size(150.dp), contentAlignment = Alignment.Center) {
                        // Y Button (Top)
                        GamepadButton(
                            label = "Y",
                            buttonColor = ButtonY,
                            modifier = Modifier.align(Alignment.TopCenter)
                        ) { pressed -> sendButton(GamepadEncoder.BTN_Y, pressed) }

                        // A Button (Bottom)
                        GamepadButton(
                            label = "A",
                            buttonColor = ButtonA,
                            modifier = Modifier.align(Alignment.BottomCenter)
                        ) { pressed -> sendButton(GamepadEncoder.BTN_A, pressed) }

                        // X Button (Left)
                        GamepadButton(
                            label = "X",
                            buttonColor = ButtonX,
                            modifier = Modifier.align(Alignment.CenterStart)
                        ) { pressed -> sendButton(GamepadEncoder.BTN_X, pressed) }

                        // B Button (Right)
                        GamepadButton(
                            label = "B",
                            buttonColor = ButtonB,
                            modifier = Modifier.align(Alignment.CenterEnd)
                        ) { pressed -> sendButton(GamepadEncoder.BTN_B, pressed) }
                    }

                    VirtualJoystick(
                        size = 130.dp,
                        onMove = { x, y ->
                            service?.transportManager?.sendPacket(encoder.updateRightStick(x, y))
                        }
                    )
                }
            }
        }
    }
}
