package com.droidpad.hid

import com.droidpad.protocol.KeyboardPayload
import com.droidpad.protocol.Packet

/**
 * Encodes keyboard input into standard USB HID Usage IDs with 6KRO (6-key rollover).
 */
class KeyboardEncoder {

    // Active pressed HID keycodes (max 6)
    private val pressedKeys = mutableSetOf<Byte>()

    // Modifier bitmask:
    // bit0=LCtrl, bit1=LShift, bit2=LAlt, bit3=LMeta, bit4=RCtrl, bit5=RShift, bit6=RAlt, bit7=RMeta
    var modifiers: Byte = 0
        private set

    @Synchronized
    fun setModifier(mask: Byte, active: Boolean): Packet.Keyboard {
        modifiers = if (active) {
            (modifiers.toInt() or mask.toInt()).toByte()
        } else {
            (modifiers.toInt() and mask.toInt().inv()).toByte()
        }
        return buildPacket()
    }

    @Synchronized
    fun toggleModifier(mask: Byte): Packet.Keyboard {
        modifiers = (modifiers.toInt() xor mask.toInt()).toByte()
        return buildPacket()
    }

    @Synchronized
    fun keyDown(hidCode: Byte): Packet.Keyboard {
        if (pressedKeys.size < 6) {
            pressedKeys.add(hidCode)
        }
        return buildPacket()
    }

    @Synchronized
    fun keyUp(hidCode: Byte): Packet.Keyboard {
        pressedKeys.remove(hidCode)
        return buildPacket()
    }

    @Synchronized
    fun releaseAll(): Packet.Keyboard {
        pressedKeys.clear()
        modifiers = 0
        return buildPacket()
    }

    private fun buildPacket(): Packet.Keyboard {
        val keys = ByteArray(6)
        pressedKeys.take(6).forEachIndexed { index, code ->
            keys[index] = code
        }
        return Packet.Keyboard(KeyboardPayload(modifiers, keys))
    }

    companion object {
        // Modifier masks
        const val MOD_LCTRL: Byte  = 0x01
        const val MOD_LSHIFT: Byte = 0x02
        const val MOD_LALT: Byte   = 0x04
        const val MOD_LMETA: Byte  = 0x08
        const val MOD_RCTRL: Byte  = 0x10
        const val MOD_RSHIFT: Byte = 0x20
        const val MOD_RALT: Byte   = 0x40
        const val MOD_RMETA: Byte  = (0x80).toByte()

        // HID Usage IDs (USB HID Usage Tables 1.3, Section 10)
        const val HID_A: Byte = 0x04
        const val HID_B: Byte = 0x05
        const val HID_C: Byte = 0x06
        const val HID_D: Byte = 0x07
        const val HID_E: Byte = 0x08
        const val HID_F: Byte = 0x09
        const val HID_G: Byte = 0x0A
        const val HID_H: Byte = 0x0B
        const val HID_I: Byte = 0x0C
        const val HID_J: Byte = 0x0D
        const val HID_K: Byte = 0x0E
        const val HID_L: Byte = 0x0F
        const val HID_M: Byte = 0x10
        const val HID_N: Byte = 0x11
        const val HID_O: Byte = 0x12
        const val HID_P: Byte = 0x13
        const val HID_Q: Byte = 0x14
        const val HID_R: Byte = 0x15
        const val HID_S: Byte = 0x16
        const val HID_T: Byte = 0x17
        const val HID_U: Byte = 0x18
        const val HID_V: Byte = 0x19
        const val HID_W: Byte = 0x1A
        const val HID_X: Byte = 0x1B
        const val HID_Y: Byte = 0x1C
        const val HID_Z: Byte = 0x1D

        const val HID_1: Byte = 0x1E
        const val HID_2: Byte = 0x1F
        const val HID_3: Byte = 0x20
        const val HID_4: Byte = 0x21
        const val HID_5: Byte = 0x22
        const val HID_6: Byte = 0x23
        const val HID_7: Byte = 0x24
        const val HID_8: Byte = 0x25
        const val HID_9: Byte = 0x26
        const val HID_0: Byte = 0x27

        const val HID_ENTER: Byte         = 0x28
        const val HID_ESC: Byte           = 0x29
        const val HID_BACKSPACE: Byte     = 0x2A
        const val HID_TAB: Byte           = 0x2B
        const val HID_SPACE: Byte         = 0x2C
        const val HID_MINUS: Byte         = 0x2D
        const val HID_EQUAL: Byte         = 0x2E
        const val HID_LEFT_BRACKET: Byte  = 0x2F
        const val HID_RIGHT_BRACKET: Byte = 0x30
        const val HID_BACKSLASH: Byte     = 0x31
        const val HID_SEMICOLON: Byte     = 0x33
        const val HID_APOSTROPHE: Byte    = 0x34
        const val HID_COMMA: Byte         = 0x36
        const val HID_DOT: Byte           = 0x37
        const val HID_SLASH: Byte         = 0x38
        const val HID_CAPSLOCK: Byte      = 0x39

        const val HID_RIGHT: Byte = 0x4F
        const val HID_LEFT: Byte  = 0x50
        const val HID_DOWN: Byte  = 0x51
        const val HID_UP: Byte    = 0x52

        const val HID_F1: Byte  = 0x3A
        const val HID_F2: Byte  = 0x3B
        const val HID_F3: Byte  = 0x3C
        const val HID_F4: Byte  = 0x3D
        const val HID_F5: Byte  = 0x3E
        const val HID_F6: Byte  = 0x3F
        const val HID_F7: Byte  = 0x40
        const val HID_F8: Byte  = 0x41
        const val HID_F9: Byte  = 0x42
        const val HID_F10: Byte = 0x43
        const val HID_F11: Byte = 0x44
        const val HID_F12: Byte = 0x45
    }
}
