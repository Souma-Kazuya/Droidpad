package com.droidpad.ai.model

enum class GameGenre(val displayName: String, val icon: String) {
    SOULSLIKE("Soulslike / Action RPG", "⚔️"),
    FIGHTING("Fighting Game", "🥊"),
    RACING("Racing / Driving", "🏎️"),
    FPS("Shooter / FPS", "🎯"),
    PLATFORMER("Platformer / Metroidvania", "🧗"),
    RPG("Open World / RPG", "🛡️"),
    GENERAL("General Controller", "🎮");

    companion object {
        fun fromString(value: String): GameGenre {
            val v = value.lowercase().trim()
            return when {
                v.contains("souls") || v.contains("ring") || v.contains("borne") -> SOULSLIKE
                v.contains("fight") || v.contains("brawl") || v.contains("street") || v.contains("tekken") -> FIGHTING
                v.contains("race") || v.contains("drive") || v.contains("forza") || v.contains("speed") || v.contains("kart") -> RACING
                v.contains("fps") || v.contains("shoot") || v.contains("doom") || v.contains("halo") || v.contains("call of") -> FPS
                v.contains("platform") || v.contains("metroid") || v.contains("knight") || v.contains("celeste") || v.contains("mario") -> PLATFORMER
                v.contains("rpg") || v.contains("witcher") || v.contains("zelda") -> RPG
                else -> GENERAL
            }
        }
    }
}

enum class TipCategory(val displayName: String, val badge: String) {
    COMBOS("Combos & Input Chains", "⚡"),
    TIMING("Timing & Iframes", "⏱️"),
    MAPPING("Controller Layout & Ergonomics", "🎮"),
    PRO_TACTIC("Pro Tactics & Secrets", "💡"),
    TROUBLESHOOTING("Latency & Calibration", "🛠️")
}

data class GameTip(
    val id: String,
    val title: String,
    val tip: String,
    val category: TipCategory,
    val buttons: List<String> = emptyList(),
    val gameTitle: String? = null,
    val genre: GameGenre = GameGenre.GENERAL
)

data class GameProfile(
    val title: String,
    val genre: GameGenre,
    val description: String,
    val defaultTips: List<GameTip>
)
