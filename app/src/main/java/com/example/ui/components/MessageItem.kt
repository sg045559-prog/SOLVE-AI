package com.example.ui.components

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.net.Uri
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Park
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.data.local.ChatMessageEntity
import com.example.ui.theme.LeafGreenAccent
import com.example.ui.theme.LeafGreenSecondary
import com.example.ui.theme.LeafGreenTertiary

@Composable
fun MessageItem(
    message: ChatMessageEntity,
    isSpeaking: Boolean = false,
    onSpeakClick: (String) -> Unit = {},
    onStopSpeakClick: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val isUser = message.role == "user"

    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 14.dp, vertical = 6.dp),
        horizontalArrangement = if (isUser) Arrangement.End else Arrangement.Start
    ) {
        if (!isUser) {
            // SOLVE AI Avatar with green leaf emblem
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(LeafGreenSecondary)
                    .padding(6.dp),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Park,
                    contentDescription = "SOLVE AI",
                    tint = Color.White,
                    modifier = Modifier.size(20.dp)
                )
            }
            Spacer(modifier = Modifier.width(8.dp))
        }

        Column(
            modifier = Modifier.weight(1f, fill = false),
            horizontalAlignment = if (isUser) Alignment.End else Alignment.Start
        ) {
            // Attached Image Preview if any
            if (!message.imageUri.isNullOrEmpty()) {
                Card(
                    modifier = Modifier
                        .padding(bottom = 6.dp)
                        .size(width = 200.dp, height = 150.dp)
                        .testTag("attached_image_preview"),
                    shape = RoundedCornerShape(12.dp),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    AsyncImage(
                        model = Uri.parse(message.imageUri),
                        contentDescription = "Homework Image to Solve",
                        modifier = Modifier.fillMaxWidth(),
                        contentScale = ContentScale.Crop
                    )
                }
            }

            // Attached Document Badge if any
            if (!message.documentName.isNullOrEmpty()) {
                Surface(
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.padding(bottom = 6.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Description,
                            contentDescription = "Document Attached",
                            modifier = Modifier.size(16.dp),
                            tint = LeafGreenAccent
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = message.documentName,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            // Message Bubble
            val bubbleShape = if (isUser) {
                RoundedCornerShape(topStart = 16.dp, topEnd = 4.dp, bottomStart = 16.dp, bottomEnd = 16.dp)
            } else {
                RoundedCornerShape(topStart = 4.dp, topEnd = 16.dp, bottomStart = 16.dp, bottomEnd = 16.dp)
            }

            Surface(
                shape = bubbleShape,
                color = if (isUser) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
                tonalElevation = if (isUser) 0.dp else 2.dp,
                shadowElevation = 1.dp,
                modifier = Modifier.testTag(if (isUser) "user_message_bubble" else "assistant_message_bubble")
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    // Header meta for assistant: Category & Security indicator
                    if (!isUser) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Surface(
                                    color = LeafGreenSecondary.copy(alpha = 0.15f),
                                    shape = RoundedCornerShape(4.dp)
                                ) {
                                    Text(
                                        text = message.subjectCategory,
                                        style = MaterialTheme.typography.labelSmall,
                                        color = LeafGreenTertiary,
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                        fontWeight = FontWeight.SemiBold
                                    )
                                }
                                Spacer(modifier = Modifier.width(6.dp))
                                Icon(
                                    imageVector = Icons.Default.Lock,
                                    contentDescription = "E2E Encrypted",
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                                    modifier = Modifier.size(12.dp)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                    }

                    // Render Body with Rich Formatter (Markdown & Code blocks)
                    RenderMessageBody(
                        content = message.content,
                        isUser = isUser
                    )
                }
            }

            // Action row for assistant message (Copy, Speak aloud TTS)
            if (!isUser) {
                Row(
                    modifier = Modifier.padding(top = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    var copied by remember { mutableStateOf(false) }
                    IconButton(
                        onClick = {
                            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                            clipboard.setPrimaryClip(ClipData.newPlainText("SOLVE AI", message.content))
                            copied = true
                            Toast.makeText(context, "Solution copied to clipboard", Toast.LENGTH_SHORT).show()
                        },
                        modifier = Modifier.size(32.dp).testTag("copy_message_button")
                    ) {
                        Icon(
                            imageVector = if (copied) Icons.Default.Check else Icons.Default.ContentCopy,
                            contentDescription = "Copy Solution",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                            modifier = Modifier.size(16.dp)
                        )
                    }

                    IconButton(
                        onClick = {
                            if (isSpeaking) {
                                onStopSpeakClick()
                            } else {
                                onSpeakClick(message.content)
                            }
                        },
                        modifier = Modifier.size(32.dp).testTag("speak_message_button")
                    ) {
                        Icon(
                            imageVector = if (isSpeaking) Icons.Default.Stop else Icons.AutoMirrored.Filled.VolumeUp,
                            contentDescription = if (isSpeaking) "Stop voice" else "Read aloud with natural speech",
                            tint = if (isSpeaking) LeafGreenAccent else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }
        }

        if (isUser) {
            Spacer(modifier = Modifier.width(8.dp))
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primaryContainer),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Person,
                    contentDescription = "Student",
                    tint = MaterialTheme.colorScheme.onPrimaryContainer,
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }
}

@Composable
fun RenderMessageBody(
    content: String,
    isUser: Boolean
) {
    val textColor = if (isUser) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
    val parts = remember(content) { parseMessageBlocks(content) }

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        for (part in parts) {
            when (part) {
                is ContentBlock.CodeBlock -> {
                    CodeBlockView(language = part.language, code = part.code)
                }
                is ContentBlock.HeadingBlock -> {
                    Text(
                        text = part.text,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = if (isUser) textColor else LeafGreenTertiary
                    )
                }
                is ContentBlock.TextBlock -> {
                    Text(
                        text = part.text,
                        style = MaterialTheme.typography.bodyMedium,
                        lineHeight = 22.sp,
                        color = textColor
                    )
                }
            }
        }
    }
}

@Composable
fun CodeBlockView(language: String, code: String) {
    val context = LocalContext.current
    var copied by remember { mutableStateOf(false) }

    Surface(
        color = Color(0xFF0F261B),
        shape = RoundedCornerShape(8.dp),
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, Color(0xFF264634), RoundedCornerShape(8.dp))
            .testTag("code_block_container")
    ) {
        Column {
            // Header with badge and copy action
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0xFF163828))
                    .padding(horizontal = 12.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Park,
                        contentDescription = "Academic Note",
                        tint = LeafGreenAccent,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = language.ifEmpty { "Reference" }.uppercase(),
                        style = MaterialTheme.typography.labelSmall,
                        color = LeafGreenAccent,
                        fontWeight = FontWeight.Bold
                    )
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.clickable {
                        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                        clipboard.setPrimaryClip(ClipData.newPlainText("Code", code))
                        copied = true
                        Toast.makeText(context, "Code copied", Toast.LENGTH_SHORT).show()
                    }
                ) {
                    Icon(
                        imageVector = if (copied) Icons.Default.Check else Icons.Default.ContentCopy,
                        contentDescription = "Copy code",
                        tint = Color(0xFFA0C4B0),
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = if (copied) "Copied" else "Copy",
                        style = MaterialTheme.typography.labelSmall,
                        color = Color(0xFFA0C4B0)
                    )
                }
            }

            // Code content
            Text(
                text = code,
                modifier = Modifier.padding(12.dp),
                fontFamily = FontFamily.Monospace,
                fontSize = 13.sp,
                lineHeight = 18.sp,
                color = Color(0xFFE2F3E9)
            )
        }
    }
}

