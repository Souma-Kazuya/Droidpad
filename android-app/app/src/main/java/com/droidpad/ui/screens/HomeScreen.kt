package com.droidpad.ui.screens

import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.os.Build
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bluetooth
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Usb
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.droidpad.service.DroidPadService
import com.droidpad.transport.BluetoothTransport
import com.droidpad.transport.ConnectionState
import com.droidpad.transport.UsbTransport
import com.droidpad.transport.WifiTransport

@SuppressLint("MissingPermission")
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    service: DroidPadService?,
    onNavigateToKeyboard: () -> Unit,
    onNavigateToGamepad: () -> Unit,
    onNavigateToSettings: () -> Unit
) {
    val context = LocalContext.current
    val connectionState by service?.transportManager?.connectionState?.collectAsState()
        ?: remember { mutableStateOf(ConnectionState.Disconnected) }

    var wifiIp by remember { mutableStateOf("10.0.2.2") }
    var showWifiDialog by remember { mutableStateOf(false) }
    var showBtDialog by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("DroidPad", fontWeight = FontWeight.Bold) },
                actions = {
                    IconButton(onClick = onNavigateToSettings) {
                        Icon(Icons.Default.Settings, contentDescription = "Settings")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Connection Status Card
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                colors = CardDefaults.cardColors(
                    containerColor = when (connectionState) {
                        is ConnectionState.Connected -> Color(0xFF1B5E20)
                        is ConnectionState.Connecting -> Color(0xFFE65100)
                        is ConnectionState.Error -> Color(0xFFB71C1C)
                        ConnectionState.Disconnected -> MaterialTheme.colorScheme.surfaceVariant
                    }
                )
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = when (val s = connectionState) {
                            is ConnectionState.Connected -> "Connected: ${s.peerName}"
                            is ConnectionState.Connecting -> "Connecting (${s.transportName})..."
                            is ConnectionState.Error -> "Error: ${s.message}"
                            ConnectionState.Disconnected -> "Disconnected"
                        },
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )

                    if (connectionState is ConnectionState.Connected) {
                        Spacer(modifier = Modifier.height(8.dp))
                        OutlinedButton(
                            onClick = { service?.transportManager?.disconnect() },
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White)
                        ) {
                            Text("Disconnect")
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "Connect To Linux Host",
                fontWeight = FontWeight.SemiBold,
                fontSize = 16.sp,
                modifier = Modifier.align(Alignment.Start)
            )
            Spacer(modifier = Modifier.height(8.dp))

            // Transport Selection Buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = { showWifiDialog = true },
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(Icons.Default.Wifi, contentDescription = "Wi-Fi", tint = Color(0xFF107C10))
                        Text("Wi-Fi", color = Color.White)
                    }
                }

                Button(
                    onClick = { showBtDialog = true },
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(Icons.Default.Bluetooth, contentDescription = "Bluetooth", tint = Color(0xFF2196F3))
                        Text("Bluetooth", color = Color.White)
                    }
                }

                Button(
                    onClick = {
                        val deviceName = Build.MODEL ?: "Android Device"
                        service?.transportManager?.connect(UsbTransport(), deviceName)
                    },
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(Icons.Default.Usb, contentDescription = "USB", tint = Color(0xFFFF9800))
                        Text("USB", color = Color.White)
                    }
                }
            }

            Spacer(modifier = Modifier.height(32.dp))
            Text(
                text = "Input Modes",
                fontWeight = FontWeight.SemiBold,
                fontSize = 16.sp,
                modifier = Modifier.align(Alignment.Start)
            )
            Spacer(modifier = Modifier.height(12.dp))

            // Mode Launchers
            Button(
                onClick = onNavigateToGamepad,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(64.dp),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF107C10))
            ) {
                Text("🎮  Open Gamepad Mode", fontSize = 18.sp, fontWeight = FontWeight.Bold)
            }

            Spacer(modifier = Modifier.height(12.dp))

            Button(
                onClick = onNavigateToKeyboard,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(64.dp),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2D2D2D))
            ) {
                Text("⌨️  Open Keyboard Mode", fontSize = 18.sp, fontWeight = FontWeight.Bold)
            }
        }
    }

    // Wi-Fi Connection Dialog
    if (showWifiDialog) {
        AlertDialog(
            onDismissRequest = { showWifiDialog = false },
            title = { Text("Wi-Fi Connection") },
            text = {
                Column {
                    Text("Enter Linux Host IP address:")
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = wifiIp,
                        onValueChange = { wifiIp = it },
                        singleLine = true,
                        label = { Text("Host IP") }
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        showWifiDialog = false
                        val deviceName = Build.MODEL ?: "Android Device"
                        service?.transportManager?.connect(WifiTransport(wifiIp), deviceName)
                    }
                ) {
                    Text("Connect")
                }
            },
            dismissButton = {
                TextButton(onClick = { showWifiDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    // Bluetooth Paired Devices Dialog
    if (showBtDialog) {
        val adapter = remember { BluetoothAdapter.getDefaultAdapter() }
        val pairedDevices = remember { adapter?.bondedDevices?.toList() ?: emptyList<BluetoothDevice>() }

        AlertDialog(
            onDismissRequest = { showBtDialog = false },
            title = { Text("Select Paired Linux PC") },
            text = {
                if (pairedDevices.isEmpty()) {
                    Text("No paired Bluetooth devices found. Please pair your PC in Android Bluetooth settings first.")
                } else {
                    Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                        pairedDevices.forEach { device ->
                            TextButton(
                                onClick = {
                                    showBtDialog = false
                                    val deviceName = Build.MODEL ?: "Android Device"
                                    service?.transportManager?.connect(BluetoothTransport(device.address), deviceName)
                                },
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text("${device.name ?: "Unknown"} (${device.address})")
                            }
                        }
                    }
                }
            },
            confirmButton = {},
            dismissButton = {
                TextButton(onClick = { showBtDialog = false }) {
                    Text("Close")
                }
            }
        )
    }
}
