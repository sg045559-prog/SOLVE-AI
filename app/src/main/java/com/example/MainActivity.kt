package com.example

import android.Manifest
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.AddPhotoAlternate
import androidx.compose.material.icons.filled.AttachFile
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Park
import androidx.compose.material.icons.filled.RecordVoiceOver
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
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
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.example.ui.components.AnalyticsDashboardSheet
import com.example.ui.components.FallingLeavesCanvas
import com.example.ui.components.MessageItem
import com.example.ui.components.PrivacySettingsSheet
import com.example.ui.components.VoiceConversationDialog
import com.example.ui.theme.LeafGreenAccent
import com.example.ui.theme.LeafGreenSecondary
import com.example.ui.theme.LeafGreenTertiary
import com.example.ui.theme.LeafGreenUltraLight
import com.example.ui.theme.SolveAiTheme
import com.example.ui.viewmodel.SolveAiViewModel

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val viewModel: SolveAiViewModel = viewModel()
            val isDarkMode by viewModel.isDarkMode.collectAsState()

            SolveAiTheme(darkTheme = isDarkMode) {
                SolveAiMainScreen(viewModel = viewModel)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SolveAiMainScreen(viewModel: SolveAiViewModel) {
    val context = LocalContext.current
    val listState = rememberLazyListState()

    val messages by viewModel.realMessages.collectAsState()
    val inputText by viewModel.inputText.collectAsState()
    val selectedImageUri by viewModel.selectedImageUri.collectAsState()
    val selectedDocName by viewModel.selectedDocName.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val selectedSubject by viewModel.selectedSubject.collectAsState()
    val isWebSearchEnabled by viewModel.isWebSearchEnabled.collectAsState()
    val isVoiceBotOpen by viewModel.isVoiceBotOpen.collectAsState()
    val voiceBotState by viewModel.voiceBotState.collectAsState()
    val voiceTranscript by viewModel.voiceTranscript.collectAsState()
    val voiceAiResponse by viewModel.voiceAiResponse.collectAsState()
    val isDarkMode by viewModel.isDarkMode.collectAsState()
    val showLeavesAnimation by viewModel.showLeavesAnimation.collectAsState()
    val speakingMessageText by viewModel.speakingMessageText.collectAsState()
    val totalSolvedCount by viewModel.totalSolvedCount.collectAsState()

    var showAnalyticsSheet by remember { mutableStateOf(false) }
    var showPrivacySheet by remember { mutableStateOf(false) }

    // Android Photo Picker for zero-permission image importing
    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri: Uri? ->
        if (uri != null) {
            viewModel.setSelectedImage(uri)
            Toast.makeText(context, "Image imported to solve", Toast.LENGTH_SHORT).show()
        }
    }

    // Document file picker for study notes / PDFs
    val docPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            val fileName = uri.lastPathSegment ?: "StudyDoc.pdf"
            viewModel.setSelectedDoc(fileName)
            Toast.makeText(context, "Document attached", Toast.LENGTH_SHORT).show()
        }
    }

    // Audio Permission Launcher for Speech-to-Text
    val audioPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            viewModel.startListening()
            Toast.makeText(context, "Listening... speak your question", Toast.LENGTH_SHORT).show()
        } else {
            Toast.makeText(context, "Microphone permission required for voice dictation", Toast.LENGTH_LONG).show()
        }
    }

    // Auto-scroll to bottom on new message
    LaunchedEffect(messages.size, isLoading) {
        if (messages.isNotEmpty()) {
            listState.animateScrollToItem(messages.size - 1)
        }
    }

    Scaffold(
        modifier = Modifier
            .fillMaxSize()
            .testTag("solve_ai_screen"),
        containerColor = MaterialTheme.colorScheme.background,
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        // Leaf & Joint Logo Badge
                        Box(
                            modifier = Modifier
                                .size(34.dp)
                                .clip(CircleShape)
                                .background(LeafGreenSecondary),
                            contentAlignment = Alignment.Center
                        ) {
                            Image(
                                painter = painterResource(id = R.drawable.ic_solve_ai_logo),
                                contentDescription = "SOLVE AI Logo",
                                modifier = Modifier.size(34.dp),
                                contentScale = ContentScale.Crop
                            )
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = "SOLVE AI",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "Student Homework & Problem Solver",
                                style = MaterialTheme.typography.labelSmall,
                                color = LeafGreenTertiary
                            )
                        }
                    }
                },
                actions = {
                    // Real-time Voice to Voice Bot button
                    IconButton(
                        onClick = {
                            val hasPermission = ContextCompat.checkSelfPermission(
                                context,
                                Manifest.permission.RECORD_AUDIO
                            ) == PackageManager.PERMISSION_GRANTED

                            if (hasPermission) {
                                viewModel.openVoiceBot()
                            } else {
                                audioPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                            }
                        },
                        modifier = Modifier.testTag("open_voice_bot_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.RecordVoiceOver,
                            contentDescription = "Voice to Voice Bot",
                            tint = LeafGreenSecondary
                        )
                    }

                    // Analytics & Insights Dashboard button
                    IconButton(
                        onClick = { showAnalyticsSheet = true },
                        modifier = Modifier.testTag("open_analytics_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.BarChart,
                            contentDescription = "Study Analytics",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    // Privacy & Vault Settings button
                    IconButton(
                        onClick = { showPrivacySheet = true },
                        modifier = Modifier.testTag("open_settings_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Settings,
                            contentDescription = "Settings & Privacy",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.95f)
                ),
                modifier = Modifier.statusBarsPadding()
            )
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // Subtle Falling Leaves Background Animation (Canvas)
            FallingLeavesCanvas(
                leafCount = 16,
                enabled = showLeavesAnimation
            )

            Column(modifier = Modifier.fillMaxSize()) {
                // Subject Filter Bar
                SubjectSelectorRow(
                    selectedSubject = selectedSubject,
                    onSelectSubject = { viewModel.setSelectedSubject(it) }
                )

                // Conversation Message List
                LazyColumn(
                    state = listState,
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .testTag("messages_list"),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    items(messages, key = { it.id }) { msg ->
                        MessageItem(
                            message = msg,
                            isSpeaking = speakingMessageText == msg.content,
                            onSpeakClick = { viewModel.speakText(it) },
                            onStopSpeakClick = { viewModel.stopSpeech() }
                        )
                    }

                    if (isLoading) {
                        item {
                            ThinkingIndicator()
                        }
                    }

                    item {
                        Spacer(modifier = Modifier.height(8.dp))
                    }
                }

                // Chat Input Dock
                ChatInputDock(
                    inputText = inputText,
                    onInputTextChange = { viewModel.setInputText(it) },
                    selectedImageUri = selectedImageUri,
                    onClearImage = { viewModel.setSelectedImage(null) },
                    selectedDocName = selectedDocName,
                    onClearDoc = { viewModel.setSelectedDoc(null) },
                    isWebSearchEnabled = isWebSearchEnabled,
                    onToggleWebSearch = { viewModel.toggleWebSearch() },
                    onPickImage = {
                        photoPickerLauncher.launch(
                            PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                        )
                    },
                    onPickDoc = {
                        docPickerLauncher.launch("*/*")
                    },
                    onStartVoiceDictation = {
                        val hasPerm = ContextCompat.checkSelfPermission(
                            context,
                            Manifest.permission.RECORD_AUDIO
                        ) == PackageManager.PERMISSION_GRANTED
                        if (hasPerm) {
                            viewModel.startListening()
                            Toast.makeText(context, "Listening... speak now", Toast.LENGTH_SHORT).show()
                        } else {
                            audioPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                        }
                    },
                    onSendMessage = { viewModel.sendMessage() },
                    isLoading = isLoading
                )
            }
        }
    }

    // Voice to Voice Interactive Dialog
    if (isVoiceBotOpen) {
        VoiceConversationDialog(
            botState = voiceBotState,
            transcript = voiceTranscript,
            aiResponse = voiceAiResponse,
            onStartListening = { viewModel.startListening() },
            onStopListening = { viewModel.stopListening() },
            onInterruptSpeech = { viewModel.stopSpeech() },
            onDismiss = { viewModel.closeVoiceBot() }
        )
    }

    // Analytics Dashboard Sheet
    if (showAnalyticsSheet) {
        AnalyticsDashboardSheet(
            solvedCount = totalSolvedCount,
            onDismiss = { showAnalyticsSheet = false }
        )
    }

    // Privacy, GDPR & Theme Sheet
    if (showPrivacySheet) {
        PrivacySettingsSheet(
            isDarkMode = isDarkMode,
            onToggleDarkMode = { viewModel.toggleDarkMode(it) },
            showLeavesAnimation = showLeavesAnimation,
            onToggleLeaves = { viewModel.toggleLeavesAnimation(it) },
            onClearAllData = { viewModel.clearAllData() },
            onExportDataJson = { viewModel.exportDataAsJson() },
            onDismiss = { showPrivacySheet = false }
        )
    }
}

