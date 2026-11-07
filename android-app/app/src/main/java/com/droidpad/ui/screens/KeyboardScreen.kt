package com.droidpad.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.droidpad.hid.KeyboardEncoder
import com.droidpad.service.DroidPadService

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun KeyboardScreen(
    service: DroidPadService?,
    onNavigateBack: () -> Unit
) {
    val encoder = remember { service?.keyboardEncoder ?: KeyboardEncoder() }

    var ctrlActive by remember { mutableStateOf(false) }
    var altActive by remember { mutableStateOf(false) }
    var shiftActive by remember { mutableStateOf(false) }
    var metaActive by remember { mutableStateOf(false) }

    fun sendKey(hidCode: Byte, isDown: Boolean) {
        val pkt = if (isDown) encoder.keyDown(hidCode) else encoder.keyUp(hidCode)
        service?.transportManager?.sendPacket(pkt)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Virtual Keyboard") },
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
                .padding(8.dp),
            verticalArrangement = Arrangement.SpaceEvenly
        ) {
            // Function Keys Row: F1-F12
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                listOf(
                    "ESC" to KeyboardEncoder.HID_ESC,
                    "F1" to KeyboardEncoder.HID_F1, "F2" to KeyboardEncoder.HID_F2,
                    "F3" to KeyboardEncoder.HID_F3, "F4" to KeyboardEncoder.HID_F4,
                    "F5" to KeyboardEncoder.HID_F5, "F6" to KeyboardEncoder.HID_F6,
                    "F7" to KeyboardEncoder.HID_F7, "F8" to KeyboardEncoder.HID_F8,
                    "F9" to KeyboardEncoder.HID_F9, "F10" to KeyboardEncoder.HID_F10,
                    "F11" to KeyboardEncoder.HID_F11, "F12" to KeyboardEncoder.HID_F12
                ).forEach { (label, code) ->
                    KeyCap(label = label, modifier = Modifier.weight(1f)) { down -> sendKey(code, down) }
                }
            }

            // Number Row: 1 - 0, Backspace
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                listOf(
                    "1" to KeyboardEncoder.HID_1, "2" to KeyboardEncoder.HID_2,
                    "3" to KeyboardEncoder.HID_3, "4" to KeyboardEncoder.HID_4,
                    "5" to KeyboardEncoder.HID_5, "6" to KeyboardEncoder.HID_6,
                    "7" to KeyboardEncoder.HID_7, "8" to KeyboardEncoder.HID_8,
                    "9" to KeyboardEncoder.HID_9, "0" to KeyboardEncoder.HID_0,
                    "-" to KeyboardEncoder.HID_MINUS, "=" to KeyboardEncoder.HID_EQUAL,
                    "⌫" to KeyboardEncoder.HID_BACKSPACE
                ).forEach { (label, code) ->
                    val weight = if (label == "⌫") 1.5f else 1f
                    KeyCap(label = label, modifier = Modifier.weight(weight)) { down -> sendKey(code, down) }
                }
            }

            // Row 1: Tab, Q - P
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                KeyCap(label = "TAB", modifier = Modifier.weight(1.3f)) { down -> sendKey(KeyboardEncoder.HID_TAB, down) }
                listOf(
                    "Q" to KeyboardEncoder.HID_Q, "W" to KeyboardEncoder.HID_W, "E" to KeyboardEncoder.HID_E,
                    "R" to KeyboardEncoder.HID_R, "T" to KeyboardEncoder.HID_T, "Y" to KeyboardEncoder.HID_Y,
                    "U" to KeyboardEncoder.HID_U, "I" to KeyboardEncoder.HID_I, "O" to KeyboardEncoder.HID_O,
                    "P" to KeyboardEncoder.HID_P
                ).forEach { (label, code) ->
                    KeyCap(label = label, modifier = Modifier.weight(1f)) { down -> sendKey(code, down) }
                }
            }

            // Row 2: Caps, A - L, Enter
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                listOf(
                    "A" to KeyboardEncoder.HID_A, "S" to KeyboardEncoder.HID_S, "D" to KeyboardEncoder.HID_D,
                    "F" to KeyboardEncoder.HID_F, "G" to KeyboardEncoder.HID_G, "H" to KeyboardEncoder.HID_H,
                    "J" to KeyboardEncoder.HID_J, "K" to KeyboardEncoder.HID_K, "L" to KeyboardEncoder.HID_L
                ).forEach { (label, code) ->
                    KeyCap(label = label, modifier = Modifier.weight(1f)) { down -> sendKey(code, down) }
                }
                KeyCap(label = "ENTER", modifier = Modifier.weight(1.8f), color = Color(0xFF107C10)) { down ->
                    sendKey(KeyboardEncoder.HID_ENTER, down)
                }
            }

            // Row 3: Shift, Z - M, Arrows
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                ModifierToggleKey(label = "SHIFT", isActive = shiftActive, modifier = Modifier.weight(1.5f)) {
                    shiftActive = it
                    service?.transportManager?.sendPacket(encoder.setModifier(KeyboardEncoder.MOD_LSHIFT, it))
                }
                listOf(
                    "Z" to KeyboardEncoder.HID_Z, "X" to KeyboardEncoder.HID_X, "C" to KeyboardEncoder.HID_C,
                    "V" to KeyboardEncoder.HID_V, "B" to KeyboardEncoder.HID_B, "N" to KeyboardEncoder.HID_N,
                    "M" to KeyboardEncoder.HID_M
                ).forEach { (label, code) ->
                    KeyCap(label = label, modifier = Modifier.weight(1f)) { down -> sendKey(code, down) }
                }
                KeyCap(label = "▲", modifier = Modifier.weight(1f)) { down -> sendKey(KeyboardEncoder.HID_UP, down) }
            }

            // Bottom Row: Ctrl, Super, Alt, Space, Arrows
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                ModifierToggleKey(label = "CTRL", isActive = ctrlActive, modifier = Modifier.weight(1.2f)) {
                    ctrlActive = it
                    service?.transportManager?.sendPacket(encoder.setModifier(KeyboardEncoder.MOD_LCTRL, it))
                }
                ModifierToggleKey(label = "WIN", isActive = metaActive, modifier = Modifier.weight(1.1f)) {
                    metaActive = it
                    service?.transportManager?.sendPacket(encoder.setModifier(KeyboardEncoder.MOD_LMETA, it))
                }
                ModifierToggleKey(label = "ALT", isActive = altActive, modifier = Modifier.weight(1.1f)) {
                    altActive = it
                    service?.transportManager?.sendPacket(encoder.setModifier(KeyboardEncoder.MOD_LALT, it))
                }
                KeyCap(label = "SPACE", modifier = Modifier.weight(4f)) { down ->
                    sendKey(KeyboardEncoder.HID_SPACE, down)
                }
                KeyCap(label = "◀", modifier = Modifier.weight(1f)) { down -> sendKey(KeyboardEncoder.HID_LEFT, down) }
                KeyCap(label = "▼", modifier = Modifier.weight(1f)) { down -> sendKey(KeyboardEncoder.HID_DOWN, down) }
                KeyCap(label = "▶", modifier = Modifier.weight(1f)) { down -> sendKey(KeyboardEncoder.HID_RIGHT, down) }
            }
        }
    }
}

