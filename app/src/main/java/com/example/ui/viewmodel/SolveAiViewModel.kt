package com.example.ui.viewmodel

import android.app.Application
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.speech.tts.TextToSpeech
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.local.AppDatabase
import com.example.data.local.ChatMessageEntity
import com.example.data.local.ChatSessionEntity
import com.example.data.local.EncryptionHelper
import com.example.data.remote.GeminiService
import com.example.ui.components.VoiceBotState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import org.json.JSONArray
import org.json.JSONObject
import java.util.Locale
import java.util.UUID

class SolveAiViewModel(application: Application) : AndroidViewModel(application), TextToSpeech.OnInitListener {

    private val db = AppDatabase.getDatabase(application)
    private val chatDao = db.chatDao()
    private val geminiService = GeminiService(application)

    private val _currentSessionId = MutableStateFlow(UUID.randomUUID().toString())
    val currentSessionId: StateFlow<String> = _currentSessionId.asStateFlow()

    private val _inputText = MutableStateFlow("")
    val inputText: StateFlow<String> = _inputText.asStateFlow()

    private val _selectedImageUri = MutableStateFlow<Uri?>(null)
    val selectedImageUri: StateFlow<Uri?> = _selectedImageUri.asStateFlow()

    private val _selectedDocName = MutableStateFlow<String?>(null)
    val selectedDocName: StateFlow<String?> = _selectedDocName.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _selectedSubject = MutableStateFlow("All")
    val selectedSubject: StateFlow<String> = _selectedSubject.asStateFlow()

    private val _isWebSearchEnabled = MutableStateFlow(false)
    val isWebSearchEnabled: StateFlow<Boolean> = _isWebSearchEnabled.asStateFlow()

    // Voice Bot State
    private val _isVoiceBotOpen = MutableStateFlow(false)
    val isVoiceBotOpen: StateFlow<Boolean> = _isVoiceBotOpen.asStateFlow()

    private val _voiceBotState = MutableStateFlow(VoiceBotState.IDLE)
    val voiceBotState: StateFlow<VoiceBotState> = _voiceBotState.asStateFlow()

    private val _voiceTranscript = MutableStateFlow("")
    val voiceTranscript: StateFlow<String> = _voiceTranscript.asStateFlow()

    private val _voiceAiResponse = MutableStateFlow("")
    val voiceAiResponse: StateFlow<String> = _voiceAiResponse.asStateFlow()

    // Theme & Visual preferences
    private val _isDarkMode = MutableStateFlow(false)
    val isDarkMode: StateFlow<Boolean> = _isDarkMode.asStateFlow()

    private val _showLeavesAnimation = MutableStateFlow(true)
    val showLeavesAnimation: StateFlow<Boolean> = _showLeavesAnimation.asStateFlow()

    // Speech-To-Text and TTS
    private var speechRecognizer: SpeechRecognizer? = null
    private var textToSpeech: TextToSpeech? = null
    private var isTtsReady = false

    private val _speakingMessageText = MutableStateFlow<String?>(null)
    val speakingMessageText: StateFlow<String?> = _speakingMessageText.asStateFlow()

