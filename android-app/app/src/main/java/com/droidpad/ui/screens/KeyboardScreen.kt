package com.droidpad.ui.screens

import android.app.Activity
import android.content.pm.ActivityInfo
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import com.droidpad.hid.KeyboardEncoder
import com.droidpad.service.DroidPadService
import com.droidpad.transport.ConnectionState
import com.droidpad.ui.components.ControllerMode
import com.droidpad.ui.components.KeyboardHeaderStatus
import com.droidpad.ui.components.ModeTogglePill
import com.droidpad.ui.theme.*

/**
 * Virtual keyboard layout adhering strictly to Image 2.
 * Features 5 perfectly weighted key rows with unified 14.6f row widths,
 * independent multi-touch pointer tracking to prevent input interference,
 * and context-only presentation.
 */
@Composable
fun KeyboardScreen(
    service: DroidPadService?,
    onNavigateToGamepad: () -> Unit,
    onNavigateBack: () -> Unit
) {
    val context = LocalContext.current
    val activity = context as? Activity

    // Immersive Landscape Effect: Locks to landscape and hides system bars
    // so only the keyboard context is displayed and keys don't interfere with system UI.
    DisposableEffect(Unit) {
        activity?.let { act ->
            val prevOrientation = act.requestedOrientation
            act.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE
            val window = act.window
            val insetsController = WindowCompat.getInsetsController(window, window.decorView)
            val prevBehavior = insetsController.systemBarsBehavior
            insetsController.systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
            insetsController.hide(WindowInsetsCompat.Type.systemBars())

            onDispose {
                act.requestedOrientation = prevOrientation
                insetsController.systemBarsBehavior = prevBehavior
                insetsController.show(WindowInsetsCompat.Type.systemBars())
            }
        } ?: onDispose {}
    }

    val encoder = remember { service?.keyboardEncoder ?: KeyboardEncoder() }
    val connectionState by service?.transportManager?.connectionState?.collectAsState()
        ?: remember { mutableStateOf(ConnectionState.Disconnected) }

    var ctrlActive by remember { mutableStateOf(false) }
    var altActive by remember { mutableStateOf(false) }
    var shiftActive by remember { mutableStateOf(false) }
    var winActive by remember { mutableStateOf(false) }
    var capsActive by remember { mutableStateOf(false) }

    fun sendKey(hidCode: Byte, isDown: Boolean) {
        val pkt = if (isDown) encoder.keyDown(hidCode) else encoder.keyUp(hidCode)
        service?.transportManager?.sendPacket(pkt)
    }

    fun toggleModifier(mask: Byte, current: Boolean, onUpdate: (Boolean) -> Unit) {
        val next = !current
        onUpdate(next)
        val pkt = encoder.setModifier(mask, next)
        service?.transportManager?.sendPacket(pkt)
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(DarkSlateBackground)
            .windowInsetsPadding(WindowInsets.safeDrawing)
            .padding(horizontal = 14.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        // ── 1. Top Bar: Mode Toggle (Left) | Host Info + Hide Button (Right) ─
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 6.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            ModeTogglePill(
                selectedMode = ControllerMode.KEYBOARD,
                onModeChange = { mode ->
                    if (mode == ControllerMode.GAMEPAD) {
                        onNavigateToGamepad()
                    }
                }
            )

            KeyboardHeaderStatus(
                connectionState = connectionState,
                onHide = onNavigateBack
            )
        }

        // ── 2. Keyboard Grid (5 Proportional Rows) ───────────────────────────
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            verticalArrangement = Arrangement.spacedBy(5.dp)
        ) {
            // ── Row 1: Esc, 1-0, -, =, Bksp (Sum = 14.6f) ─────────────────────
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                StandardKey("Esc", KeyboardEncoder.HID_ESC, Modifier.weight(1f)) { c, d -> sendKey(c, d) }
                StandardKey("1", KeyboardEncoder.HID_1, Modifier.weight(1f)) { c, d -> sendKey(c, d) }
                StandardKey("2", KeyboardEncoder.HID_2, Modifier.weight(1f)) { c, d -> sendKey(c, d) }
                StandardKey("3", KeyboardEncoder.HID_3, Modifier.weight(1f)) { c, d -> sendKey(c, d) }
                StandardKey("4", KeyboardEncoder.HID_4, Modifier.weight(1f)) { c, d -> sendKey(c, d) }
                StandardKey("5", KeyboardEncoder.HID_5, Modifier.weight(1f)) { c, d -> sendKey(c, d) }
                StandardKey("6", KeyboardEncoder.HID_6, Modifier.weight(1f)) { c, d -> sendKey(c, d) }
                StandardKey("7", KeyboardEncoder.HID_7, Modifier.weight(1f)) { c, d -> sendKey(c, d) }
                StandardKey("8", KeyboardEncoder.HID_8, Modifier.weight(1f)) { c, d -> sendKey(c, d) }
                StandardKey("9", KeyboardEncoder.HID_9, Modifier.weight(1f)) { c, d -> sendKey(c, d) }
                StandardKey("0", KeyboardEncoder.HID_0, Modifier.weight(1f)) { c, d -> sendKey(c, d) }
                StandardKey("-", KeyboardEncoder.HID_MINUS, Modifier.weight(1f)) { c, d -> sendKey(c, d) }
                StandardKey("=", KeyboardEncoder.HID_EQUAL, Modifier.weight(1f)) { c, d -> sendKey(c, d) }
                StandardKey("Bksp", KeyboardEncoder.HID_BACKSPACE, Modifier.weight(1.6f)) { c, d -> sendKey(c, d) }
            }

            // ── Row 2: Tab, Q-P, [, ], \ (Sum = 14.6f) ────────────────────────
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                StandardKey("Tab", KeyboardEncoder.HID_TAB, Modifier.weight(1.4f)) { c, d -> sendKey(c, d) }
                StandardKey("Q", KeyboardEncoder.HID_Q, Modifier.weight(1f)) { c, d -> sendKey(c, d) }
                StandardKey("W", KeyboardEncoder.HID_W, Modifier.weight(1f)) { c, d -> sendKey(c, d) }
                StandardKey("E", KeyboardEncoder.HID_E, Modifier.weight(1f)) { c, d -> sendKey(c, d) }
                StandardKey("R", KeyboardEncoder.HID_R, Modifier.weight(1f)) { c, d -> sendKey(c, d) }
                StandardKey("T", KeyboardEncoder.HID_T, Modifier.weight(1f)) { c, d -> sendKey(c, d) }
                StandardKey("Y", KeyboardEncoder.HID_Y, Modifier.weight(1f)) { c, d -> sendKey(c, d) }
                StandardKey("U", KeyboardEncoder.HID_U, Modifier.weight(1f)) { c, d -> sendKey(c, d) }
                StandardKey("I", KeyboardEncoder.HID_I, Modifier.weight(1f)) { c, d -> sendKey(c, d) }
                StandardKey("O", KeyboardEncoder.HID_O, Modifier.weight(1f)) { c, d -> sendKey(c, d) }
                StandardKey("P", KeyboardEncoder.HID_P, Modifier.weight(1f)) { c, d -> sendKey(c, d) }
                StandardKey("[", KeyboardEncoder.HID_LEFT_BRACKET, Modifier.weight(1f)) { c, d -> sendKey(c, d) }
                StandardKey("]", KeyboardEncoder.HID_RIGHT_BRACKET, Modifier.weight(1f)) { c, d -> sendKey(c, d) }
                StandardKey("\\", KeyboardEncoder.HID_BACKSLASH, Modifier.weight(1.2f)) { c, d -> sendKey(c, d) }
            }

            // ── Row 3: Caps, A-L, ;, ', Enter (Sum = 14.6f) ───────────────────
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                ModifierKey("Caps", capsActive, Modifier.weight(1.6f)) {
                    capsActive = !capsActive
                    sendKey(KeyboardEncoder.HID_CAPSLOCK, true)
                    sendKey(KeyboardEncoder.HID_CAPSLOCK, false)
                }
                StandardKey("A", KeyboardEncoder.HID_A, Modifier.weight(1f)) { c, d -> sendKey(c, d) }
                StandardKey("S", KeyboardEncoder.HID_S, Modifier.weight(1f)) { c, d -> sendKey(c, d) }
                StandardKey("D", KeyboardEncoder.HID_D, Modifier.weight(1f)) { c, d -> sendKey(c, d) }
                StandardKey("F", KeyboardEncoder.HID_F, Modifier.weight(1f)) { c, d -> sendKey(c, d) }
                StandardKey("G", KeyboardEncoder.HID_G, Modifier.weight(1f)) { c, d -> sendKey(c, d) }
                StandardKey("H", KeyboardEncoder.HID_H, Modifier.weight(1f)) { c, d -> sendKey(c, d) }
                StandardKey("J", KeyboardEncoder.HID_J, Modifier.weight(1f)) { c, d -> sendKey(c, d) }
                StandardKey("K", KeyboardEncoder.HID_K, Modifier.weight(1f)) { c, d -> sendKey(c, d) }
                StandardKey("L", KeyboardEncoder.HID_L, Modifier.weight(1f)) { c, d -> sendKey(c, d) }
                StandardKey(";", KeyboardEncoder.HID_SEMICOLON, Modifier.weight(1f)) { c, d -> sendKey(c, d) }
                StandardKey("'", KeyboardEncoder.HID_APOSTROPHE, Modifier.weight(1f)) { c, d -> sendKey(c, d) }
                StandardKey("Enter", KeyboardEncoder.HID_ENTER, Modifier.weight(2.0f)) { c, d -> sendKey(c, d) }
            }

            // ── Row 4: Shift, Z-M, ,, ., /, Up, Shift (Sum = 14.6f) ───────────
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                ModifierKey("Shift", shiftActive, Modifier.weight(1.8f)) {
                    toggleModifier(KeyboardEncoder.MOD_LSHIFT, shiftActive) { shiftActive = it }
                }
                StandardKey("Z", KeyboardEncoder.HID_Z, Modifier.weight(1f)) { c, d -> sendKey(c, d) }
                StandardKey("X", KeyboardEncoder.HID_X, Modifier.weight(1f)) { c, d -> sendKey(c, d) }
                StandardKey("C", KeyboardEncoder.HID_C, Modifier.weight(1f)) { c, d -> sendKey(c, d) }
                StandardKey("V", KeyboardEncoder.HID_V, Modifier.weight(1f)) { c, d -> sendKey(c, d) }
                StandardKey("B", KeyboardEncoder.HID_B, Modifier.weight(1f)) { c, d -> sendKey(c, d) }
                StandardKey("N", KeyboardEncoder.HID_N, Modifier.weight(1f)) { c, d -> sendKey(c, d) }
                StandardKey("M", KeyboardEncoder.HID_M, Modifier.weight(1f)) { c, d -> sendKey(c, d) }
                StandardKey(",", KeyboardEncoder.HID_COMMA, Modifier.weight(1f)) { c, d -> sendKey(c, d) }
                StandardKey(".", KeyboardEncoder.HID_DOT, Modifier.weight(1f)) { c, d -> sendKey(c, d) }
                StandardKey("/", KeyboardEncoder.HID_SLASH, Modifier.weight(1f)) { c, d -> sendKey(c, d) }
                StandardKey("Up", KeyboardEncoder.HID_UP, Modifier.weight(1f)) { c, d -> sendKey(c, d) }
                ModifierKey("Shift", shiftActive, Modifier.weight(1.8f)) {
                    toggleModifier(KeyboardEncoder.MOD_RSHIFT, shiftActive) { shiftActive = it }
                }
            }

            // ── Row 5: Ctrl, Win, Alt, Space, Left, Down, Right, Ctrl (Sum = 14.6f) ─
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                ModifierKey("Ctrl", ctrlActive, Modifier.weight(1.4f)) {
                    toggleModifier(KeyboardEncoder.MOD_LCTRL, ctrlActive) { ctrlActive = it }
                }
                ModifierKey("Win", winActive, Modifier.weight(1.1f)) {
                    toggleModifier(KeyboardEncoder.MOD_LMETA, winActive) { winActive = it }
                }
                ModifierKey("Alt", altActive, Modifier.weight(1.1f)) {
                    toggleModifier(KeyboardEncoder.MOD_LALT, altActive) { altActive = it }
                }
                StandardKey("Space", KeyboardEncoder.HID_SPACE, Modifier.weight(5.8f)) { c, d -> sendKey(c, d) }
                StandardKey("Left", KeyboardEncoder.HID_LEFT, Modifier.weight(1.3f)) { c, d -> sendKey(c, d) }
                StandardKey("Down", KeyboardEncoder.HID_DOWN, Modifier.weight(1.3f)) { c, d -> sendKey(c, d) }
                StandardKey("Right", KeyboardEncoder.HID_RIGHT, Modifier.weight(1.3f)) { c, d -> sendKey(c, d) }
                ModifierKey("Ctrl", ctrlActive, Modifier.weight(1.3f)) {
                    toggleModifier(KeyboardEncoder.MOD_RCTRL, ctrlActive) { ctrlActive = it }
                }
            }
        }
    }
}