@Composable
fun SubjectSelectorRow(
    selectedSubject: String,
    onSelectSubject: (String) -> Unit
) {
    val subjects = listOf("All", "Math", "Science", "Literature", "General")

    LazyRow(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.9f))
            .padding(horizontal = 14.dp, vertical = 6.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        item {
            Icon(
                imageVector = Icons.Default.Park,
                contentDescription = null,
                tint = LeafGreenSecondary,
                modifier = Modifier.size(18.dp)
            )
        }
        items(subjects) { subject ->
            val isSelected = selectedSubject == subject
            FilterChip(
                selected = isSelected,
                onClick = { onSelectSubject(subject) },
                label = { Text(subject, fontSize = 12.sp) },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = LeafGreenSecondary,
                    selectedLabelColor = Color.White,
                    containerColor = MaterialTheme.colorScheme.surfaceVariant,
                    labelColor = MaterialTheme.colorScheme.onSurfaceVariant
                ),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.testTag("filter_$subject")
            )
        }
    }
}

@Composable
fun ThinkingIndicator() {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(32.dp)
                .clip(CircleShape)
                .background(LeafGreenSecondary),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.Park,
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.size(18.dp)
            )
        }
        Spacer(modifier = Modifier.width(10.dp))
        Surface(
            shape = RoundedCornerShape(12.dp),
            color = MaterialTheme.colorScheme.surfaceVariant
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                CircularProgressIndicator(
                    modifier = Modifier.size(14.dp),
                    strokeWidth = 2.dp,
                    color = LeafGreenSecondary
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "SOLVE AI is reasoning...",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
fun ChatInputDock(
    inputText: String,
    onInputTextChange: (String) -> Unit,
    selectedImageUri: Uri?,
    onClearImage: () -> Unit,
    selectedDocName: String?,
    onClearDoc: () -> Unit,
    isWebSearchEnabled: Boolean,
    onToggleWebSearch: () -> Unit,
    onPickImage: () -> Unit,
    onPickDoc: () -> Unit,
    onStartVoiceDictation: () -> Unit,
    onSendMessage: () -> Unit,
    isLoading: Boolean
) {
    Surface(
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 4.dp,
        modifier = Modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .imePadding()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 8.dp)
        ) {
            // Preview chips for attached image / document
            if (selectedImageUri != null || selectedDocName != null) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 6.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (selectedImageUri != null) {
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant,
                            border = androidx.compose.foundation.BorderStroke(1.dp, LeafGreenAccent)
                        ) {
                            Row(
                                modifier = Modifier.padding(4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                AsyncImage(
                                    model = selectedImageUri,
                                    contentDescription = "Selected image preview",
                                    modifier = Modifier
                                        .size(36.dp)
                                        .clip(RoundedCornerShape(6.dp)),
                                    contentScale = ContentScale.Crop
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Image attached", style = MaterialTheme.typography.labelSmall)
                                IconButton(onClick = onClearImage, modifier = Modifier.size(24.dp)) {
                                    Icon(Icons.Default.Close, contentDescription = "Remove image", modifier = Modifier.size(14.dp))
                                }
                            }
                        }
                    }

                    if (selectedDocName != null) {
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = selectedDocName,
                                    style = MaterialTheme.typography.labelSmall,
                                    maxLines = 1
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                IconButton(onClick = onClearDoc, modifier = Modifier.size(20.dp)) {
                                    Icon(Icons.Default.Close, contentDescription = "Remove doc", modifier = Modifier.size(12.dp))
                                }
                            }
                        }
                    }
                }
            }

            // Input Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Attachments row (Image / Doc / Web toggle)
                IconButton(
                    onClick = onPickImage,
                    modifier = Modifier
                        .size(38.dp)
                        .testTag("attach_image_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.AddPhotoAlternate,
                        contentDescription = "Import Image to Solve",
                        tint = if (selectedImageUri != null) LeafGreenAccent else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                IconButton(
                    onClick = onPickDoc,
                    modifier = Modifier
                        .size(38.dp)
                        .testTag("attach_doc_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.AttachFile,
                        contentDescription = "Attach Study Document",
                        tint = if (selectedDocName != null) LeafGreenAccent else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                IconButton(
                    onClick = onToggleWebSearch,
                    modifier = Modifier
                        .size(38.dp)
                        .testTag("toggle_web_search_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.Language,
                        contentDescription = "Web Search Grounding",
                        tint = if (isWebSearchEnabled) LeafGreenSecondary else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                    )
                }

                // Text Field with integrated microphone dictation icon
                OutlinedTextField(
                    value = inputText,
                    onValueChange = onInputTextChange,
                    placeholder = {
                        Text(
                            "Ask homework, math or study question...",
                            fontSize = 14.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                        )
                    },
                    modifier = Modifier
                        .weight(1f)
                        .padding(horizontal = 4.dp)
                        .testTag("chat_input_field"),
                    shape = RoundedCornerShape(24.dp),
                    trailingIcon = {
                        // Microphone icon for Speech-to-Text dictation
                        IconButton(
                            onClick = onStartVoiceDictation,
                            modifier = Modifier
                                .size(34.dp)
                                .testTag("mic_dictation_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Mic,
                                contentDescription = "Dictate homework with speech",
                                tint = LeafGreenSecondary,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    },
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                    keyboardActions = KeyboardActions(onSend = { onSendMessage() }),
                    maxLines = 4,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = LeafGreenSecondary,
                        unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)
                    )
                )

                // Send Button
                val canSend = inputText.isNotBlank() || selectedImageUri != null || selectedDocName != null
                IconButton(
                    onClick = onSendMessage,
                    enabled = canSend && !isLoading,
                    modifier = Modifier
                        .size(42.dp)
                        .clip(CircleShape)
                        .background(
                            if (canSend && !isLoading) LeafGreenSecondary else MaterialTheme.colorScheme.surfaceVariant
                        )
                        .testTag("send_message_button")
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.Send,
                        contentDescription = "Send homework question",
                        tint = if (canSend && !isLoading) Color.White else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f),
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }
    }
}
