package com.droidpad.transport

import com.droidpad.protocol.HandshakePayload
import com.droidpad.protocol.Packet
import kotlinx.coroutines.*
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream
import java.io.PipedInputStream
import java.io.PipedOutputStream

class FakeTransport(
    override val name: String,
    private val shouldFail: Boolean = false,
    private val hostName: String = "ArchPC"
) : DroidTransport {
    var wasClosed = false
    var opened = false

    private val pipedOut = PipedOutputStream()
    private val pipedIn = PipedInputStream(pipedOut)

    override suspend fun open(): Pair<InputStream, OutputStream> {
        if (shouldFail) {
            throw IOException("Transport connection refused")
        }
        opened = true
        val serverHs = Packet.Handshake(HandshakePayload("1", hostName))
        serverHs.writeTo(pipedOut)
        val output = ByteArrayOutputStream()
        return Pair(pipedIn, output)
    }

    override suspend fun close() {
        wasClosed = true
        try {
            pipedOut.close()
            pipedIn.close()
        } catch (_: Exception) {}
    }
}

class TransportManagerTest {

    @Test
    fun testSuccessfulConnection() = runBlocking {
        val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
        val manager = TransportManager(scope)

        val transport = FakeTransport("Wi-Fi", shouldFail = false, hostName = "LivingRoomPC")
        manager.connect(transport, "Pixel8")

        // Wait for connection to transition to Connected
        withTimeout(3000) {
            while (manager.connectionState.value !is ConnectionState.Connected) {
                delay(20)
            }
        }

        val state = manager.connectionState.value
        assertTrue("Expected Connected state, got $state", state is ConnectionState.Connected)
        val connected = state as ConnectionState.Connected
        assertEquals("LivingRoomPC", connected.peerName)
        assertEquals("Wi-Fi", connected.transportName)

        manager.disconnectSuspend()
        assertEquals(ConnectionState.Disconnected, manager.connectionState.value)
        assertTrue(transport.wasClosed)
        scope.cancel()
    }

    @Test
    fun testConnectionFailurePreservesErrorState() = runBlocking {
        val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
        val manager = TransportManager(scope)

        val transport = FakeTransport("Bluetooth", shouldFail = true)
        manager.connect(transport, "Pixel8")

        // Wait for connection to transition to Error
        withTimeout(3000) {
            while (manager.connectionState.value !is ConnectionState.Error) {
                delay(20)
            }
        }

        val state = manager.connectionState.value
        assertTrue("Expected Error state, got $state", state is ConnectionState.Error)
        val error = state as ConnectionState.Error
        assertEquals("Transport connection refused", error.message)
        scope.cancel()
    }

    @Test
    fun testRapidReconnectCancelsPrevious() = runBlocking {
        val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
        val manager = TransportManager(scope)

        val transport1 = FakeTransport("Wi-Fi", hostName = "Host1")
        val transport2 = FakeTransport("USB", hostName = "Host2")

        manager.connect(transport1, "Pixel8")
        // Rapid switch
        manager.connect(transport2, "Pixel8")

        withTimeout(3000) {
            while (manager.connectionState.value !is ConnectionState.Connected) {
                delay(20)
            }
        }

        val state = manager.connectionState.value
        assertTrue("Expected Connected state for transport2, got $state", state is ConnectionState.Connected)
        val connected = state as ConnectionState.Connected
        assertEquals("Host2", connected.peerName)
        assertEquals("USB", connected.transportName)

        manager.disconnectSuspend()
        scope.cancel()
    }
}
