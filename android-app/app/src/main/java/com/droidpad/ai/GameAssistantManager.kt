package com.droidpad.ai

import com.droidpad.ai.engine.NanoLMEngine
import com.droidpad.ai.knowledge.GameKnowledgeBase
import com.droidpad.ai.model.GameGenre
import com.droidpad.ai.model.GameProfile
import com.droidpad.ai.model.GameTip
import com.droidpad.protocol.GameContextPayload
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*

data class AssistantUiState(
    val activeGameTitle: String = "Universal Controller",
    val activeGenre: GameGenre = GameGenre.GENERAL,
    val currentTip: GameTip? = null,
    val availableTips: List<GameTip> = emptyList(),
    val currentTipIndex: Int = 0,
    val lastQuery: String = "",
    val lastResponse: String = "",
    val isStreaming: Boolean = false,
    val isDrawerOpen: Boolean = false,
    val inferenceLatencyMs: Long = 0,
    val isOfflineModelActive: Boolean = true
)

class GameAssistantManager(
    private val scope: CoroutineScope
) {
    val engine = NanoLMEngine()

    private val _uiState = MutableStateFlow(
        AssistantUiState(
            availableTips = GameKnowledgeBase.PROFILES.last().defaultTips,
            currentTip = GameKnowledgeBase.PROFILES.last().defaultTips.firstOrNull()
        )
    )
    val uiState: StateFlow<AssistantUiState> = _uiState.asStateFlow()

    private var autoRotateJob: Job? = null
    private var queryJob: Job? = null

    init {
        startAutoRotateTips()
    }

    /**
     * Updates game context when received from host daemon via 0x06 packet.
     */
    fun onHostGameContextReceived(context: GameContextPayload?) {
        if (context == null) return
        setGameContext(context.gameTitle, GameGenre.fromString(context.genre))
    }

    /**
     * Set active game and genre (either from host or manual user selection).
     */
    fun setGameContext(gameTitle: String, genre: GameGenre) {
        val profile = GameKnowledgeBase.getProfileForGame(gameTitle, genre.name)
        val tips = profile.defaultTips.ifEmpty {
            GameKnowledgeBase.getAllTipsForGenre(genre)
        }

        _uiState.update { current ->
            current.copy(
                activeGameTitle = gameTitle.ifBlank { profile.title },
                activeGenre = genre,
                availableTips = tips,
                currentTip = tips.firstOrNull(),
                currentTipIndex = 0
            )
        }
    }

    fun nextTip() {
        val state = _uiState.value
        if (state.availableTips.isEmpty()) return
        val nextIdx = (state.currentTipIndex + 1) % state.availableTips.size
        _uiState.update { it.copy(currentTipIndex = nextIdx, currentTip = state.availableTips[nextIdx]) }
    }

    fun prevTip() {
        val state = _uiState.value
        if (state.availableTips.isEmpty()) return
        val prevIdx = if (state.currentTipIndex - 1 < 0) state.availableTips.size - 1 else state.currentTipIndex - 1
        _uiState.update { it.copy(currentTipIndex = prevIdx, currentTip = state.availableTips[prevIdx]) }
    }

    fun setDrawerOpen(open: Boolean) {
        _uiState.update { it.copy(isDrawerOpen = open) }
    }

    /**
     * Ask the on-device Mini AI a question with streaming token output.
     */
    fun askAssistant(queryText: String) {
        if (queryText.isBlank()) return
        queryJob?.cancel()

        queryJob = scope.launch(Dispatchers.Default) {
            _uiState.update {
                it.copy(
                    lastQuery = queryText,
                    lastResponse = "",
                    isStreaming = true
                )
            }

            val startTime = System.currentTimeMillis()
            val state = _uiState.value
            val responseBuilder = StringBuilder()

            engine.streamQueryResponse(queryText, state.activeGameTitle, state.activeGenre)
                .collect { token ->
                    responseBuilder.append(token)
                    _uiState.update {
                        it.copy(lastResponse = responseBuilder.toString())
                    }
                }

            val duration = System.currentTimeMillis() - startTime
            _uiState.update {
                it.copy(
                    isStreaming = false,
                    inferenceLatencyMs = duration
                )
            }
        }
    }

    fun stop() {
        autoRotateJob?.cancel()
        autoRotateJob = null
        queryJob?.cancel()
        queryJob = null
    }

    private fun startAutoRotateTips() {
        autoRotateJob?.cancel()
        autoRotateJob = scope.launch(Dispatchers.Default) {
            while (isActive) {
                delay(12000) // Rotate tip every 12 seconds
                if (!_uiState.value.isDrawerOpen) {
                    nextTip()
                }
            }
        }
    }
}
