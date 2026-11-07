package com.droidpad.transport

import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothSocket
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.InputStream
import java.io.OutputStream
import java.util.UUID

class BluetoothTransport(
    private val deviceAddress: String
) : DroidTransport {

    override val name: String = "Bluetooth"
    private var socket: BluetoothSocket? = null

    @SuppressLint("MissingPermission")
    override suspend fun open(): Pair<InputStream, OutputStream> = withContext(Dispatchers.IO) {
        val adapter = BluetoothAdapter.getDefaultAdapter()
            ?: throw IllegalStateException("Bluetooth is not supported on this device")

        if (!adapter.isEnabled) {
            throw IllegalStateException("Bluetooth is currently disabled")
        }

        val device: BluetoothDevice = adapter.getRemoteDevice(deviceAddress)
        val rfcommSocket = device.createRfcommSocketToServiceRecord(DROIDPAD_UUID)

        adapter.cancelDiscovery() // Always cancel discovery before connecting
        rfcommSocket.connect()

        socket = rfcommSocket
        Pair(rfcommSocket.inputStream, rfcommSocket.outputStream)
    }

    override suspend fun close() = withContext(Dispatchers.IO) {
        try {
            socket?.close()
        } catch (_: Exception) {}
        socket = null
    }

    companion object {
        val DROIDPAD_UUID: UUID = UUID.fromString("6e400001-b5a3-f393-e0a9-e50e24dcca9e")
    }
}