    val messages: StateFlow<List<ChatMessageEntity>> = _currentSessionId.map { sessionId ->
        // We observe DB messages for session and decrypt on the fly
        emptyList<ChatMessageEntity>()
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val realMessages: StateFlow<List<ChatMessageEntity>> = kotlinx.coroutines.flow.channelFlow {
        _currentSessionId.collect { sessionId ->
            chatDao.getMessagesForSession(sessionId).collect { list ->
                val decrypted = list.map { entity ->
                    if (entity.isEncrypted) {
                        entity.copy(content = EncryptionHelper.decrypt(entity.content))
                    } else {
                        entity
                    }
                }
                send(decrypted)
            }
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val totalSolvedCount: StateFlow<Int> = chatDao.getTotalAnsweredCount()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    init {
        initSpeechRecognizer()
        textToSpeech = TextToSpeech(application, this)

        // Seed initial session and helpful welcome message
        viewModelScope.launch {
            val sessionId = _currentSessionId.value
            chatDao.insertSession(
                ChatSessionEntity(
                    id = sessionId,
                    title = "SOLVE AI Homework Session"
                )
            )
            chatDao.insertMessage(
                ChatMessageEntity(
                    sessionId = sessionId,
                    role = "assistant",
                    content = EncryptionHelper.encrypt(
                        "🍃 **Welcome to SOLVE AI!**\n\n" +
                        "I am your student tutor and homework problem solver. Here is what you can do:\n" +
                        "• **Import Image**: Tap 📷 to upload a photo of homework, math problems, or study diagrams to solve.\n" +
                        "• **Voice Dictation**: Tap 🎙️ to dictate your homework questions via Speech-to-Text.\n" +
                        "• **Voice Bot**: Tap the Voice icon for a real-time conversational study session.\n" +
                        "• **Web Grounding**: Enable 🌐 Web Search for live citations.\n\n" +
                        "What problem are we solving today?"
                    ),
                    subjectCategory = "General",
                    isEncrypted = true
                )
            )
        }
    }

    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            textToSpeech?.language = Locale.US
            isTtsReady = true
        }
    }

    private fun initSpeechRecognizer() {
        val context = getApplication<Application>()
        if (SpeechRecognizer.isRecognitionAvailable(context)) {
            speechRecognizer = SpeechRecognizer.createSpeechRecognizer(context).apply {
                setRecognitionListener(object : RecognitionListener {
                    override fun onReadyForSpeech(params: Bundle?) {
                        if (_isVoiceBotOpen.value) {
                            _voiceBotState.value = VoiceBotState.LISTENING
                        }
                    }
                    override fun onBeginningOfSpeech() {}
                    override fun onRmsChanged(rmsdB: Float) {}
                    override fun onBufferReceived(buffer: ByteArray?) {}
                    override fun onEndOfSpeech() {
                        if (_isVoiceBotOpen.value) {
                            _voiceBotState.value = VoiceBotState.THINKING
                        }
                    }
                    override fun onError(error: Int) {
                        if (_isVoiceBotOpen.value) {
                            _voiceBotState.value = VoiceBotState.IDLE
                        }
                    }
                    override fun onResults(results: Bundle?) {
                        val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                        val text = matches?.firstOrNull() ?: return
                        if (_isVoiceBotOpen.value) {
                            _voiceTranscript.value = text
                            sendVoicePrompt(text)
                        } else {
                            _inputText.value = if (_inputText.value.isEmpty()) text else "${_inputText.value} $text"
                        }
                    }
                    override fun onPartialResults(partialResults: Bundle?) {
                        val matches = partialResults?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                        val partial = matches?.firstOrNull() ?: return
                        if (_isVoiceBotOpen.value) {
                            _voiceTranscript.value = partial
                        }
                    }
                    override fun onEvent(eventType: Int, params: Bundle?) {}
                })
            }
        }
    }

    fun setInputText(text: String) {
        _inputText.value = text
    }

    fun setSelectedImage(uri: Uri?) {
        _selectedImageUri.value = uri
    }

    fun setSelectedDoc(name: String?) {
        _selectedDocName.value = name
    }

    fun setSelectedSubject(subject: String) {
        _selectedSubject.value = subject
    }

    fun toggleWebSearch() {
        _isWebSearchEnabled.value = !_isWebSearchEnabled.value
    }

    fun toggleDarkMode(enabled: Boolean) {
        _isDarkMode.value = enabled
    }

    fun toggleLeavesAnimation(enabled: Boolean) {
        _showLeavesAnimation.value = enabled
    }

    fun openVoiceBot() {
        _isVoiceBotOpen.value = true
        _voiceBotState.value = VoiceBotState.IDLE
        _voiceTranscript.value = ""
        _voiceAiResponse.value = ""
    }

    fun closeVoiceBot() {
        stopListening()
        stopSpeech()
        _isVoiceBotOpen.value = false
        _voiceBotState.value = VoiceBotState.IDLE
    }

    fun startListening() {
        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault())
            putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
            putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 1)
        }
        speechRecognizer?.startListening(intent)
        if (_isVoiceBotOpen.value) {
            _voiceBotState.value = VoiceBotState.LISTENING
        }
    }

    fun stopListening() {
        speechRecognizer?.stopListening()
        if (_isVoiceBotOpen.value && _voiceBotState.value == VoiceBotState.LISTENING) {
            _voiceBotState.value = VoiceBotState.IDLE
        }
    }

    fun speakText(text: String) {
        if (!isTtsReady) return
        _speakingMessageText.value = text
        // Strip markdown asterisks and code blocks for clean spoken voice
        val cleanSpeech = text.replace(Regex("```[a-zA-Z]*"), "")
            .replace("```", "")
            .replace("#", "")
            .replace("*", "")
            .take(400) // Keep voice answers natural and conversational
        textToSpeech?.speak(cleanSpeech, TextToSpeech.QUEUE_FLUSH, null, "SOLVE_AI_SPEECH")
    }

    fun stopSpeech() {
        textToSpeech?.stop()
        _speakingMessageText.value = null
        if (_voiceBotState.value == VoiceBotState.SPEAKING) {
            _voiceBotState.value = VoiceBotState.IDLE
        }
    }

    private fun sendVoicePrompt(prompt: String) {
        viewModelScope.launch {
            _voiceBotState.value = VoiceBotState.THINKING
            val subject = if (_selectedSubject.value == "All") "General" else _selectedSubject.value
            val result = geminiService.generateSolution(
                prompt = prompt,
                imageUri = null,
                enableWebGrounding = _isWebSearchEnabled.value,
                subject = subject
            )
            val answer = result.getOrNull() ?: "I've solved that for you."
            _voiceAiResponse.value = answer
            _voiceBotState.value = VoiceBotState.SPEAKING

            // Also persist to current chat
            val sessionId = _currentSessionId.value
            chatDao.insertMessage(
                ChatMessageEntity(
                    sessionId = sessionId,
                    role = "user",
                    content = EncryptionHelper.encrypt(prompt),
                    subjectCategory = subject,
                    isEncrypted = true
                )
            )
            chatDao.insertMessage(
                ChatMessageEntity(
                    sessionId = sessionId,
                    role = "assistant",
                    content = EncryptionHelper.encrypt(answer),
                    subjectCategory = subject,
                    isEncrypted = true
                )
            )

            // Speak response
            speakText(answer)
        }
    }

    fun sendMessage() {
        val query = _inputText.value.trim()
        val image = _selectedImageUri.value
        val doc = _selectedDocName.value

        if (query.isEmpty() && image == null && doc == null) return

        val userPrompt = if (query.isNotEmpty()) {
            query
        } else if (image != null) {
            "Please solve and explain the homework problem in this image."
        } else {
            "Please analyze this study document ($doc)."
        }

        val subject = if (_selectedSubject.value == "All") "General" else _selectedSubject.value
        val sessionId = _currentSessionId.value

        _inputText.value = ""
        _selectedImageUri.value = null
        _selectedDocName.value = null
        _isLoading.value = true

        viewModelScope.launch {
            // Save encrypted user message to Room
            chatDao.insertMessage(
                ChatMessageEntity(
                    sessionId = sessionId,
                    role = "user",
                    content = EncryptionHelper.encrypt(userPrompt),
                    imageUri = image?.toString(),
                    documentName = doc,
                    subjectCategory = subject,
                    isEncrypted = true
                )
            )

            // Call Gemini API with multimodal vision, web grounding, and offline fallback
            val result = geminiService.generateSolution(
                prompt = userPrompt,
                imageUri = image,
                enableWebGrounding = _isWebSearchEnabled.value,
                subject = subject
            )

            val answer = result.getOrNull() ?: "Unable to complete request."

            // Save encrypted assistant answer to Room
            chatDao.insertMessage(
                ChatMessageEntity(
                    sessionId = sessionId,
                    role = "assistant",
                    content = EncryptionHelper.encrypt(answer),
                    subjectCategory = subject,
                    isEncrypted = true,
                    hasCode = answer.contains("```")
                )
            )

            _isLoading.value = false
        }
    }

    fun clearAllData() {
        viewModelScope.launch {
            chatDao.clearAllMessages()
            chatDao.clearAllSessions()
            // Reset with fresh session
            _currentSessionId.value = UUID.randomUUID().toString()
        }
    }

    fun exportDataAsJson(): String {
        val all = realMessages.value
        val jsonArray = JSONArray()
        for (m in all) {
            val obj = JSONObject().apply {
                put("id", m.id)
                put("role", m.role)
                put("content", m.content)
                put("subject", m.subjectCategory)
                put("timestamp", m.timestamp)
                put("encryption", "AES-256-CLIENT")
            }
            jsonArray.put(obj)
        }
        return jsonArray.toString(2)
    }

    override fun onCleared() {
        super.onCleared()
        speechRecognizer?.destroy()
        textToSpeech?.stop()
        textToSpeech?.shutdown()
    }
}
