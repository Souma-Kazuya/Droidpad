package com.droidpad.transport

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ConnectionStateMachineTest {

    @Test
    fun testInitialStateIsDisconnected() {
        val fsm = ConnectionStateMachine()
        assertEquals(ConnectionState.Disconnected, fsm.state)
    }

    @Test
    fun testNormalConnectionLifecycle() {
        val fsm = ConnectionStateMachine()

        // 1. Initiate Connect
        val connecting = fsm.transition(ConnectionEvent.ConnectInitiated("Wi-Fi"))
        assertTrue(connecting is ConnectionState.Connecting)
        assertEquals("Wi-Fi", (connecting as ConnectionState.Connecting).transportName)
        assertEquals(connecting, fsm.state)

        // 2. Handshake Succeeded
        val connected = fsm.transition(ConnectionEvent.HandshakeCompleted("LinuxPC", "Wi-Fi"))
        assertTrue(connected is ConnectionState.Connected)
        val c = connected as ConnectionState.Connected
        assertEquals("LinuxPC", c.peerName)
        assertEquals("Wi-Fi", c.transportName)
        assertEquals(connected, fsm.state)

        // 3. Disconnect
        val disconnected = fsm.transition(ConnectionEvent.DisconnectedExplicitly)
        assertEquals(ConnectionState.Disconnected, disconnected)
        assertEquals(ConnectionState.Disconnected, fsm.state)
    }

    @Test
    fun testConnectionFailurePreservesError() {
        val fsm = ConnectionStateMachine()

        fsm.transition(ConnectionEvent.ConnectInitiated("USB"))
        val error = fsm.transition(ConnectionEvent.ErrorOccurred("Device not found"))

        assertTrue(error is ConnectionState.Error)
        assertEquals("Device not found", (error as ConnectionState.Error).message)
        assertEquals(error, fsm.state)
    }

    @Test
    fun testHandshakeRejectedIfNotConnecting() {
        val fsm = ConnectionStateMachine()

        // When in Disconnected state, HandshakeCompleted must be rejected
        val rejected = fsm.transition(ConnectionEvent.HandshakeCompleted("RogueHost", "Wi-Fi"))
        assertEquals(ConnectionState.Disconnected, rejected)
        assertEquals(ConnectionState.Disconnected, fsm.state)
    }

    @Test
    fun testReconnectFromErrorState() {
        val fsm = ConnectionStateMachine()

        fsm.transition(ConnectionEvent.ErrorOccurred("Previous failure"))
        assertTrue(fsm.state is ConnectionState.Error)

        val connecting = fsm.transition(ConnectionEvent.ConnectInitiated("Bluetooth"))
        assertTrue(connecting is ConnectionState.Connecting)
        assertEquals("Bluetooth", (connecting as ConnectionState.Connecting).transportName)
    }
}
