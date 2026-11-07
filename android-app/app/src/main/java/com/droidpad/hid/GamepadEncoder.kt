package com.droidpad.hid

import com.droidpad.protocol.GamepadPayload
import com.droidpad.protocol.Packet
import kotlin.math.abs

/**
 * Encodes virtual gamepad controls into standard Xbox 360 controller reports.
 */
class GamepadEncoder {

    private var buttonMask: Short = 0
    private var leftX: Short = 0
    private var leftY: Short = 0
    private var rightX: Short = 0
    private var rightY: Short = 0
    private var leftTrigger: Byte = 0
    private var rightTrigger: Byte = 0

    var deadzone: Float = 0.08f // 8% default deadzone

    @Synchronized
    fun setButton(mask: Short, pressed: Boolean): Packet.Gamepad {
        buttonMask = if (pressed) {
            (buttonMask.toInt() or mask.toInt()).toShort()
        } else {
            (buttonMask.toInt() and mask.toInt().inv()).toShort()
        }
        return buildPacket()
    }

    @Synchronized
    fun updateLeftStick(x: Float, y: Float): Packet.Gamepad {
        leftX = applyDeadzoneAndScale(x)
        // Invert Y so up is negative on evdev stick axis if desired, or standard screen coords
        leftY = applyDeadzoneAndScale(y)
        return buildPacket()
    }

    @Synchronized
    fun updateRightStick(x: Float, y: Float): Packet.Gamepad {
        rightX = applyDeadzoneAndScale(x)
        rightY = applyDeadzoneAndScale(y)
        return buildPacket()
    }

    @Synchronized
    fun updateLeftTrigger(value: Float): Packet.Gamepad {
        // Value in [0f..1f] scaled to [0..255]
        val clamped = value.coerceIn(0f, 1f)
        leftTrigger = (clamped * 255).toInt().toByte()
        return buildPacket()
    }

    @Synchronized
    fun updateRightTrigger(value: Float): Packet.Gamepad {
        val clamped = value.coerceIn(0f, 1f)
        rightTrigger = (clamped * 255).toInt().toByte()
        return buildPacket()
    }

    @Synchronized
    fun reset(): Packet.Gamepad {
        buttonMask = 0
        leftX = 0
        leftY = 0
        rightX = 0
        rightY = 0
        leftTrigger = 0
        rightTrigger = 0
        return buildPacket()
    }

    private fun applyDeadzoneAndScale(value: Float): Short {
        val clamped = value.coerceIn(-1f, 1f)
        if (abs(clamped) < deadzone) return 0
        // Rescale remaining range to 0..1
        val sign = if (clamped < 0) -1f else 1f
        val scaled = (abs(clamped) - deadzone) / (1f - deadzone)
        val finalVal = (sign * scaled * 32767).toInt()
        return finalVal.coerceIn(-32768, 32767).toShort()
    }

    private fun buildPacket(): Packet.Gamepad {
        return Packet.Gamepad(
            GamepadPayload(
                buttons = buttonMask,
                leftX = leftX,
                leftY = leftY,
                rightX = rightX,
                rightY = rightY,
                leftTrigger = leftTrigger,
                rightTrigger = rightTrigger
            )
        )
    }

    companion object {
        const val BTN_A: Short      = (1 shl 0).toShort()
        const val BTN_B: Short      = (1 shl 1).toShort()
        const val BTN_X: Short      = (1 shl 2).toShort()
        const val BTN_Y: Short      = (1 shl 3).toShort()
        const val BTN_LB: Short     = (1 shl 4).toShort()
        const val BTN_RB: Short     = (1 shl 5).toShort()
        const val BTN_START: Short  = (1 shl 6).toShort()
        const val BTN_SELECT: Short = (1 shl 7).toShort()
        const val BTN_L3: Short     = (1 shl 8).toShort()
        const val BTN_R3: Short     = (1 shl 9).toShort()
        const val DPAD_UP: Short    = (1 shl 10).toShort()
        const val DPAD_DOWN: Short  = (1 shl 11).toShort()
        const val DPAD_LEFT: Short  = (1 shl 12).toShort()
        const val DPAD_RIGHT: Short = (1 shl 13).toShort()
        const val BTN_GUIDE: Short  = (1 shl 14).toShort()
    }
}
