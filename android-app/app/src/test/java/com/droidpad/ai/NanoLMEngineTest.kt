package com.droidpad.ai

import com.droidpad.ai.engine.NanoLMEngine
import com.droidpad.ai.knowledge.GameKnowledgeBase
import com.droidpad.ai.model.GameGenre
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test

class NanoLMEngineTest {

    private val engine = NanoLMEngine()

    @Test
    fun testOfflineInferenceParryQuery() = runBlocking {
        val result = engine.query(
            query = "How do I parry reliably?",
            gameTitle = "Elden Ring",
            genre = GameGenre.SOULSLIKE
        )

        assertTrue("Should be offline", result.isOffline)
        assertTrue("Latency should be under 500ms", result.latencyMs < 500)
        assertTrue("Response should contain parry advice", result.response.contains("Parry", ignoreCase = true))
        assertTrue("Response should mention buttons or timing", result.response.contains("LT") || result.response.contains("timing", ignoreCase = true))
    }

    @Test
    fun testOfflineInferenceHadoukenQuery() = runBlocking {
        val result = engine.query(
            query = "How to do a hadouken fireball combo?",
            gameTitle = "Street Fighter",
            genre = GameGenre.FIGHTING
        )

        assertTrue(result.isOffline)
        assertTrue("Response should mention D-Pad or quarter-circle", result.response.contains("D-Pad") || result.response.contains("Hadouken"))
    }

    @Test
    fun testOfflineStreamingTokens() = runBlocking {
        val tokens = engine.streamQueryResponse(
            query = "How to reduce input lag?",
            gameTitle = "Universal",
            genre = GameGenre.GENERAL
        ).toList()

        assertTrue("Tokens should be streamed", tokens.isNotEmpty())
        val fullText = tokens.joinToString("")
        assertTrue("Full text should discuss latency/USB/Wi-Fi", fullText.contains("USB") || fullText.contains("Latency"))
    }

    @Test
    fun testGameKnowledgeBaseProfileRetrieval() {
        val soulslikeProfile = GameKnowledgeBase.getProfileForGame("Dark Souls III", "soulslike")
        assertEquals(GameGenre.SOULSLIKE, soulslikeProfile.genre)

        val fightingProfile = GameKnowledgeBase.getProfileForGame("Tekken 8", "fighting")
        assertEquals(GameGenre.FIGHTING, fightingProfile.genre)

        val fallbackProfile = GameKnowledgeBase.getProfileForGame("RandomNonExistentGame", "unknown")
        assertEquals(GameGenre.GENERAL, fallbackProfile.genre)
    }

    @Test
    fun testGameAssistantManagerTipCycling() = runBlocking {
        val manager = GameAssistantManager(this)
        try {
            manager.setGameContext("Elden Ring", GameGenre.SOULSLIKE)

            val initialTip = manager.uiState.value.currentTip
            assertNotNull(initialTip)

            manager.nextTip()
            val nextTip = manager.uiState.value.currentTip
            assertNotNull(nextTip)
            assertNotEquals(initialTip?.id, nextTip?.id)

            manager.prevTip()
            assertEquals(initialTip?.id, manager.uiState.value.currentTip?.id)
        } finally {
            manager.stop()
        }
    }
}
