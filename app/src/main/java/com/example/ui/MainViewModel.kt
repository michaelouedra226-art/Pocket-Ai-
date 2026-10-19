package com.example.ui

import android.app.Application
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.api.AiProviderService
import com.example.data.api.AiRequest
import com.example.data.api.ProviderType
import com.example.data.db.AppDatabase
import com.example.data.db.entities.ConversationEntity
import com.example.data.db.entities.MemoryEntity
import com.example.data.db.entities.MessageEntity
import com.example.data.db.entities.PersonaEntity
import com.example.data.tools.DeviceToolService
import com.example.data.tools.ToolExecutionResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import org.json.JSONObject
import java.util.UUID

data class SettingsState(
    val provider: ProviderType = ProviderType.DEEPSEEK,
    val model: String = "deepseek-chat",
    val deepseekApiKey: String = "",
    val geminiApiKey: String = "",
    val openaiApiKey: String = "",
    val claudeApiKey: String = "",
    val customBaseUrl: String = "",
    val temperature: Float = 0.6f,
    val maxTokens: Int = 8192,
    val toolsEnabled: Boolean = true
)

class MainViewModel(application: Application) : AndroidViewModel(application) {

    private val db = AppDatabase.getInstance(application)
    private val conversationDao = db.conversationDao()
    private val messageDao = db.messageDao()
    private val personaDao = db.personaDao()
    private val memoryDao = db.memoryDao()

    private val toolService = DeviceToolService(application, memoryDao)
    private val aiService = AiProviderService(application)

    private val sharedPrefs = application.getSharedPreferences("deepseek_hermes_prefs", Context.MODE_PRIVATE)

