package com.droidpad.protocol

import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.EOFException

class PacketTest {

    @Test
    fun testPingRoundtrip() {
        val out = ByteArrayOutputStream()
        Packet.Ping.writeTo(out)

        val bytes = out.toByteArray()
        // Header: [0x44, 0x50, 0x03, 0x00]
        assertArrayEquals(byteArrayOf(0x44, 0x50, 0x03, 0x00), bytes)

        val input = ByteArrayInputStream(bytes)
        val read = Packet.readFrom(input)
        assertEquals(Packet.Ping, read)
    }

    @Test
    fun testKeyboardRoundtrip() {
        val keycodes = byteArrayOf(0x04, 0x05, 0x06, 0x00, 0x00, 0x00)
        val original = Packet.Keyboard(KeyboardPayload(modifiers = 0x05, keycodes = keycodes))

        val out = ByteArrayOutputStream()
        original.writeTo(out)

        val bytes = out.toByteArray()
        assertEquals(12, bytes.size) // 4 header + 8 payload
        assertEquals(0x44.toByte(), bytes[0])
        assertEquals(0x50.toByte(), bytes[1])
        assertEquals(PacketType.KEYBOARD.id, bytes[2])
        assertEquals(8.toByte(), bytes[3])

        val input = ByteArrayInputStream(bytes)
        val read = Packet.readFrom(input)

        assertTrue(read is Packet.Keyboard)
        val k = (read as Packet.Keyboard).payload
        assertEquals(0x05.toByte(), k.modifiers)
        assertArrayEquals(keycodes, k.keycodes)
    }

    @Test
    fun testGamepadRoundtrip() {
        val payload = GamepadPayload(
            buttons = (0x0001 or 0x0400).toShort(), // BTN_A + DPAD_UP
            leftX = -12345,
            leftY = 20000,
            rightX = 0,
            rightY = -30000,
            leftTrigger = 250.toByte(),
            rightTrigger = 128.toByte()
        )
        val original = Packet.Gamepad(payload)

        val out = ByteArrayOutputStream()
        original.writeTo(out)

        val bytes = out.toByteArray()
        assertEquals(16, bytes.size) // 4 header + 12 payload

        val input = ByteArrayInputStream(bytes)
        val read = Packet.readFrom(input)

        assertTrue(read is Packet.Gamepad)
        val g = (read as Packet.Gamepad).payload
        assertEquals(payload.buttons, g.buttons)
        assertEquals(payload.leftX, g.leftX)
        assertEquals(payload.leftY, g.leftY)
        assertEquals(payload.rightX, g.rightX)
        assertEquals(payload.rightY, g.rightY)
        assertEquals(payload.leftTrigger, g.leftTrigger)
        assertEquals(payload.rightTrigger, g.rightTrigger)
    }

    @Test
    fun testRumbleRoundtrip() {
        val original = Packet.Rumble(RumblePayload(leftMotor = 100.toByte(), rightMotor = 200.toByte()))

        val out = ByteArrayOutputStream()
        original.writeTo(out)

        val bytes = out.toByteArray()
        assertEquals(6, bytes.size) // 4 header + 2 payload

        val input = ByteArrayInputStream(bytes)
        val read = Packet.readFrom(input)

        assertTrue(read is Packet.Rumble)
        val r = (read as Packet.Rumble).payload
        assertEquals(100.toByte(), r.leftMotor)
        assertEquals(200.toByte(), r.rightMotor)
    }

    @Test
    fun testHandshakeRoundtrip() {
        val original = Packet.Handshake(HandshakePayload(version = "1", name = "Pixel 8 Pro"))

        val out = ByteArrayOutputStream()
        original.writeTo(out)

        val bytes = out.toByteArray()
        assertTrue(bytes.size > 4)

        val input = ByteArrayInputStream(bytes)
        val read = Packet.readFrom(input)

        assertTrue(read is Packet.Handshake)
        val h = (read as Packet.Handshake).payload
        assertEquals("1", h.version)
        assertEquals("Pixel 8 Pro", h.name)
    }

    @Test
    fun testGameContextRoundtrip() {
        val original = Packet.GameContext(
            GameContextPayload(
                gameTitle = "Elden Ring",
                genre = "soulslike",
                activeProcess = "eldenring.exe"
            )
        )

        val out = ByteArrayOutputStream()
        original.writeTo(out)

        val bytes = out.toByteArray()
        assertEquals(0x44.toByte(), bytes[0])
        assertEquals(0x50.toByte(), bytes[1])
        assertEquals(PacketType.GAME_CONTEXT.id, bytes[2])

        val input = ByteArrayInputStream(bytes)
        val read = Packet.readFrom(input)

        assertTrue(read is Packet.GameContext)
        val gc = (read as Packet.GameContext).payload
        assertEquals("Elden Ring", gc.gameTitle)
        assertEquals("soulslike", gc.genre)
        assertEquals("eldenring.exe", gc.activeProcess)
    }

    @Test
    fun testBadMagicRejection() {
        val badBytes = byteArrayOf(0x00, 0x00, 0x03, 0x00)
        try {
            Packet.readFrom(ByteArrayInputStream(badBytes))
            fail("Expected IllegalStateException for invalid magic")
        } catch (e: IllegalStateException) {
            assertTrue(e.message!!.contains("Invalid packet magic"))
        }
    }

    @Test
    fun testUnknownPacketTypeRejection() {
        val unknownTypeBytes = byteArrayOf(0x44, 0x50, 0xFF.toByte(), 0x00)
        try {
            Packet.readFrom(ByteArrayInputStream(unknownTypeBytes))
            fail("Expected IllegalArgumentException for unknown packet type")
        } catch (e: IllegalArgumentException) {
            assertTrue(e.message!!.contains("Unknown packet type"))
        }
    }

    @Test
    fun testTruncatedPayloadRejection() {
        // Declares 8 payload bytes, but only provides 3
        val truncated = byteArrayOf(0x44, 0x50, 0x01, 0x08, 0x01, 0x02, 0x03)
        try {
            Packet.readFrom(ByteArrayInputStream(truncated))
            fail("Expected EOFException for truncated stream")
        } catch (e: EOFException) {
            assertTrue(e.message!!.contains("EOF"))
        }
    }

    @Test(expected = IllegalArgumentException::class)
    fun testKeyboardPayloadKeycodesSizeRequirement() {
        KeyboardPayload(0, ByteArray(5))
    }
}
