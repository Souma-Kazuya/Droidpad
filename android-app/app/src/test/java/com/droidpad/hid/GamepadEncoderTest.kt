package com.droidpad.hid

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class GamepadEncoderTest {

    private lateinit var encoder: GamepadEncoder

    @Before
    fun setUp() {
        encoder = GamepadEncoder()
    }

    @Test
    fun testInitialState() {
        val pkt = encoder.reset()
        val g = pkt.payload
        assertEquals(0.toShort(), g.buttons)
        assertEquals(0.toShort(), g.leftX)
        assertEquals(0.toShort(), g.leftY)
        assertEquals(0.toShort(), g.rightX)
        assertEquals(0.toShort(), g.rightY)
        assertEquals(0.toByte(), g.leftTrigger)
        assertEquals(0.toByte(), g.rightTrigger)
    }

    @Test
    fun testButtonMaskSetAndClear() {
        // Press A
        var pkt = encoder.setButton(GamepadEncoder.BTN_A, true)
        assertEquals(GamepadEncoder.BTN_A, pkt.payload.buttons)

        // Press Start while holding A
        pkt = encoder.setButton(GamepadEncoder.BTN_START, true)
        val expectedBoth = (GamepadEncoder.BTN_A.toInt() or GamepadEncoder.BTN_START.toInt()).toShort()
        assertEquals(expectedBoth, pkt.payload.buttons)

        // Release A, keep Start
        pkt = encoder.setButton(GamepadEncoder.BTN_A, false)
        assertEquals(GamepadEncoder.BTN_START, pkt.payload.buttons)

        // Release Start
        pkt = encoder.setButton(GamepadEncoder.BTN_START, false)
        assertEquals(0.toShort(), pkt.payload.buttons)
    }

    @Test
    fun testAnalogStickDeadzone() {
        encoder.deadzone = 0.08f // 8%

        // Small drift within deadzone must be filtered to zero
        var pkt = encoder.updateLeftStick(0.04f, -0.05f)
        assertEquals(0.toShort(), pkt.payload.leftX)
        assertEquals(0.toShort(), pkt.payload.leftY)

        // Max deflection should reach full scale (32767)
        pkt = encoder.updateLeftStick(1.0f, -1.0f)
        assertEquals(32767.toShort(), pkt.payload.leftX)
        assertEquals((-32767).toShort(), pkt.payload.leftY)

        // Values exceeding 1.0f must clamp properly
        pkt = encoder.updateRightStick(2.5f, -3.0f)
        assertEquals(32767.toShort(), pkt.payload.rightX)
        assertEquals((-32767).toShort(), pkt.payload.rightY)
    }

    @Test
    fun testTriggerScaling() {
        // 0.0f -> 0
        var pkt = encoder.updateLeftTrigger(0f)
        assertEquals(0.toByte(), pkt.payload.leftTrigger)

        // 0.5f -> 127
        pkt = encoder.updateLeftTrigger(0.5f)
        assertEquals(127.toByte(), pkt.payload.leftTrigger)

        // 1.0f -> 255 (as signed byte in Java/Kotlin: -1 or (255).toByte())
        pkt = encoder.updateLeftTrigger(1.0f)
        assertEquals(255.toByte(), pkt.payload.leftTrigger)
        assertEquals(255, pkt.payload.leftTrigger.toInt() and 0xFF)

        // Clamping negative and overflow
        pkt = encoder.updateRightTrigger(-0.5f)
        assertEquals(0.toByte(), pkt.payload.rightTrigger)

        pkt = encoder.updateRightTrigger(1.5f)
        assertEquals(255, pkt.payload.rightTrigger.toInt() and 0xFF)
    }

    @Test
    fun testReset() {
        encoder.setButton(GamepadEncoder.BTN_X, true)
        encoder.updateLeftStick(0.9f, 0.9f)
        encoder.updateLeftTrigger(1.0f)

        val pkt = encoder.reset()
        val g = pkt.payload
        assertEquals(0.toShort(), g.buttons)
        assertEquals(0.toShort(), g.leftX)
        assertEquals(0.toShort(), g.leftY)
        assertEquals(0.toByte(), g.leftTrigger)
    }
}
