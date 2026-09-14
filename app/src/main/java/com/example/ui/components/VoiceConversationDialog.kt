package com.example.ui.components

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Park
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.ui.theme.LeafDarkBackground
import com.example.ui.theme.LeafGreenAccent
import com.example.ui.theme.LeafGreenLight
import com.example.ui.theme.LeafGreenSecondary
import com.example.ui.theme.LeafGreenTertiary

enum class VoiceBotState {
    IDLE,
    LISTENING,
    THINKING,
    SPEAKING
}

@Composable
fun VoiceConversationDialog(
    botState: VoiceBotState,
    transcript: String,
    aiResponse: String,
    onStartListening: () -> Unit,
    onStopListening: () -> Unit,
    onInterruptSpeech: () -> Unit,
    onDismiss: () -> Unit
) {
    var selectedLanguage by remember { mutableStateOf("English (US)") }
    val languages = listOf("English (US)", "Spanish", "French", "German", "Hindi", "Mandarin")

    // Multi-ring breathing/pulsing animation
    val infiniteTransition = rememberInfiniteTransition(label = "voice_orb")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 0.95f,
        targetValue = if (botState == VoiceBotState.LISTENING || botState == VoiceBotState.SPEAKING) 1.25f else 1.05f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse_scale"
    )

    val outerRingAlpha by infiniteTransition.animateFloat(
        initialValue = 0.2f,
        targetValue = if (botState != VoiceBotState.IDLE) 0.6f else 0.3f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "outer_ring_alpha"
    )

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxSize()
                .testTag("voice_to_voice_dialog"),
            color = LeafDarkBackground
        ) {
            Box(modifier = Modifier.fillMaxSize()) {
                // Falling leaves in background for voice mode
                FallingLeavesCanvas(leafCount = 12, enabled = true)

                // Top bar with branding and close button
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 48.dp, start = 20.dp, end = 20.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Park,
                            contentDescription = "SOLVE AI Voice",
                            tint = LeafGreenAccent,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "SOLVE AI Voice Mode",
                            style = MaterialTheme.typography.titleMedium,
                            color = Color.White,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(Color(0xFF1E3A2B))
                            .testTag("close_voice_dialog_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Close Voice Mode",
                            tint = Color.White,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }

                // Center: Animated Interactive Audio Orb
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 28.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    // Status Badge
                    Surface(
                        color = when (botState) {
                            VoiceBotState.LISTENING -> LeafGreenAccent.copy(alpha = 0.2f)
                            VoiceBotState.THINKING -> Color(0xFFF4A261).copy(alpha = 0.2f)
                            VoiceBotState.SPEAKING -> LeafGreenLight.copy(alpha = 0.2f)
                            VoiceBotState.IDLE -> Color(0xFF1E3A2B)
                        },
                        shape = RoundedCornerShape(20.dp),
                        modifier = Modifier.padding(bottom = 36.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.GraphicEq,
                                contentDescription = "Audio activity",
                                tint = LeafGreenAccent,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = when (botState) {
                                    VoiceBotState.LISTENING -> "Listening with Whisper STT..."
                                    VoiceBotState.THINKING -> "Thinking & Reasoning..."
                                    VoiceBotState.SPEAKING -> "Speaking natural voice..."
                                    VoiceBotState.IDLE -> "Tap orb to speak"
                                },
                                style = MaterialTheme.typography.labelMedium,
                                color = Color.White,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }

                    // Pulsing Voice Orb
                    Box(
                        modifier = Modifier
                            .size(240.dp)
                            .clickable {
                                when (botState) {
                                    VoiceBotState.IDLE -> onStartListening()
                                    VoiceBotState.LISTENING -> onStopListening()
                                    VoiceBotState.SPEAKING -> onInterruptSpeech()
                                    VoiceBotState.THINKING -> {}
                                }
                            }
                            .testTag("voice_orb_button"),
                        contentAlignment = Alignment.Center
                    ) {
                        // Outer Halo
                        Box(
                            modifier = Modifier
                                .size(240.dp)
                                .scale(pulseScale)
                                .clip(CircleShape)
                                .background(
                                    Brush.radialGradient(
                                        listOf(
                                            LeafGreenAccent.copy(alpha = outerRingAlpha),
                                            Color.Transparent
                                        )
                                    )
                                )
                        )

                        // Middle Glow
                        Box(
                            modifier = Modifier
                                .size(170.dp)
                                .scale(if (botState == VoiceBotState.LISTENING) pulseScale * 0.95f else 1f)
                                .clip(CircleShape)
                                .background(
                                    Brush.radialGradient(
                                        listOf(
                                            LeafGreenSecondary,
                                            LeafGreenTertiary
                                        )
                                    )
                                )
                                .border(2.dp, LeafGreenLight.copy(alpha = 0.5f), CircleShape)
                        )

                        // Core Orb with Mic / Stop icon
                        Box(
                            modifier = Modifier
                                .size(110.dp)
                                .clip(CircleShape)
                                .background(
                                    Brush.verticalGradient(
                                        listOf(LeafGreenTertiary, LeafGreenAccent)
                                    )
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = when (botState) {
                                    VoiceBotState.LISTENING -> Icons.Default.Stop
                                    VoiceBotState.SPEAKING -> Icons.Default.Stop
                                    else -> Icons.Default.Mic
                                },
                                contentDescription = "Mic Voice Control",
                                tint = Color(0xFF0C1911),
                                modifier = Modifier.size(48.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(36.dp))

                    // Live Transcript Display
                    if (transcript.isNotEmpty()) {
                        Text(
                            text = "\"$transcript\"",
                            style = MaterialTheme.typography.bodyLarge,
                            color = Color.White,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.padding(horizontal = 16.dp)
                        )
                    }

                    if (aiResponse.isNotEmpty() && botState == VoiceBotState.SPEAKING) {
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = aiResponse.take(160) + if (aiResponse.length > 160) "..." else "",
                            style = MaterialTheme.typography.bodyMedium,
                            color = Color(0xFFA0C4B0),
                            textAlign = TextAlign.Center,
                            modifier = Modifier.padding(horizontal = 16.dp)
                        )
                    }
                }

                // Bottom language selector & hint
                Column(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(bottom = 36.dp, start = 20.dp, end = 20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "Low-latency streaming • Multilingual voice • Noise suppression",
                        style = MaterialTheme.typography.labelSmall,
                        color = Color(0xFF6B8E78),
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Row(
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        for (lang in languages.take(3)) {
                            val isSelected = selectedLanguage == lang
                            Surface(
                                color = if (isSelected) LeafGreenAccent.copy(alpha = 0.25f) else Color(0xFF14241B),
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier
                                    .padding(horizontal = 4.dp)
                                    .clickable { selectedLanguage = lang }
                            ) {
                                Text(
                                    text = lang,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = if (isSelected) LeafGreenAccent else Color(0xFFA0C4B0),
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
