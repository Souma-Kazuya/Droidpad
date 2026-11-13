package com.droidpad.ui.components

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.SportsEsports
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.droidpad.ai.AssistantUiState
import com.droidpad.ai.knowledge.GameKnowledgeBase
import com.droidpad.ai.model.GameGenre
import com.droidpad.ai.model.GameTip
import com.droidpad.ui.theme.*

/**
 * Interactive Offline Mini AI Gamepad Assistant Modal / Drawer.
 * Provides live tips, combo cheat-sheets, game switcher, and an offline conversational assistant.
 */
@Composable
fun AiAssistantDrawer(
    state: AssistantUiState,
    onClose: () -> Unit,
    onSelectGameGenre: (title: String, genre: GameGenre) -> Unit,
    onAskQuery: (String) -> Unit
) {
    var selectedTab by remember { mutableIntStateOf(0) } // 0: Tips & Combos, 1: Ask AI
    var inputQuery by remember { mutableStateOf("") }
    var showGamePicker by remember { mutableStateOf(false) }

    Dialog(
        onDismissRequest = onClose,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.65f))
                .clickable { onClose() },
            contentAlignment = Alignment.Center
        ) {
            Card(
                modifier = Modifier
                    .fillMaxWidth(0.85f)
                    .fillMaxHeight(0.88f)
                    .clickable(enabled = false) {} // Prevent click-through
                    .border(1.dp, AccentMintGreen.copy(alpha = 0.4f), RoundedCornerShape(16.dp)),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = DarkSlateBackground)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp)
                ) {
                    // ── 1. Header Bar: Title, Game Selector, Close ──
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(28.dp)
                                    .clip(CircleShape)
                                    .background(AccentMintGreen.copy(alpha = 0.2f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.AutoAwesome,
                                    contentDescription = null,
                                    tint = AccentMintGreen,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                            Text(
                                text = "Mini AI Gamepad Companion",
                                color = TextWhite,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            // Active Game & Genre Dropdown Button
                            Box {
                                OutlinedButton(
                                    onClick = { showGamePicker = true },
                                    shape = RoundedCornerShape(12.dp),
                                    colors = ButtonDefaults.outlinedButtonColors(
                                        contentColor = AccentMintGreen
                                    ),
                                    border = ButtonDefaults.outlinedButtonBorder.copy(
                                        brush = androidx.compose.ui.graphics.SolidColor(AccentMintGreen.copy(alpha = 0.5f))
                                    ),
                                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                                    modifier = Modifier.height(34.dp)
                                ) {
                                    Text(
                                        text = "${state.activeGenre.icon} ${state.activeGameTitle}",
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                }

                                DropdownMenu(
                                    expanded = showGamePicker,
                                    onDismissRequest = { showGamePicker = false },
                                    modifier = Modifier.background(DarkControlCard)
                                ) {
                                    GameKnowledgeBase.PROFILES.forEach { profile ->
                                        DropdownMenuItem(
                                            text = {
                                                Text(
                                                    text = "${profile.genre.icon} ${profile.title}",
                                                    color = TextWhite,
                                                    fontSize = 13.sp
                                                )
                                            },
                                            onClick = {
                                                onSelectGameGenre(profile.title, profile.genre)
                                                showGamePicker = false
                                            },
                                            trailingIcon = if (state.activeGameTitle == profile.title) {
                                                { Icon(Icons.Default.Check, null, tint = AccentMintGreen) }
                                            } else null
                                        )
                                    }
                                }
                            }

                            // Close Button
                            IconButton(
                                onClick = onClose,
                                modifier = Modifier.size(32.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = "Close",
                                    tint = TextMuted
                                )
                            }
                        }
                    }

                    // Offline status banner
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 6.dp)
                            .clip(RoundedCornerShape(6.dp))
                            .background(DarkControlCard)
                            .padding(horizontal = 10.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "⚡ 100% On-Device NanoLM • 0 Cloud Data Used",
                            color = AccentMintGreen,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium
                        )
                        if (state.inferenceLatencyMs > 0) {
                            Text(
                                text = "Latency: ${state.inferenceLatencyMs}ms",
                                color = TextMuted,
                                fontSize = 11.sp
                            )
                        }
                    }

                    // ── 2. Tab Navigation: Tips vs Ask AI ──
                    TabRow(
                        selectedTabIndex = selectedTab,
                        containerColor = DarkControlCard,
                        contentColor = AccentMintGreen,
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .height(38.dp)
                    ) {
                        Tab(
                            selected = selectedTab == 0,
                            onClick = { selectedTab = 0 },
                            text = { Text("Tips & Combos (${state.availableTips.size})", fontSize = 12.sp, fontWeight = FontWeight.SemiBold) }
                        )
                        Tab(
                            selected = selectedTab == 1,
                            onClick = { selectedTab = 1 },
                            text = { Text("Ask Mini AI (Offline)", fontSize = 12.sp, fontWeight = FontWeight.SemiBold) }
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // ── 3. Tab Content ──
                    if (selectedTab == 0) {
                        // TIPS & COMBOS TAB
                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            items(state.availableTips) { tip ->
                                TipCard(tip = tip)
                            }
                        }
                    } else {
                        // ASK MINI AI TAB
                        Column(
                            modifier = Modifier.fillMaxSize()
                        ) {
                            // Quick Suggestion Prompt Chips
                            val promptSuggestions = listOf(
                                "How to parry?",
                                "Hadouken combo",
                                "Reduce input lag",
                                "Bumper jumper layout",
                                "Pogo jump timing"
                            )

                            LazyRow(
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp)
                            ) {
                                items(promptSuggestions) { chipText ->
                                    FilterChip(
                                        selected = false,
                                        onClick = {
                                            inputQuery = chipText
                                            onAskQuery(chipText)
                                        },
                                        label = { Text(chipText, fontSize = 11.sp) },
                                        colors = FilterChipDefaults.filterChipColors(
                                            containerColor = DarkControlElement,
                                            labelColor = TextWhite
                                        ),
                                        shape = RoundedCornerShape(12.dp)
                                    )
                                }
                            }

                            // Query Input Row
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                OutlinedTextField(
                                    value = inputQuery,
                                    onValueChange = { inputQuery = it },
                                    placeholder = { Text("Ask offline assistant...", fontSize = 12.sp, color = TextMuted) },
                                    modifier = Modifier.weight(1f).height(48.dp),
                                    shape = RoundedCornerShape(10.dp),
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedBorderColor = AccentMintGreen,
                                        unfocusedBorderColor = DarkControlElement,
                                        focusedTextColor = TextWhite,
                                        unfocusedTextColor = TextWhite
                                    ),
                                    singleLine = true
                                )

                                Button(
                                    onClick = {
                                        if (inputQuery.isNotBlank()) {
                                            onAskQuery(inputQuery)
                                        }
                                    },
                                    enabled = inputQuery.isNotBlank() && !state.isStreaming,
                                    shape = RoundedCornerShape(10.dp),
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = AccentMintGreen,
                                        contentColor = TextDark
                                    ),
                                    modifier = Modifier.height(48.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.AutoMirrored.Filled.Send,
                                        contentDescription = "Send",
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(10.dp))

                            // Response Area
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .weight(1f),
                                shape = RoundedCornerShape(10.dp),
                                colors = CardDefaults.cardColors(containerColor = DarkControlCard)
                            ) {
                                LazyColumn(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .padding(12.dp)
                                ) {
                                    if (state.lastResponse.isNotBlank()) {
                                        item {
                                            if (state.lastQuery.isNotBlank()) {
                                                Text(
                                                    text = "Q: ${state.lastQuery}",
                                                    color = AccentMintGreen,
                                                    fontSize = 12.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    modifier = Modifier.padding(bottom = 6.dp)
                                                )
                                            }
                                            Text(
                                                text = state.lastResponse,
                                                color = TextWhite,
                                                fontSize = 13.sp,
                                                lineHeight = 18.sp
                                            )
                                        }
                                    } else if (state.isStreaming) {
                                        item {
                                            Text(
                                                text = "Thinking locally on device...",
                                                color = AccentMintGreen,
                                                fontSize = 13.sp
                                            )
                                        }
                                    } else {
                                        item {
                                            Column(
                                                modifier = Modifier.fillMaxWidth().padding(top = 24.dp),
                                                horizontalAlignment = Alignment.CenterHorizontally
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Default.SportsEsports,
                                                    contentDescription = null,
                                                    tint = TextMuted,
                                                    modifier = Modifier.size(36.dp)
                                                )
                                                Spacer(modifier = Modifier.height(8.dp))
                                                Text(
                                                    text = "Tap a prompt chip or type a question above.",
                                                    color = TextMuted,
                                                    fontSize = 12.sp
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun TipCard(tip: GameTip) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(10.dp),
        colors = CardDefaults.cardColors(containerColor = DarkControlCard)
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(text = tip.category.badge, fontSize = 14.sp)
                    Text(
                        text = tip.title,
                        color = TextWhite,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                Text(
                    text = tip.category.displayName,
                    color = AccentMintGreen,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Medium
                )
            }

            Text(
                text = tip.tip,
                color = TextMuted,
                fontSize = 12.sp,
                lineHeight = 16.sp
            )

            if (tip.buttons.isNotEmpty()) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(top = 2.dp)
                ) {
                    Text(
                        text = "Inputs:",
                        color = TextMuted,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                    tip.buttons.forEach { btn ->
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(4.dp))
                                .background(DarkControlElement)
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = btn,
                                color = AccentMintGreen,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }
    }
}
