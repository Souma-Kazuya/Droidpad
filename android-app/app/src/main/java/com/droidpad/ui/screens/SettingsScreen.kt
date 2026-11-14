package com.droidpad.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.droidpad.service.DroidPadService
import com.droidpad.ui.layout.AbxyStyle
import com.droidpad.ui.layout.GamepadLayoutManager
import com.droidpad.ui.layout.StickArrangement

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    service: DroidPadService?,
    onNavigateBack: () -> Unit
) {
    val context = LocalContext.current
    val layoutManager = remember { GamepadLayoutManager.getInstance(context) }
    val layoutPrefs by layoutManager.layoutState.collectAsState()

    var deadzone by remember { mutableFloatStateOf(service?.gamepadEncoder?.deadzone ?: 0.08f) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Settings") },
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
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            // ── Controller Layout Customization ──────────────────────────────
            Text("Controller Layout", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Color(0xFF107C10))

            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                    // ABXY Style Picker
                    Text("ABXY Action Buttons Style", fontWeight = FontWeight.SemiBold)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        FilterChip(
                            selected = layoutPrefs.abxyStyle == AbxyStyle.XBOX,
                            onClick = { layoutManager.updateAbxyStyle(AbxyStyle.XBOX) },
                            label = { Text("Xbox (Y/X/B/A)") }
                        )
                        FilterChip(
                            selected = layoutPrefs.abxyStyle == AbxyStyle.NINTENDO,
                            onClick = { layoutManager.updateAbxyStyle(AbxyStyle.NINTENDO) },
                            label = { Text("Nintendo (X/Y/A/B)") }
                        )
                    }

                    HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))

                    // Joystick Placement
                    Text("Joystick Placement", fontWeight = FontWeight.SemiBold)
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            RadioButton(
                                selected = layoutPrefs.stickArrangement == StickArrangement.ASYMMETRIC_XBOX,
                                onClick = { layoutManager.updateStickArrangement(StickArrangement.ASYMMETRIC_XBOX) }
                            )
                            Spacer(Modifier.width(8.dp))
                            Text("Xbox Asymmetrical (Left stick top, Right stick bottom)")
                        }
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            RadioButton(
                                selected = layoutPrefs.stickArrangement == StickArrangement.SYMMETRIC_BOTTOM,
                                onClick = { layoutManager.updateStickArrangement(StickArrangement.SYMMETRIC_BOTTOM) }
                            )
                            Spacer(Modifier.width(8.dp))
                            Text("PlayStation Symmetrical (Both sticks at bottom)")
                        }
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            RadioButton(
                                selected = layoutPrefs.stickArrangement == StickArrangement.INVERTED_STICKS,
                                onClick = { layoutManager.updateStickArrangement(StickArrangement.INVERTED_STICKS) }
                            )
                            Spacer(Modifier.width(8.dp))
                            Text("Inverted Sticks (D-pad top, Left stick bottom; Right stick top)")
                        }
                    }

                    HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))

                    // Swap Sticks Toggle
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Swap Left / Right Joysticks", fontWeight = FontWeight.Medium)
                            Text("Swaps axes inputs between thumb zones.", fontSize = 12.sp, color = Color.Gray)
                        }
                        Switch(
                            checked = layoutPrefs.swapSticks,
                            onCheckedChange = { layoutManager.updateSwapSticks(it) }
                        )
                    }

                    HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))

                    // Joystick Scaling
                    Text("Joystick Size: ${(layoutPrefs.joystickScale * 100).toInt()}%", fontWeight = FontWeight.Medium)
                    Slider(
                        value = layoutPrefs.joystickScale,
                        onValueChange = { layoutManager.updateScales(it, layoutPrefs.buttonScale) },
                        valueRange = 0.8f..1.4f,
                        steps = 5
                    )

                    // Button Scaling
                    Text("Button Size: ${(layoutPrefs.buttonScale * 100).toInt()}%", fontWeight = FontWeight.Medium)
                    Slider(
                        value = layoutPrefs.buttonScale,
                        onValueChange = { layoutManager.updateScales(layoutPrefs.joystickScale, it) },
                        valueRange = 0.8f..1.4f,
                        steps = 5
                    )
                }
            }

            // Gamepad Calibration
            Text("Gamepad Calibration", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Color(0xFF107C10))

            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Joystick Deadzone: ${(deadzone * 100).toInt()}%", fontWeight = FontWeight.SemiBold)
                    Slider(
                        value = deadzone,
                        onValueChange = {
                            deadzone = it
                            service?.gamepadEncoder?.deadzone = it
                        },
                        valueRange = 0.0f..0.30f,
                        steps = 6
                    )
                    Text("Higher values prevent stick drift on sensitive touchscreens.", fontSize = 12.sp, color = Color.Gray)
                }
            }

            // Connection Guides
            Text("Connection Setup", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Color(0xFF107C10))

            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("USB Connection Guide", fontWeight = FontWeight.Bold)
                    Text("1. Connect device via USB and enable USB Debugging.")
                    Text("2. On Linux terminal, run:")
                    Text(
                        "adb reverse tcp:27184 tcp:27184",
                        color = Color(0xFF107C10),
                        fontWeight = FontWeight.Bold
                    )
                    Text("3. Tap USB on home screen to connect with < 3ms latency.")
                }
            }

            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Bluetooth Connection Guide", fontWeight = FontWeight.Bold)
                    Text("1. Pair your Linux PC in Android's Bluetooth Settings.")
                    Text("2. Open DroidPad and tap Bluetooth.")
                    Text("3. Select your PC from the paired devices list.")
                }
            }

            // About Section
            Text("About DroidPad", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Color(0xFF107C10))

            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text("DroidPad v1.0.0", fontWeight = FontWeight.Bold)
                    Text("Android to Linux Keyboard & Gamepad Bridge")
                    Text("Transports: Wi-Fi (TLS), Bluetooth (RFCOMM), USB (ADB)")
                    Text("Emulation: Linux uinput (evdev & Xbox 360)")
                }
            }
        }
    }
}
