package com.droidpad.ui.screens

import android.app.Activity
import android.content.pm.ActivityInfo
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import com.droidpad.ai.AssistantUiState
import com.droidpad.hid.GamepadEncoder
import com.droidpad.service.DroidPadService
import com.droidpad.transport.ConnectionState
import com.droidpad.ui.components.*
import com.droidpad.ui.layout.GamepadLayoutManager
import com.droidpad.ui.layout.StickArrangement
import com.droidpad.ui.theme.*

/**
 * Gamepad layout adhering strictly to Image 1.
 * Provides complete visual parity, isolated multi-touch gesture zones to prevent interference,
 * and context-only presentation (no generic toolbars or distractions).
 */
@Composable
fun GamepadScreen(
    service: DroidPadService?,
    onNavigateToKeyboard: () -> Unit,
    onNavigateBack: () -> Unit,
    onNavigateToSettings: () -> Unit
) {
    val context = LocalContext.current
    val activity = context as? Activity

    // Immersive Landscape Effect: Locks to landscape and hides system bars
    // so only the controller context is displayed and buttons don't interfere with system UI.
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

    val encoder = remember { service?.gamepadEncoder ?: GamepadEncoder() }
    val connectionState by service?.transportManager?.connectionState?.collectAsState()
        ?: remember { mutableStateOf(ConnectionState.Disconnected) }
    val assistantState by service?.assistantManager?.uiState?.collectAsState()
        ?: remember { mutableStateOf(AssistantUiState()) }

    val layoutManager = remember { GamepadLayoutManager.getInstance(context) }
    val layoutPrefs by layoutManager.layoutState.collectAsState()

    fun sendButton(mask: Short, pressed: Boolean) {
        val pkt = encoder.setButton(mask, pressed)
        service?.transportManager?.sendPacket(pkt)
    }

    fun sendTrigger(isLeft: Boolean, value: Float) {
        val pkt = if (isLeft) {
            encoder.updateLeftTrigger(value)
        } else {
            encoder.updateRightTrigger(value)
        }
        service?.transportManager?.sendPacket(pkt)
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(DarkSlateBackground)
            .windowInsetsPadding(WindowInsets.safeDrawing)
            .padding(horizontal = 14.dp, vertical = 10.dp)
    ) {
        // ── 1. Top Bar: LT/LB (Left) | Mode Toggle (Center) | RB/RT (Right) ──
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.TopCenter),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Left Trigger & Bumper Row
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                GamepadButton(
                    label = "LT",
                    modifier = Modifier.size(width = 74.dp, height = 46.dp),
                    shape = RoundedCornerShape(12.dp)
                ) { pressed ->
                    sendTrigger(isLeft = true, if (pressed) 1.0f else 0.0f)
                }

                GamepadButton(
                    label = "LB",
                    modifier = Modifier.size(width = 74.dp, height = 46.dp),
                    shape = RoundedCornerShape(12.dp)
                ) { pressed ->
                    sendButton(GamepadEncoder.BTN_LB, pressed)
                }
            }

            // Center: Segmented Toggle [ Gamepad | Keyboard ]
            ModeTogglePill(
                selectedMode = ControllerMode.GAMEPAD,
                onModeChange = { mode ->
                    if (mode == ControllerMode.KEYBOARD) {
                        onNavigateToKeyboard()
                    }
                }
            )

            // Right Bumper & Trigger Row
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                GamepadButton(
                    label = "RB",
                    modifier = Modifier.size(width = 74.dp, height = 46.dp),
                    shape = RoundedCornerShape(12.dp)
                ) { pressed ->
                    sendButton(GamepadEncoder.BTN_RB, pressed)
                }

                GamepadButton(
                    label = "RT",
                    modifier = Modifier.size(width = 74.dp, height = 46.dp),
                    shape = RoundedCornerShape(12.dp)
                ) { pressed ->
                    sendTrigger(isLeft = false, if (pressed) 1.0f else 0.0f)
                }
            }
        }

        // ── 2. Left Thumb Zone ──────────────────────────────────────────────
        val joystickSize = (132 * layoutPrefs.joystickScale).dp
        val diamondSize = (138 * layoutPrefs.buttonScale).dp
        val dpadSize = (125 * layoutPrefs.buttonScale).dp

        val leftStickComposable = @Composable {
            VirtualJoystick(
                size = joystickSize,
                onMove = { x, y ->
                    val pkt = if (!layoutPrefs.swapSticks) {
                        encoder.updateLeftStick(x, y)
                    } else {
                        encoder.updateRightStick(x, y)
                    }
                    service?.transportManager?.sendPacket(pkt)
                }
            )
        }

        val rightStickComposable = @Composable {
            VirtualJoystick(
                size = joystickSize,
                onMove = { x, y ->
                    val pkt = if (!layoutPrefs.swapSticks) {
                        encoder.updateRightStick(x, y)
                    } else {
                        encoder.updateLeftStick(x, y)
                    }
                    service?.transportManager?.sendPacket(pkt)
                }
            )
        }

        val dpadComposable = @Composable {
            Dpad(
                size = dpadSize,
                onDirectionChange = { up, down, left, right ->
                    encoder.setButton(GamepadEncoder.DPAD_UP, up)
                    encoder.setButton(GamepadEncoder.DPAD_DOWN, down)
                    encoder.setButton(GamepadEncoder.DPAD_LEFT, left)
                    val pkt = encoder.setButton(GamepadEncoder.DPAD_RIGHT, right)
                    service?.transportManager?.sendPacket(pkt)
                }
            )
        }

        val abxyComposable = @Composable {
            ABXYDiamond(
                size = diamondSize,
                style = layoutPrefs.abxyStyle,
                onButtonPress = { letter, pressed ->
                    when (letter) {
                        "A" -> sendButton(GamepadEncoder.BTN_A, pressed)
                        "B" -> sendButton(GamepadEncoder.BTN_B, pressed)
                        "X" -> sendButton(GamepadEncoder.BTN_X, pressed)
                        "Y" -> sendButton(GamepadEncoder.BTN_Y, pressed)
                    }
                }
            )
        }

        Column(
            modifier = Modifier
                .align(Alignment.CenterStart)
                .fillMaxHeight()
                .padding(top = 58.dp, bottom = 4.dp),
            verticalArrangement = Arrangement.SpaceBetween,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            when (layoutPrefs.stickArrangement) {
                StickArrangement.ASYMMETRIC_XBOX -> {
                    // Left Stick High, Dpad Low
                    leftStickComposable()
                    dpadComposable()
                }
                StickArrangement.SYMMETRIC_BOTTOM -> {
                    // PlayStation Style: Dpad High, Left Stick Low
                    dpadComposable()
                    leftStickComposable()
                }
                StickArrangement.INVERTED_STICKS -> {
                    // Dpad High, Left Stick Low
                    dpadComposable()
                    leftStickComposable()
                }
            }
        }

        // ── 3. Center Control Zone ───────────────────────────────────────────
        Column(
            modifier = Modifier
                .align(Alignment.Center)
                .fillMaxHeight()
                .padding(top = 58.dp, bottom = 4.dp),
            verticalArrangement = Arrangement.SpaceBetween,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Sub-cluster: Connection Card + Mini AI HUD + (View / Guide / Menu)
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Connection Status Card
                GamepadConnectionCard(
                    connectionState = connectionState
                )

                // Mini AI Assistant HUD Pill
                MiniAiHudPill(
                    currentTip = assistantState.currentTip,
                    activeGameTitle = assistantState.activeGameTitle,
                    onNextTip = { service?.assistantManager?.nextTip() },
                    onPrevTip = { service?.assistantManager?.prevTip() },
                    onClick = { service?.assistantManager?.setDrawerOpen(true) },
                    modifier = Modifier.widthIn(max = 280.dp)
                )

                // View - Guide (X) - Menu Row
                Row(
                    horizontalArrangement = Arrangement.spacedBy(14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    GamepadButton(
                        label = "View",
                        modifier = Modifier.size(width = 66.dp, height = 42.dp),
                        shape = RoundedCornerShape(10.dp)
                    ) { pressed ->
                        sendButton(GamepadEncoder.BTN_SELECT, pressed)
                    }

                    // Xbox Guide Button (Nexus)
                    GamepadButton(
                        label = "X",
                        modifier = Modifier.size(46.dp),
                        buttonColor = AccentMintGreen,
                        pressedColor = AccentMintGreenPressed,
                        textColor = TextDark,
                        fontSize = 16.sp,
                        shape = CircleShape
                    ) { pressed ->
                        sendButton(GamepadEncoder.BTN_GUIDE, pressed)
                    }

                    GamepadButton(
                        label = "Menu",
                        modifier = Modifier.size(width = 66.dp, height = 42.dp),
                        shape = RoundedCornerShape(10.dp)
                    ) { pressed ->
                        sendButton(GamepadEncoder.BTN_START, pressed)
                    }
                }
            }

            // Bottom Center: Settings & Disconnect Buttons
            Row(
                horizontalArrangement = Arrangement.spacedBy(14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                GamepadButton(
                    label = "Settings",
                    modifier = Modifier.size(width = 98.dp, height = 42.dp),
                    shape = RoundedCornerShape(10.dp)
                ) { pressed ->
                    if (!pressed) {
                        onNavigateToSettings()
                    }
                }

                GamepadButton(
                    label = "Disconnect",
                    modifier = Modifier.size(width = 108.dp, height = 42.dp),
                    shape = RoundedCornerShape(10.dp)
                ) { pressed ->
                    if (!pressed) {
                        service?.transportManager?.disconnect()
                        onNavigateBack()
                    }
                }
            }
        }

        // ── 4. Right Thumb Zone ──────────────────────────────────────────────
        Column(
            modifier = Modifier
                .align(Alignment.CenterEnd)
                .fillMaxHeight()
                .padding(top = 58.dp, bottom = 4.dp),
            verticalArrangement = Arrangement.SpaceBetween,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            when (layoutPrefs.stickArrangement) {
                StickArrangement.ASYMMETRIC_XBOX -> {
                    // ABXY High, Right Stick Low
                    abxyComposable()
                    rightStickComposable()
                }
                StickArrangement.SYMMETRIC_BOTTOM -> {
                    // PlayStation Style: ABXY High, Right Stick Low
                    abxyComposable()
                    rightStickComposable()
                }
                StickArrangement.INVERTED_STICKS -> {
                    // Right Stick High, ABXY Low
                    rightStickComposable()
                    abxyComposable()
                }
            }
        }

        // ── 5. Offline Mini AI Assistant Drawer (Overlay) ────────────────────
        if (assistantState.isDrawerOpen) {
            AiAssistantDrawer(
                state = assistantState,
                onClose = { service?.assistantManager?.setDrawerOpen(false) },
                onSelectGameGenre = { title, genre ->
                    service?.assistantManager?.setGameContext(title, genre)
                },
                onAskQuery = { query ->
                    service?.assistantManager?.askAssistant(query)
                }
            )
        }
    }
}
