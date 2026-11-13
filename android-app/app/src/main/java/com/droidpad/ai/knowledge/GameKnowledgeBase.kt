package com.droidpad.ai.knowledge

import com.droidpad.ai.model.GameGenre
import com.droidpad.ai.model.GameProfile
import com.droidpad.ai.model.GameTip
import com.droidpad.ai.model.TipCategory

/**
 * Embedded offline knowledge base for the Mini AI Gamepad Assistant.
 * Provides rich, verified controller tips, combo inputs, and genre mechanics
 * without any external internet or cloud access.
 */
object GameKnowledgeBase {

    val PROFILES: List<GameProfile> = listOf(
        GameProfile(
            title = "Elden Ring / Souls",
            genre = GameGenre.SOULSLIKE,
            description = "Third-person action RPG requiring precise roll iframes and posture breaks.",
            defaultTips = listOf(
                GameTip(
                    id = "el_1",
                    title = "Invulnerability Frame Roll",
                    tip = "Medium equip load provides 13 iframes (0.43s). Roll *into* or *past* forward attacks rather than away to punish enemy recovery animations.",
                    category = TipCategory.TIMING,
                    buttons = listOf("B", "Left Stick"),
                    gameTitle = "Elden Ring",
                    genre = GameGenre.SOULSLIKE
                ),
                GameTip(
                    id = "el_2",
                    title = "Jump Heavy Attack Stance Break",
                    tip = "Jump with [A] and immediately follow up with Heavy Attack [RT]. This deals massive poise damage and quickly staggers bosses for critical hits.",
                    category = TipCategory.COMBOS,
                    buttons = listOf("A", "RT"),
                    gameTitle = "Elden Ring",
                    genre = GameGenre.SOULSLIKE
                ),
                GameTip(
                    id = "el_3",
                    title = "Parry Startup Timing",
                    tip = "Small shields and Buckler parry active frames start around frame 4. Press [LT] when the enemy weapon hand starts its downward swing, not at the wind-up peak.",
                    category = TipCategory.TIMING,
                    buttons = listOf("LT"),
                    gameTitle = "Elden Ring",
                    genre = GameGenre.SOULSLIKE
                ),
                GameTip(
                    id = "el_4",
                    title = "Claw-Grip Alternative for Sprinting",
                    tip = "Remap Sprint/Dodge to [LB] or [L3] in game settings so you can steer your camera with the Right Stick while running at full speed.",
                    category = TipCategory.MAPPING,
                    buttons = listOf("LB", "Right Stick"),
                    gameTitle = "Elden Ring",
                    genre = GameGenre.SOULSLIKE
                )
            )
        ),
        GameProfile(
            title = "Street Fighter / Fighting",
            genre = GameGenre.FIGHTING,
            description = "High-precision 2D/3D fighting game with quarter-circle and dragon punch inputs.",
            defaultTips = listOf(
                GameTip(
                    id = "fg_1",
                    title = "Quarter-Circle Forward (Hadouken)",
                    tip = "Glide your thumb smoothly across the D-Pad from Down (↓) to Down-Forward (↘) to Forward (→), then press Punch [X]. Use D-Pad instead of Analog Stick for clean inputs.",
                    category = TipCategory.COMBOS,
                    buttons = listOf("D-Pad ↓↘→", "X"),
                    gameTitle = "Street Fighter",
                    genre = GameGenre.FIGHTING
                ),
                GameTip(
                    id = "fg_2",
                    title = "Dragon Punch / Shoryuken Motion",
                    tip = "Input Forward (→), Down (↓), Down-Forward (↘) + Punch [Y]. Shortcut trick: Double tap Down-Forward diagonally on D-Pad for instant anti-air response.",
                    category = TipCategory.COMBOS,
                    buttons = listOf("D-Pad →↓↘", "Y"),
                    gameTitle = "Street Fighter",
                    genre = GameGenre.FIGHTING
                ),
                GameTip(
                    id = "fg_3",
                    title = "Anti-Air Reversal Timing",
                    tip = "Buffer your DP motion while blocking crouched. As soon as the opponent reaches the apex of their jump-in, hit the punch button to execute an invulnerable wake-up.",
                    category = TipCategory.TIMING,
                    buttons = listOf("D-Pad ↘", "Y"),
                    gameTitle = "Street Fighter",
                    genre = GameGenre.FIGHTING
                ),
                GameTip(
                    id = "fg_4",
                    title = "Drive Rush / Dash Cancel",
                    tip = "Tap Forward twice (→ →) or use Parry shortcut [MP+MK] to immediately cancel normals into high-speed offensive pressure.",
                    category = TipCategory.PRO_TACTIC,
                    buttons = listOf("D-Pad →→", "LB"),
                    gameTitle = "Street Fighter",
                    genre = GameGenre.FIGHTING
                )
            )
        ),
        GameProfile(
            title = "Forza / Racing",
            genre = GameGenre.RACING,
            description = "Simulation & arcade racing requiring analog throttle control and weight transfer.",
            defaultTips = listOf(
                GameTip(
                    id = "rc_1",
                    title = "Analog Trigger Throttle Feathering",
                    tip = "Do not slam [RT] to 100% when exiting hairpins. Ease into 30% -> 60% -> 100% throttle to avoid breaking rear traction and spinning out.",
                    category = TipCategory.TIMING,
                    buttons = listOf("RT (Analog)"),
                    gameTitle = "Forza",
                    genre = GameGenre.RACING
                ),
                GameTip(
                    id = "rc_2",
                    title = "Trail Braking on Corner Entry",
                    tip = "Brake hard with [LT] in a straight line before the corner, then gradually release brake pressure as you turn the Left Stick toward the apex to keep front tires loaded.",
                    category = TipCategory.PRO_TACTIC,
                    buttons = listOf("LT", "Left Stick"),
                    gameTitle = "Forza",
                    genre = GameGenre.RACING
                ),
                GameTip(
                    id = "rc_3",
                    title = "Rocket League Fast Aerial",
                    tip = "Hold Left Stick Down, Jump [A] + Boost [B] simultaneously, release stick to neutral, Jump [A] again while tilting up. Gets you airborne in half the normal time.",
                    category = TipCategory.COMBOS,
                    buttons = listOf("Left Stick ↓", "A", "B"),
                    gameTitle = "Rocket League",
                    genre = GameGenre.RACING
                ),
                GameTip(
                    id = "rc_4",
                    title = "Steering Stick Deadzone Setup",
                    tip = "In game controller options, set steering inside deadzone to 2-5% and outside deadzone to 95%. This eliminates center slack without accidental twitching.",
                    category = TipCategory.MAPPING,
                    buttons = listOf("Left Stick"),
                    gameTitle = "Forza",
                    genre = GameGenre.RACING
                )
            )
        ),
        GameProfile(
            title = "Hollow Knight / Platformer",
            genre = GameGenre.PLATFORMER,
            description = "Precision platformers and metroidvanias with aerial pogo and dash chaining.",
            defaultTips = listOf(
                GameTip(
                    id = "pl_1",
                    title = "Down-Slash Pogo Jump",
                    tip = "While airborne, hold Down on D-Pad/Stick and press Attack [X]. Slashing enemies, spikes, or bouncy mushrooms resets your double jump and dash!",
                    category = TipCategory.COMBOS,
                    buttons = listOf("D-Pad ↓", "X"),
                    gameTitle = "Hollow Knight",
                    genre = GameGenre.PLATFORMER
                ),
                GameTip(
                    id = "pl_2",
                    title = "Dash Pogo Chain",
                    tip = "Execute: Jump [A] -> Dash [RT] -> Down-Slash [↓ + X] -> Air Dash [RT]. Allows traversing entire rooms without ever touching hazardous ground.",
                    category = TipCategory.COMBOS,
                    buttons = listOf("A", "RT", "D-Pad ↓", "X"),
                    gameTitle = "Hollow Knight",
                    genre = GameGenre.PLATFORMER
                ),
                GameTip(
                    id = "pl_3",
                    title = "Celeste Hyperdash Wave",
                    tip = "Crouch on ground with [↓], Dash diagonally down-forward [↘ + RT], and hit Jump [A] immediately. You preserve full speed while restoring your air dash.",
                    category = TipCategory.PRO_TACTIC,
                    buttons = listOf("D-Pad ↘", "RT", "A"),
                    gameTitle = "Celeste",
                    genre = GameGenre.PLATFORMER
                ),
                GameTip(
                    id = "pl_4",
                    title = "Attack & Jump Separation",
                    tip = "Keep Attack on [X] and Jump on [A] so your right thumb pad can press [A] with the lower knuckle while the tip of your thumb taps [X].",
                    category = TipCategory.MAPPING,
                    buttons = listOf("A", "X"),
                    gameTitle = "Hollow Knight",
                    genre = GameGenre.PLATFORMER
                )
            )
        ),
        GameProfile(
            title = "Doom / FPS",
            genre = GameGenre.FPS,
            description = "Fast-paced first person shooter with mobility and precision aiming.",
            defaultTips = listOf(
                GameTip(
                    id = "fps_1",
                    title = "Bumper Jumper Layout",
                    tip = "Remap Jump to [LB] instead of [A]. This allows you to jump, slide, and bunny-hop continuously without ever taking your right thumb off the aiming stick.",
                    category = TipCategory.MAPPING,
                    buttons = listOf("LB", "Right Stick"),
                    gameTitle = "Doom / FPS",
                    genre = GameGenre.FPS
                ),
                GameTip(
                    id = "fps_2",
                    title = "Doom Eternal Quick-Switch Combo",
                    tip = "Super Shotgun [RT] -> Quick Swap [LB] -> Ballista [RT] -> Quick Swap [LB] -> Super Shotgun [RT]. Quadruples your burst DPS against Marauders and Super Heavies.",
                    category = TipCategory.COMBOS,
                    buttons = listOf("RT", "LB", "RT"),
                    gameTitle = "Doom Eternal",
                    genre = GameGenre.FPS
                ),
                GameTip(
                    id = "fps_3",
                    title = "Apex Slide-Jump Velocity Boost",
                    tip = "Sprint with [L3], press Crouch [B] to slide on a downward incline, and press Jump [LB/A] at the crest of the slide for 30% increased travel distance.",
                    category = TipCategory.COMBOS,
                    buttons = listOf("L3", "B", "A"),
                    gameTitle = "Apex Legends",
                    genre = GameGenre.FPS
                ),
                GameTip(
                    id = "fps_4",
                    title = "Trigger Hairline Sensitivity",
                    tip = "DroidPad digital triggers fire on touch. Use this instant trigger travel to shoot semi-auto marksman rifles at max possible fire rate.",
                    category = TipCategory.PRO_TACTIC,
                    buttons = listOf("RT"),
                    gameTitle = "Doom / FPS",
                    genre = GameGenre.FPS
                )
            )
        ),
        GameProfile(
            title = "Universal Controller Optimization",
            genre = GameGenre.GENERAL,
            description = "Latency, ergonomics, and calibration tricks for all games.",
            defaultTips = listOf(
                GameTip(
                    id = "gen_1",
                    title = "Sub-5ms Latency via USB / 5GHz Wi-Fi",
                    tip = "For competitive games, connect over USB (ADB tunnel) or a 5GHz Wi-Fi channel with direct line-of-sight to keep input transmission under 4ms.",
                    category = TipCategory.TROUBLESHOOTING,
                    buttons = listOf("USB", "Wi-Fi 5GHz"),
                    genre = GameGenre.GENERAL
                ),
                GameTip(
                    id = "gen_2",
                    title = "Multi-Touch Thumb Placement",
                    tip = "Rest the ball of your right thumb between [X] and [A]. You can roll your thumb forward for [X] and down for [A] without lifting off the screen.",
                    category = TipCategory.MAPPING,
                    buttons = listOf("A", "B", "X", "Y"),
                    genre = GameGenre.GENERAL
                ),
                GameTip(
                    id = "gen_3",
                    title = "Haptic Rumble Feedback Optimization",
                    tip = "DroidPad translates host rumble packets to your phone's linear resonant actuator (LRA). Feeling the rumble peak tells you when shields or stances break.",
                    category = TipCategory.PRO_TACTIC,
                    buttons = listOf("Haptics"),
                    genre = GameGenre.GENERAL
                ),
                GameTip(
                    id = "gen_4",
                    title = "Zero-Deadzone Drift Prevention",
                    tip = "Virtual touch joysticks have 0 physical spring wear and 0 mechanical drift. You can set in-game stick deadzones to pure 0% for instant responsiveness.",
                    category = TipCategory.TROUBLESHOOTING,
                    buttons = listOf("Left Stick", "Right Stick"),
                    genre = GameGenre.GENERAL
                )
            )
        )
    )

    fun getProfileForGame(gameTitle: String?, genreHint: String? = null): GameProfile {
        if (!gameTitle.isNullOrBlank()) {
            val lower = gameTitle.lowercase()
            val match = PROFILES.firstOrNull { profile ->
                profile.title.lowercase().contains(lower) ||
                profile.defaultTips.any { it.gameTitle?.lowercase()?.contains(lower) == true }
            }
            if (match != null) return match
        }

        if (!genreHint.isNullOrBlank()) {
            val genre = GameGenre.fromString(genreHint)
            val match = PROFILES.firstOrNull { it.genre == genre }
            if (match != null) return match
        }

        return PROFILES.last() // Universal Controller Optimization
    }

    fun getAllTipsForGenre(genre: GameGenre): List<GameTip> {
        val specific = PROFILES.filter { it.genre == genre }.flatMap { it.defaultTips }
        val general = PROFILES.last().defaultTips
        return (specific + general).distinctBy { it.id }
    }
}
