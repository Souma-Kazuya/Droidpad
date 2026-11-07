package com.droidpad.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.droidpad.service.DroidPadService

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    service: DroidPadService?,
    onNavigateBack: () -> Unit
) {
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
