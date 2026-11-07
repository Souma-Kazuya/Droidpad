package com.droidpad.transport

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.InputStream
import java.io.OutputStream
import java.net.InetSocketAddress
import java.net.Socket

/**
 * Connects over a local ADB tunnel.
 *
 * Setup:
 * On Linux host, execute:
 * `adb reverse tcp:27184 tcp:27184`
 * (allowing Android to connect to localhost:27184 which forwards to the PC daemon)
 */
class UsbTransport(
    private val port: Int = 27184
) : DroidTransport {

    override val name: String = "USB"
    private var socket: Socket? = null

    override suspend fun open(): Pair<InputStream, OutputStream> = withContext(Dispatchers.IO) {
        val s = Socket()
        s.tcpNoDelay = true
        s.connect(InetSocketAddress("127.0.0.1", port), 3000)

        socket = s
        Pair(s.inputStream, s.outputStream)
    }

    override suspend fun close() = withContext(Dispatchers.IO) {
        try {
            socket?.close()
        } catch (_: Exception) {}
        socket = null
    }
}