@Composable
private fun StandardKey(
    label: String,
    hidCode: Byte,
    modifier: Modifier = Modifier,
    onKeyEvent: (code: Byte, isDown: Boolean) -> Unit
) {
    var isPressed by remember { mutableStateOf(false) }

    Box(
        modifier = modifier
            .fillMaxHeight()
            .clip(RoundedCornerShape(8.dp))
            .background(if (isPressed) DarkControlElementPressed else DarkControlElement)
            .pointerInput(Unit) {
                awaitEachGesture {
                    val down = awaitFirstDown(requireUnconsumed = false)
                    down.consume()
                    isPressed = true
                    onKeyEvent(hidCode, true)

                    val up = waitForUpOrCancellation()
                    if (up != null) {
                        up.consume()
                    }
                    isPressed = false
                    onKeyEvent(hidCode, false)
                }
            },
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label,
            color = TextWhite,
            fontSize = 13.sp,
            fontWeight = FontWeight.SemiBold
        )
    }
}

@Composable
private fun ModifierKey(
    label: String,
    isActive: Boolean,
    modifier: Modifier = Modifier,
    onToggle: () -> Unit
) {
    var isPressed by remember { mutableStateOf(false) }

    Box(
        modifier = modifier
            .fillMaxHeight()
            .clip(RoundedCornerShape(8.dp))
            .background(
                when {
                    isActive -> AccentMintGreen
                    isPressed -> DarkControlElementPressed
                    else -> DarkControlElement
                }
            )
            .pointerInput(Unit) {
                awaitEachGesture {
                    val down = awaitFirstDown(requireUnconsumed = false)
                    down.consume()
                    isPressed = true

                    val up = waitForUpOrCancellation()
                    if (up != null) {
                        up.consume()
                        onToggle()
                    }
                    isPressed = false
                }
            },
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label,
            color = if (isActive) TextDark else TextWhite,
            fontSize = 13.sp,
            fontWeight = FontWeight.Bold
        )
    }
}