    val conversations: StateFlow<List<ConversationEntity>> = conversationDao.getAllConversations()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val personas: StateFlow<List<PersonaEntity>> = personaDao.getAllPersonas()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), AppDatabase.DefaultPersonas)

    val memories: StateFlow<List<MemoryEntity>> = memoryDao.getAllMemories()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _currentConversation = MutableStateFlow<ConversationEntity?>(null)
    val currentConversation: StateFlow<ConversationEntity?> = _currentConversation.asStateFlow()

    private val _messages = MutableStateFlow<List<MessageEntity>>(emptyList())
    val messages: StateFlow<List<MessageEntity>> = _messages.asStateFlow()

    private val _currentPersona = MutableStateFlow(AppDatabase.DefaultPersonas.first())
    val currentPersona: StateFlow<PersonaEntity> = _currentPersona.asStateFlow()

    private val _isAgentThinking = MutableStateFlow(false)
    val isAgentThinking: StateFlow<Boolean> = _isAgentThinking.asStateFlow()

    private val _currentToolExecution = MutableStateFlow<ToolExecutionResult?>(null)
    val currentToolExecution: StateFlow<ToolExecutionResult?> = _currentToolExecution.asStateFlow()

    private val _settings = MutableStateFlow(loadSettings())
    val settings: StateFlow<SettingsState> = _settings.asStateFlow()

    init {
        viewModelScope.launch {
            // Restore or create initial conversation
            val list = conversationDao.getAllConversations()
            list.collect { convs ->
                if (_currentConversation.value == null) {
                    if (convs.isNotEmpty()) {
                        selectConversation(convs.first().id)
                    } else {
                        createNewConversation()
                    }
                }
            }
        }
    }

    private fun loadSettings(): SettingsState {
        val providerName = sharedPrefs.getString("provider", ProviderType.DEEPSEEK.name) ?: ProviderType.DEEPSEEK.name
        val provider = try { ProviderType.valueOf(providerName) } catch (_: Exception) { ProviderType.DEEPSEEK }
        return SettingsState(
            provider = provider,
            model = sharedPrefs.getString("model", "deepseek-chat") ?: "deepseek-chat",
            deepseekApiKey = sharedPrefs.getString("deepseek_key", "").orEmpty(),
            geminiApiKey = sharedPrefs.getString("gemini_key", "").orEmpty(),
            openaiApiKey = sharedPrefs.getString("openai_key", "").orEmpty(),
            claudeApiKey = sharedPrefs.getString("claude_key", "").orEmpty(),
            customBaseUrl = sharedPrefs.getString("custom_url", "").orEmpty(),
            temperature = sharedPrefs.getFloat("temperature", 0.6f),
            maxTokens = sharedPrefs.getInt("max_tokens", 8192),
            toolsEnabled = sharedPrefs.getBoolean("tools_enabled", true)
        )
    }

    fun updateSettings(newSettings: SettingsState) {
        _settings.value = newSettings
        sharedPrefs.edit().apply {
            putString("provider", newSettings.provider.name)
            putString("model", newSettings.model)
            putString("deepseek_key", newSettings.deepseekApiKey)
            putString("gemini_key", newSettings.geminiApiKey)
            putString("openai_key", newSettings.openaiApiKey)
            putString("claude_key", newSettings.claudeApiKey)
            putString("custom_url", newSettings.customBaseUrl)
            putFloat("temperature", newSettings.temperature)
            putInt("max_tokens", newSettings.maxTokens)
            putBoolean("tools_enabled", newSettings.toolsEnabled)
            apply()
        }
    }

    fun selectConversation(id: String) {
        viewModelScope.launch {
            val conv = conversationDao.getConversationById(id) ?: return@launch
            _currentConversation.value = conv
            // Subscribe to messages
            messageDao.getMessagesForConversation(id).collect {
                _messages.value = it
            }
        }
    }

    fun createNewConversation(title: String = "Nouvelle mission") {
        viewModelScope.launch {
            val conv = ConversationEntity(
                id = UUID.randomUUID().toString(),
                title = title,
                model = _settings.value.model,
                provider = _settings.value.provider.displayName,
                personaId = _currentPersona.value.id
            )
            conversationDao.insertOrUpdate(conv)
            _currentConversation.value = conv
            _messages.value = emptyList()

            // Optional welcoming message
            val welcome = MessageEntity(
                id = UUID.randomUUID().toString(),
                conversationId = conv.id,
                role = "assistant",
                content = """⚡ **DeepSeek Hermes v1.0.0** prêt.
Modèle : **${conv.model}** via **${conv.provider}**
Persona : **${_currentPersona.value.name}**

Que souhaitez-vous exécuter ? Posez une question technique, demandez une télémétrie de l'appareil (`device_telemetry`), ou stockez un paramètre dans la mémoire Hermes.""",
                status = "complete"
            )
            messageDao.insertOrUpdate(welcome)
        }
    }

    fun deleteConversation(id: String) {
        viewModelScope.launch {
            messageDao.deleteForConversation(id)
            conversationDao.deleteById(id)
            if (_currentConversation.value?.id == id) {
                _currentConversation.value = null
                _messages.value = emptyList()
            }
        }
    }

    fun clearAllConversations() {
        viewModelScope.launch {
            conversationDao.deleteAll()
            _currentConversation.value = null
            _messages.value = emptyList()
            createNewConversation()
        }
    }

    fun setPersona(persona: PersonaEntity) {
        _currentPersona.value = persona
    }

    fun saveCustomPersona(name: String, description: String, prompt: String, temp: Float) {
        viewModelScope.launch {
            val entity = PersonaEntity(
                id = UUID.randomUUID().toString(),
                name = name,
                description = description,
                systemPrompt = prompt,
                iconKey = "custom",
                temperature = temp,
                isDefault = false
            )
            personaDao.insertOrUpdate(entity)
            _currentPersona.value = entity
        }
    }

    fun saveMemory(key: String, value: String, category: String) {
        viewModelScope.launch {
            val entity = MemoryEntity(
                id = UUID.randomUUID().toString(),
                category = category,
                key = key,
                value = value
            )
            memoryDao.insertOrUpdate(entity)
        }
    }

    fun deleteMemory(id: String) {
        viewModelScope.launch {
            memoryDao.deleteById(id)
        }
    }

    fun sendMessage(userText: String) {
        if (userText.isBlank()) return
        val conv = _currentConversation.value ?: return

        viewModelScope.launch(Dispatchers.IO) {
            val userMsg = MessageEntity(
                id = UUID.randomUUID().toString(),
                conversationId = conv.id,
                role = "user",
                content = userText
            )
            messageDao.insertOrUpdate(userMsg)

            // Update title if initial
            if (conv.title == "Nouvelle mission") {
                val newTitle = if (userText.length > 28) userText.take(28) + "..." else userText
                conversationDao.insertOrUpdate(conv.copy(title = newTitle, updatedAt = System.currentTimeMillis()))
            }

            _isAgentThinking.value = true
            _currentToolExecution.value = null

            // Build request history
            val currentMsgs = _messages.value + userMsg
            val historyPairs = currentMsgs.map { it.role to it.content }

            val settings = _settings.value
            val currentKey = when (settings.provider) {
                ProviderType.DEEPSEEK -> settings.deepseekApiKey
                ProviderType.GEMINI -> settings.geminiApiKey
                ProviderType.OPENAI -> settings.openaiApiKey
                ProviderType.CLAUDE -> settings.claudeApiKey
                ProviderType.LOCAL_GGUF -> ""
            }

            val request = AiRequest(
                provider = settings.provider,
                model = settings.model,
                apiKey = currentKey,
                customBaseUrl = settings.customBaseUrl,
                systemPrompt = _currentPersona.value.systemPrompt,
                messages = historyPairs,
                temperature = _currentPersona.value.temperature,
                maxTokens = settings.maxTokens
            )

            val firstResponse = aiService.generateResponse(request)

            if (!firstResponse.success) {
                _isAgentThinking.value = false
                val errorMsg = MessageEntity(
                    id = UUID.randomUUID().toString(),
                    conversationId = conv.id,
                    role = "assistant",
                    content = "⚠️ Erreur: ${firstResponse.errorMessage}",
                    status = "error"
                )
                messageDao.insertOrUpdate(errorMsg)
                return@launch
            }

            // Check if agent invoked a tool
            if (settings.toolsEnabled && firstResponse.rawToolCallName != null) {
                val toolName = firstResponse.rawToolCallName
                val toolArgs = try {
                    JSONObject(firstResponse.rawToolCallArgs ?: "{}")
                } catch (_: Exception) {
                    JSONObject()
                }

                // Execute tool
                val toolResult = toolService.executeTool(toolName, toolArgs)
                _currentToolExecution.value = toolResult

                // Store intermediate tool observation message
                val toolMsg = MessageEntity(
                    id = UUID.randomUUID().toString(),
                    conversationId = conv.id,
                    role = "tool",
                    content = toolResult.output,
                    toolName = toolName,
                    toolArgsJson = toolArgs.toString(),
                    toolResultJson = toolResult.rawJson,
                    thinking = firstResponse.thinking
                )
                messageDao.insertOrUpdate(toolMsg)

                // Synthesize final answer with observation
                val updatedHistory = historyPairs + listOf(
                    "assistant" to (firstResponse.content + "\nACTION: $toolName"),
                    "user" to "OBSERVATION: ${toolResult.output}\nSynthétise maintenant la réponse finale pour l'utilisateur."
                )

                val synthResponse = aiService.generateResponse(request.copy(messages = updatedHistory))

                val finalAssistantMsg = MessageEntity(
                    id = UUID.randomUUID().toString(),
                    conversationId = conv.id,
                    role = "assistant",
                    content = synthResponse.content.ifBlank { toolResult.output },
                    thinking = synthResponse.thinking ?: firstResponse.thinking,
                    toolName = toolName,
                    toolArgsJson = toolArgs.toString(),
                    toolResultJson = toolResult.rawJson,
                    status = if (synthResponse.success) "complete" else "error"
                )
                messageDao.insertOrUpdate(finalAssistantMsg)
            } else {
                val assistantMsg = MessageEntity(
                    id = UUID.randomUUID().toString(),
                    conversationId = conv.id,
                    role = "assistant",
                    content = firstResponse.content,
                    thinking = firstResponse.thinking,
                    status = "complete"
                )
                messageDao.insertOrUpdate(assistantMsg)
            }

            _isAgentThinking.value = false
            _currentToolExecution.value = null
        }
    }
}
