package com.droidpad.hid

import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class KeyboardEncoderTest {

    private lateinit var encoder: KeyboardEncoder

    @Before
    fun setUp() {
        encoder = KeyboardEncoder()
    }

    @Test
    fun testInitialState() {
        val pkt = encoder.releaseAll()
        assertEquals(0.toByte(), pkt.payload.modifiers)
        assertArrayEquals(ByteArray(6), pkt.payload.keycodes)
    }

    @Test
    fun testModifierSetAndClear() {
        // Set LCtrl (bit 0 = 0x01)
        var pkt = encoder.setModifier(KeyboardEncoder.MOD_LCTRL, true)
        assertEquals(0x01.toByte(), pkt.payload.modifiers)

        // Set LAlt (bit 2 = 0x04) -> LCtrl | LAlt = 0x05
        pkt = encoder.setModifier(KeyboardEncoder.MOD_LALT, true)
        assertEquals(0x05.toByte(), pkt.payload.modifiers)

        // Clear LCtrl -> remaining is LAlt (0x04)
        pkt = encoder.setModifier(KeyboardEncoder.MOD_LCTRL, false)
        assertEquals(0x04.toByte(), pkt.payload.modifiers)

        // Clear LAlt -> 0
        pkt = encoder.setModifier(KeyboardEncoder.MOD_LALT, false)
        assertEquals(0.toByte(), pkt.payload.modifiers)
    }

    @Test
    fun testToggleModifier() {
        var pkt = encoder.toggleModifier(KeyboardEncoder.MOD_LSHIFT)
        assertEquals(KeyboardEncoder.MOD_LSHIFT, pkt.payload.modifiers)

        pkt = encoder.toggleModifier(KeyboardEncoder.MOD_LSHIFT)
        assertEquals(0.toByte(), pkt.payload.modifiers)
    }

    @Test
    fun testKeyDownAndKeyUp() {
        encoder.keyDown(KeyboardEncoder.HID_A)
        encoder.keyDown(KeyboardEncoder.HID_B)
        val pkt = encoder.keyDown(KeyboardEncoder.HID_C)

        val keys = pkt.payload.keycodes
        assertEquals(KeyboardEncoder.HID_A, keys[0])
        assertEquals(KeyboardEncoder.HID_B, keys[1])
        assertEquals(KeyboardEncoder.HID_C, keys[2])
        assertEquals(0.toByte(), keys[3])

        // Key up B
        val pkt2 = encoder.keyUp(KeyboardEncoder.HID_B)
        val keys2 = pkt2.payload.keycodes
        assertEquals(KeyboardEncoder.HID_A, keys2[0])
        assertEquals(KeyboardEncoder.HID_C, keys2[1])
        assertEquals(0.toByte(), keys2[2])
    }

    @Test
    fun test6KeyRolloverCap() {
        // Press 6 keys: A, B, C, D, E, F
        encoder.keyDown(KeyboardEncoder.HID_A)
        encoder.keyDown(KeyboardEncoder.HID_B)
        encoder.keyDown(KeyboardEncoder.HID_C)
        encoder.keyDown(KeyboardEncoder.HID_D)
        encoder.keyDown(KeyboardEncoder.HID_E)
        val pkt6 = encoder.keyDown(KeyboardEncoder.HID_F)

        assertArrayEquals(
            byteArrayOf(
                KeyboardEncoder.HID_A,
                KeyboardEncoder.HID_B,
                KeyboardEncoder.HID_C,
                KeyboardEncoder.HID_D,
                KeyboardEncoder.HID_E,
                KeyboardEncoder.HID_F
            ),
            pkt6.payload.keycodes
        )

        // Attempt 7th key (G) - should be ignored due to 6KRO cap
        val pkt7 = encoder.keyDown(KeyboardEncoder.HID_G)
        assertArrayEquals(
            byteArrayOf(
                KeyboardEncoder.HID_A,
                KeyboardEncoder.HID_B,
                KeyboardEncoder.HID_C,
                KeyboardEncoder.HID_D,
                KeyboardEncoder.HID_E,
                KeyboardEncoder.HID_F
            ),
            pkt7.payload.keycodes
        )

        // Release one key (A), now G can be accepted
        encoder.keyUp(KeyboardEncoder.HID_A)
        val pktAfterRelease = encoder.keyDown(KeyboardEncoder.HID_G)
        assertTrue(pktAfterRelease.payload.keycodes.contains(KeyboardEncoder.HID_G))
    }

    @Test
    fun testReleaseAllClearsModifiersAndKeys() {
        encoder.setModifier(KeyboardEncoder.MOD_LCTRL, true)
        encoder.keyDown(KeyboardEncoder.HID_SPACE)
        encoder.keyDown(KeyboardEncoder.HID_ENTER)

        val pkt = encoder.releaseAll()
        assertEquals(0.toByte(), pkt.payload.modifiers)
        assertArrayEquals(ByteArray(6), pkt.payload.keycodes)
    }
}