@Composable
private fun KeyCap(
    label: String,
    modifier: Modifier = Modifier,
    color: Color = Color(0xFF2D2D2D),
    onKeyEvent: (isDown: Boolean) -> Unit
) {
    var isPressed by remember { mutableStateOf(false) }

    Box(
        modifier = modifier
            .height(52.dp)
            .clip(RoundedCornerShape(6.dp))
            .background(if (isPressed) Color(0xFF107C10) else color)
            .pointerInput(Unit) {
                detectTapGestures(
                    onPress = {
                        isPressed = true
                        onKeyEvent(true)
                        tryAwaitRelease()
                        isPressed = false
                        onKeyEvent(false)
                    }
                )
            },
        contentAlignment = Alignment.Center
    ) {
        Text(text = label, color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
private fun ModifierToggleKey(
    label: String,
    isActive: Boolean,
    modifier: Modifier = Modifier,
    onToggle: (Boolean) -> Unit
) {
    Box(
        modifier = modifier
            .height(52.dp)
            .clip(RoundedCornerShape(6.dp))
            .background(if (isActive) Color(0xFF107C10) else Color(0xFF242424))
            .pointerInput(isActive) {
                detectTapGestures(
                    onTap = {
                        onToggle(!isActive)
                    }
                )
            },
        contentAlignment = Alignment.Center
    ) {
        Text(text = label, color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
    }
}