sealed class ContentBlock {
    data class TextBlock(val text: String) : ContentBlock()
    data class HeadingBlock(val text: String) : ContentBlock()
    data class CodeBlock(val language: String, val code: String) : ContentBlock()
}

fun parseMessageBlocks(raw: String): List<ContentBlock> {
    val blocks = mutableListOf<ContentBlock>()
    val lines = raw.lines()
    var inCode = false
    var codeLang = ""
    val codeBuffer = StringBuilder()
    val textBuffer = StringBuilder()

    for (line in lines) {
        if (line.trim().startsWith("```")) {
            if (inCode) {
                // Ending code block
                blocks.add(ContentBlock.CodeBlock(language = codeLang, code = codeBuffer.toString().trimEnd()))
                codeBuffer.clear()
                inCode = false
            } else {
                // Starting code block
                if (textBuffer.isNotEmpty()) {
                    blocks.add(ContentBlock.TextBlock(textBuffer.toString().trim()))
                    textBuffer.clear()
                }
                codeLang = line.trim().removePrefix("```").trim()
                inCode = true
            }
        } else if (inCode) {
            codeBuffer.append(line).append("\n")
        } else {
            if (line.startsWith("### ") || line.startsWith("## ") || line.startsWith("# ")) {
                if (textBuffer.isNotEmpty()) {
                    blocks.add(ContentBlock.TextBlock(textBuffer.toString().trim()))
                    textBuffer.clear()
                }
                blocks.add(ContentBlock.HeadingBlock(line.replace(Regex("^#+\\s*"), "")))
            } else {
                textBuffer.append(line).append("\n")
            }
        }
    }

    if (inCode && codeBuffer.isNotEmpty()) {
        blocks.add(ContentBlock.CodeBlock(language = codeLang, code = codeBuffer.toString().trimEnd()))
    } else if (textBuffer.isNotEmpty()) {
        blocks.add(ContentBlock.TextBlock(textBuffer.toString().trim()))
    }

    return blocks.ifEmpty { listOf(ContentBlock.TextBlock(raw)) }
}
