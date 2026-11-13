package com.droidpad.transport

/**
 * Events driving state transitions in the connection lifecycle.
 */
sealed class ConnectionEvent {
    data class ConnectInitiated(val transportName: String) : ConnectionEvent()
    data class HandshakeCompleted(val peerName: String, val transportName: String) : ConnectionEvent()
    data class ErrorOccurred(val message: String) : ConnectionEvent()
    object DisconnectedExplicitly : ConnectionEvent()
    object ConnectionTerminated : ConnectionEvent()
}

/**
 * Formal Finite State Machine for DroidPad connection lifecycle.
 * Enforces valid state transitions and prevents illegal state corruptions.
 */
class ConnectionStateMachine(initialState: ConnectionState = ConnectionState.Disconnected) {

    var state: ConnectionState = initialState
        private set

    /**
     * Executes a state transition based on the incoming event.
     * Returns the updated state.
     */
    @Synchronized
    fun transition(event: ConnectionEvent): ConnectionState {
        val nextState = when (event) {
            is ConnectionEvent.ConnectInitiated -> {
                // Connection can be initiated from Disconnected, Error, or directly switching from another transport
                ConnectionState.Connecting(event.transportName)
            }
            is ConnectionEvent.HandshakeCompleted -> {
                when (val current = state) {
                    is ConnectionState.Connecting -> {
                        ConnectionState.Connected(event.peerName, event.transportName)
                    }
                    else -> {
                        // Handshake completed after connection was canceled or aborted; reject transition
                        current
                    }
                }
            }
            is ConnectionEvent.ErrorOccurred -> {
                ConnectionState.Error(event.message)
            }
            is ConnectionEvent.DisconnectedExplicitly,
            is ConnectionEvent.ConnectionTerminated -> {
                ConnectionState.Disconnected
            }
        }
        state = nextState
        return nextState
    }
}
