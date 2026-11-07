package com.droidpad.transport

import com.droidpad.protocol.HandshakePayload
import com.droidpad.protocol.Packet
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.io.InputStream
import java.io.OutputStream

sealed class ConnectionState {
    object Disconnected : ConnectionState()
    data class Connecting(val transportName: String) : ConnectionState()
    data class Connected(val peerName: String, val transportName: String) : ConnectionState()
    data class Error(val message: String) : ConnectionState()
}

interface DroidTransport {
    val name: String
    suspend fun open(): Pair<InputStream, OutputStream>
    suspend fun close()
}

class TransportManager(
    private val scope: CoroutineScope
) {
    private val _connectionState = MutableStateFlow<ConnectionState>(ConnectionState.Disconnected)
    val connectionState: StateFlow<ConnectionState> = _connectionState.asStateFlow()

    private var activeTransport: DroidTransport? = null
    private var outputStream: OutputStream? = null
    private var inputStream: InputStream? = null
    private var readJob: Job? = null
    private var pingJob: Job? = null

    var onRumbleReceived: ((left: Byte, right: Byte) -> Unit)? = null

    fun connect(transport: DroidTransport, deviceName: String) {
        disconnect()

        scope.launch(Dispatchers.IO) {
            try {
                _connectionState.value = ConnectionState.Connecting(transport.name)
                activeTransport = transport

                val (input, output) = transport.open()
                inputStream = input
                outputStream = output

                // Send initial handshake
                val hs = Packet.Handshake(HandshakePayload("1", deviceName))
                hs.writeTo(output)

                // Read server handshake
                val serverHs = Packet.readFrom(input)
                val peerName = if (serverHs is Packet.Handshake) serverHs.payload.name else "Linux Host"

                _connectionState.value = ConnectionState.Connected(peerName, transport.name)

                // Start read loop & ping loop
                startReadLoop(input)
                startPingLoop(output)

            } catch (e: Exception) {
                _connectionState.value = ConnectionState.Error(e.message ?: "Connection failed")
                disconnect()
            }
        }
    }

    fun sendPacket(packet: Packet) {
        val out = outputStream ?: return
        scope.launch(Dispatchers.IO) {
            try {
                synchronized(out) {
                    packet.writeTo(out)
                }
            } catch (e: Exception) {
                disconnect()
            }
        }
    }

    private fun startReadLoop(input: InputStream) {
        readJob = scope.launch(Dispatchers.IO) {
            try {
                while (isActive) {
                    val pkt = Packet.readFrom(input)
                    when (pkt) {
                        is Packet.Rumble -> {
                            onRumbleReceived?.invoke(pkt.payload.leftMotor, pkt.payload.rightMotor)
                        }
                        is Packet.Ping -> {
                            // Keep-alive received
                        }
                        else -> {
                            // Ignored or handled
                        }
                    }
                }
            } catch (e: Exception) {
                if (isActive) {
                    disconnect()
                }
            }
        }
    }

    private fun startPingLoop(output: OutputStream) {
        pingJob = scope.launch(Dispatchers.IO) {
            while (isActive) {
                delay(2000)
                try {
                    synchronized(output) {
                        Packet.Ping.writeTo(output)
                    }
                } catch (e: Exception) {
                    break
                }
            }
        }
    }

    fun disconnect() {
        readJob?.cancel()
        readJob = null
        pingJob?.cancel()
        pingJob = null

        scope.launch(Dispatchers.IO) {
            try {
                activeTransport?.close()
            } catch (_: Exception) {}
            activeTransport = null
            inputStream = null
            outputStream = null
            _connectionState.value = ConnectionState.Disconnected
        }
    }
}
