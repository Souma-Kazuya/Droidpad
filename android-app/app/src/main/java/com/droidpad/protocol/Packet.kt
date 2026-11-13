package com.droidpad.protocol

import org.json.JSONObject
import java.io.DataInputStream
import java.io.DataOutputStream
import java.io.InputStream
import java.io.OutputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder

/**
 * Packet types corresponding to the DroidPad wire specification.
 */
enum class PacketType(val id: Byte) {
    KEYBOARD(0x01),
    GAMEPAD(0x02),
    PING(0x03),
    RUMBLE(0x04),
    HANDSHAKE(0x05),
    GAME_CONTEXT(0x06);

    companion object {
        fun fromId(id: Byte): PacketType = entries.firstOrNull { it.id == id }
            ?: throw IllegalArgumentException("Unknown packet type: 0x%02X".format(id))
    }
}

data class KeyboardPayload(
    val modifiers: Byte,
    val keycodes: ByteArray // Exactly 6 bytes
) {
    init {
        require(keycodes.size == 6) { "Keyboard payload keycodes must be exactly 6 bytes" }
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false
        other as KeyboardPayload
        return modifiers == other.modifiers && keycodes.contentEquals(other.keycodes)
    }

    override fun hashCode(): Int = 31 * modifiers.toInt() + keycodes.contentHashCode()
}

data class GamepadPayload(
    val buttons: Short,
    val leftX: Short,
    val leftY: Short,
    val rightX: Short,
    val rightY: Short,
    val leftTrigger: Byte,
    val rightTrigger: Byte
)

data class RumblePayload(
    val leftMotor: Byte,
    val rightMotor: Byte
)

data class HandshakePayload(
    val version: String,
    val name: String
)

data class GameContextPayload(
    val gameTitle: String,
    val genre: String,
    val activeProcess: String
)

sealed class Packet(val type: PacketType) {
    data class Keyboard(val payload: KeyboardPayload) : Packet(PacketType.KEYBOARD)
    data class Gamepad(val payload: GamepadPayload) : Packet(PacketType.GAMEPAD)
    object Ping : Packet(PacketType.PING)
    data class Rumble(val payload: RumblePayload) : Packet(PacketType.RUMBLE)
    data class Handshake(val payload: HandshakePayload) : Packet(PacketType.HANDSHAKE)
    data class GameContext(val payload: GameContextPayload) : Packet(PacketType.GAME_CONTEXT)

    fun writeTo(output: OutputStream) {
        val payloadBytes = when (this) {
            is Keyboard -> {
                val buf = ByteBuffer.allocate(8).order(ByteOrder.LITTLE_ENDIAN)
                buf.put(payload.modifiers)
                buf.put(0.toByte()) // Reserved
                buf.put(payload.keycodes)
                buf.array()
            }
            is Gamepad -> {
                val buf = ByteBuffer.allocate(12).order(ByteOrder.LITTLE_ENDIAN)
                buf.putShort(payload.buttons)
                buf.putShort(payload.leftX)
                buf.putShort(payload.leftY)
                buf.putShort(payload.rightX)
                buf.putShort(payload.rightY)
                buf.put(payload.leftTrigger)
                buf.put(payload.rightTrigger)
                buf.array()
            }
            is Ping -> ByteArray(0)
            is Rumble -> byteArrayOf(payload.leftMotor, payload.rightMotor)
            is Handshake -> {
                val json = JSONObject().apply {
                    put("version", payload.version)
                    put("name", payload.name)
                }.toString().toByteArray(Charsets.UTF_8)
                json
            }
            is GameContext -> {
                val json = JSONObject().apply {
                    put("game_title", payload.gameTitle)
                    put("genre", payload.genre)
                    put("active_process", payload.activeProcess)
                }.toString().toByteArray(Charsets.UTF_8)
                json
            }
        }

        val header = byteArrayOf(
            0x44.toByte(), // 'D'
            0x50.toByte(), // 'P'
            type.id,
            payloadBytes.size.toByte()
        )

        output.write(header)
        if (payloadBytes.isNotEmpty()) {
            output.write(payloadBytes)
        }
        output.flush()
    }

    companion object {
        private const val MAGIC_0 = 0x44.toByte()
        private const val MAGIC_1 = 0x50.toByte()

        fun readFrom(input: InputStream): Packet {
            val header = ByteArray(4)
            var read = 0
            while (read < 4) {
                val count = input.read(header, read, 4 - read)
                if (count == -1) throw java.io.EOFException("Premature EOF reading header")
                read += count
            }

            if (header[0] != MAGIC_0 || header[1] != MAGIC_1) {
                throw IllegalStateException("Invalid packet magic: 0x%02X 0x%02X".format(header[0], header[1]))
            }

            val pktType = PacketType.fromId(header[2])
            val length = header[3].toInt() and 0xFF

            val payloadBytes = ByteArray(length)
            read = 0
            while (read < length) {
                val count = input.read(payloadBytes, read, length - read)
                if (count == -1) throw java.io.EOFException("Premature EOF reading payload")
                read += count
            }

            return when (pktType) {
                PacketType.KEYBOARD -> {
                    val modifiers = payloadBytes[0]
                    val keycodes = payloadBytes.copyOfRange(2, 8)
                    Keyboard(KeyboardPayload(modifiers, keycodes))
                }
                PacketType.GAMEPAD -> {
                    val buf = ByteBuffer.wrap(payloadBytes).order(ByteOrder.LITTLE_ENDIAN)
                    val buttons = buf.short
                    val lx = buf.short
                    val ly = buf.short
                    val rx = buf.short
                    val ry = buf.short
                    val lt = buf.get()
                    val rt = buf.get()
                    Gamepad(GamepadPayload(buttons, lx, ly, rx, ry, lt, rt))
                }
                PacketType.PING -> Ping
                PacketType.RUMBLE -> {
                    Rumble(RumblePayload(payloadBytes[0], payloadBytes[1]))
                }
                PacketType.HANDSHAKE -> {
                    val jsonStr = String(payloadBytes, Charsets.UTF_8)
                    val json = JSONObject(jsonStr)
                    Handshake(HandshakePayload(json.getString("version"), json.getString("name")))
                }
                PacketType.GAME_CONTEXT -> {
                    val jsonStr = String(payloadBytes, Charsets.UTF_8)
                    val json = JSONObject(jsonStr)
                    GameContext(
                        GameContextPayload(
                            gameTitle = json.optString("game_title", "Unknown Game"),
                            genre = json.optString("genre", "general"),
                            activeProcess = json.optString("active_process", "")
                        )
                    )
                }
            }
        }
    }
}
