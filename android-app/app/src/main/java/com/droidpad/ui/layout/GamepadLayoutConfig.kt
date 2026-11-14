package com.droidpad.ui.layout

import android.content.Context
import android.content.SharedPreferences
import androidx.compose.runtime.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * ABXY layout button mapping style.
 */
enum class AbxyStyle {
    /** Standard Xbox: Y top, X left, B right, A bottom */
    XBOX,
    /** Nintendo Switch: X top, Y left, A right, B bottom */
    NINTENDO,
    /** PlayStation geometry: Triangle top, Square left, Circle right, Cross bottom */
    PLAYSTATION
}

/**
 * Joystick position placement arrangement.
 */
enum class StickArrangement {
    /** Xbox Asymmetric: Left stick high, D-Pad low; ABXY high, Right stick low */
    ASYMMETRIC_XBOX,
    /** PlayStation Symmetrical: D-Pad & ABXY high; Left stick & Right stick both low */
    SYMMETRIC_BOTTOM,
    /** Swapped: D-Pad high, Left stick low; Right stick high, ABXY low */
    INVERTED_STICKS
}

/**
 * Full controller layout preferences.
 */
data class GamepadLayoutPreferences(
    val abxyStyle: AbxyStyle = AbxyStyle.XBOX,
    val stickArrangement: StickArrangement = StickArrangement.ASYMMETRIC_XBOX,
    val swapSticks: Boolean = false,
    val joystickScale: Float = 1.0f,
    val buttonScale: Float = 1.0f
)

/**
 * Repository to persist and observe controller layout configurations.
 */
class GamepadLayoutManager(context: Context) {
    private val prefs: SharedPreferences =
        context.getSharedPreferences("droidpad_layout_prefs", Context.MODE_PRIVATE)

    private val _layoutState = MutableStateFlow(loadPreferences())
    val layoutState: StateFlow<GamepadLayoutPreferences> = _layoutState.asStateFlow()

    private fun loadPreferences(): GamepadLayoutPreferences {
        val abxyName = prefs.getString(KEY_ABXY_STYLE, AbxyStyle.XBOX.name) ?: AbxyStyle.XBOX.name
        val stickName = prefs.getString(KEY_STICK_ARRANGEMENT, StickArrangement.ASYMMETRIC_XBOX.name)
            ?: StickArrangement.ASYMMETRIC_XBOX.name
        val swap = prefs.getBoolean(KEY_SWAP_STICKS, false)
        val jScale = prefs.getFloat(KEY_JOYSTICK_SCALE, 1.0f)
        val bScale = prefs.getFloat(KEY_BUTTON_SCALE, 1.0f)

        return GamepadLayoutPreferences(
            abxyStyle = runCatching { AbxyStyle.valueOf(abxyName) }.getOrDefault(AbxyStyle.XBOX),
            stickArrangement = runCatching { StickArrangement.valueOf(stickName) }.getOrDefault(StickArrangement.ASYMMETRIC_XBOX),
            swapSticks = swap,
            joystickScale = jScale,
            buttonScale = bScale
        )
    }

    fun updateAbxyStyle(style: AbxyStyle) {
        prefs.edit().putString(KEY_ABXY_STYLE, style.name).apply()
        _layoutState.value = _layoutState.value.copy(abxyStyle = style)
    }

    fun updateStickArrangement(arrangement: StickArrangement) {
        prefs.edit().putString(KEY_STICK_ARRANGEMENT, arrangement.name).apply()
        _layoutState.value = _layoutState.value.copy(stickArrangement = arrangement)
    }

    fun updateSwapSticks(swap: Boolean) {
        prefs.edit().putBoolean(KEY_SWAP_STICKS, swap).apply()
        _layoutState.value = _layoutState.value.copy(swapSticks = swap)
    }

    fun updateScales(joystickScale: Float, buttonScale: Float) {
        prefs.edit()
            .putFloat(KEY_JOYSTICK_SCALE, joystickScale)
            .putFloat(KEY_BUTTON_SCALE, buttonScale)
            .apply()
        _layoutState.value = _layoutState.value.copy(
            joystickScale = joystickScale,
            buttonScale = buttonScale
        )
    }

    companion object {
        private const val KEY_ABXY_STYLE = "abxy_style"
        private const val KEY_STICK_ARRANGEMENT = "stick_arrangement"
        private const val KEY_SWAP_STICKS = "swap_sticks"
        private const val KEY_JOYSTICK_SCALE = "joystick_scale"
        private const val KEY_BUTTON_SCALE = "button_scale"

        @Volatile
        private var instance: GamepadLayoutManager? = null

        fun getInstance(context: Context): GamepadLayoutManager {
            return instance ?: synchronized(this) {
                instance ?: GamepadLayoutManager(context.applicationContext).also { instance = it }
            }
        }
    }
}
