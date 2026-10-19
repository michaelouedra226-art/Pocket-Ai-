package com.example.data.api

import android.content.Context
import com.example.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.TimeUnit

enum class ProviderType(val displayName: String, val defaultModel: String, val defaultBaseUrl: String) {
    DEEPSEEK("DeepSeek", "deepseek-chat", "https://api.deepseek.com/v1"),
    GEMINI("Google Gemini", "gemini-3.5-flash", "https://generativelanguage.googleapis.com/v1beta"),
    OPENAI("OpenAI", "gpt-4o", "https://api.openai.com/v1"),
    CLAUDE("Anthropic Claude", "claude-3-7-sonnet-20250219", "https://api.anthropic.com/v1"),
    LOCAL_GGUF("Local GGUF / Ollama", "llama3.2", "http://10.0.2.2:11434/v1")
}

data class AiRequest(
    val provider: ProviderType,
    val model: String,
    val apiKey: String,
    val customBaseUrl: String? = null,
    val systemPrompt: String,
    val messages: List<Pair<String, String>>, // role, text
    val temperature: Float = 0.7f,
    val maxTokens: Int = 8192
)

data class AiResponse(
    val success: Boolean,
    val content: String,
    val thinking: String? = null,
    val rawToolCallName: String? = null,
    val rawToolCallArgs: String? = null,
    val errorMessage: String? = null
)

class AiProviderService(private val context: Context) {

    private val httpClient = OkHttpClient.Builder()
        .connectTimeout(45, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .writeTimeout(45, TimeUnit.SECONDS)
        .build()

    suspend fun generateResponse(request: AiRequest): AiResponse = withContext(Dispatchers.IO) {
        val builtInGeminiKey = try {
            BuildConfig::class.java.getField("GEMINI_API_KEY").get(null) as? String ?: ""
        } catch (_: Exception) {
            ""
        }

        val effectiveKey = when {
            request.apiKey.isNotBlank() -> request.apiKey
            request.provider == ProviderType.GEMINI && builtInGeminiKey.isNotBlank() && builtInGeminiKey != "MY_GEMINI_API_KEY" -> builtInGeminiKey
            else -> ""
        }

        if (effectiveKey.isBlank() && request.provider != ProviderType.LOCAL_GGUF) {
            // Provide intelligent local fallback agent response
            return@withContext runLocalFallbackAgent(request)
        }

        try {
            when (request.provider) {
                ProviderType.DEEPSEEK, ProviderType.OPENAI, ProviderType.LOCAL_GGUF -> {
                    callOpenAiCompatible(request, effectiveKey)
                }
                ProviderType.GEMINI -> {
                    callGemini(request, effectiveKey)
                }
                ProviderType.CLAUDE -> {
                    callClaude(request, effectiveKey)
                }
            }
        } catch (e: Exception) {
            AiResponse(
                success = false,
                content = "",
                errorMessage = "Network or API error (${request.provider.displayName}): ${e.message}"
            )
        }
    }

    private fun callOpenAiCompatible(request: AiRequest, apiKey: String): AiResponse {
        val baseUrl = request.customBaseUrl?.takeIf { it.isNotBlank() } ?: request.provider.defaultBaseUrl
        val url = "$baseUrl/chat/completions"

        val rootJson = JSONObject().apply {
            put("model", request.model)
            put("temperature", request.temperature)
            put("max_tokens", request.maxTokens)

            val msgsArr = JSONArray()
            if (request.systemPrompt.isNotBlank()) {
                msgsArr.put(JSONObject().apply {
                    put("role", "system")
                    put("content", request.systemPrompt)
                })
            }
            request.messages.forEach { (role, content) ->
                msgsArr.put(JSONObject().apply {
                    put("role", if (role == "assistant") "assistant" else "user")
                    put("content", content)
                })
            }
            put("messages", msgsArr)
        }

        val mediaType = "application/json; charset=utf-8".toMediaType()
        val body = rootJson.toString().toRequestBody(mediaType)
        val httpReq = Request.Builder()
            .url(url)
            .addHeader("Authorization", "Bearer $apiKey")
            .addHeader("Content-Type", "application/json")
            .post(body)
            .build()

        val response = httpClient.newCall(httpReq).execute()
        val responseBody = response.body?.string().orEmpty()

        if (!response.isSuccessful) {
            return AiResponse(
                success = false,
                content = "",
                errorMessage = "HTTP ${response.code}: $responseBody"
            )
        }

        val json = JSONObject(responseBody)
        val choices = json.optJSONArray("choices")
        val firstChoice = choices?.optJSONObject(0)
        val message = firstChoice?.optJSONObject("message")
        val content = message?.optString("content", "").orEmpty()
        val reasoningContent = if (message?.has("reasoning_content") == true) message.optString("reasoning_content") else null

        val (extractedText, toolName, toolArgs) = parseActionFraming(content)

        return AiResponse(
            success = true,
            content = extractedText,
            thinking = reasoningContent,
            rawToolCallName = toolName,
            rawToolCallArgs = toolArgs
        )
    }

    private fun callGemini(request: AiRequest, apiKey: String): AiResponse {
        val model = request.model.ifBlank { "gemini-3.5-flash" }
        val url = "https://generativelanguage.googleapis.com/v1beta/models/$model:generateContent?key=$apiKey"

        val rootJson = JSONObject().apply {
            val contentsArr = JSONArray()

            request.messages.forEach { (role, content) ->
                contentsArr.put(JSONObject().apply {
                    put("role", if (role == "assistant") "model" else "user")
                    put("parts", JSONArray().apply {
                        put(JSONObject().apply { put("text", content) })
                    })
                })
            }
            put("contents", contentsArr)

            if (request.systemPrompt.isNotBlank()) {
                put("systemInstruction", JSONObject().apply {
                    put("parts", JSONArray().apply {
                        put(JSONObject().apply { put("text", request.systemPrompt) })
                    })
                })
            }

            put("generationConfig", JSONObject().apply {
                put("temperature", request.temperature)
                put("maxOutputTokens", request.maxTokens)
            })
        }

        val mediaType = "application/json; charset=utf-8".toMediaType()
        val body = rootJson.toString().toRequestBody(mediaType)
        val httpReq = Request.Builder()
            .url(url)
            .addHeader("Content-Type", "application/json")
            .post(body)
            .build()

        val response = httpClient.newCall(httpReq).execute()
        val responseBody = response.body?.string().orEmpty()

        if (!response.isSuccessful) {
            return AiResponse(
                success = false,
                content = "",
                errorMessage = "Gemini API HTTP ${response.code}: $responseBody"
            )
        }

        val json = JSONObject(responseBody)
        val candidates = json.optJSONArray("candidates")
        val firstCandidate = candidates?.optJSONObject(0)
        val contentObj = firstCandidate?.optJSONObject("content")
        val parts = contentObj?.optJSONArray("parts")

        val sb = StringBuilder()
        for (i in 0 until (parts?.length() ?: 0)) {
            val part = parts?.optJSONObject(i)
            sb.append(part?.optString("text", "").orEmpty())
        }

        val fullText = sb.toString()
        val (cleanText, toolName, toolArgs) = parseActionFraming(fullText)

        return AiResponse(
            success = true,
            content = cleanText,
            rawToolCallName = toolName,
            rawToolCallArgs = toolArgs
        )
    }

    private fun callClaude(request: AiRequest, apiKey: String): AiResponse {
        val url = "https://api.anthropic.com/v1/messages"
        val rootJson = JSONObject().apply {
            put("model", request.model)
            put("max_tokens", request.maxTokens)
            put("temperature", request.temperature)
            if (request.systemPrompt.isNotBlank()) {
                put("system", request.systemPrompt)
            }
            val msgsArr = JSONArray()
            request.messages.forEach { (role, content) ->
                msgsArr.put(JSONObject().apply {
                    put("role", if (role == "assistant") "assistant" else "user")
                    put("content", content)
                })
            }
            put("messages", msgsArr)
        }

        val mediaType = "application/json; charset=utf-8".toMediaType()
        val body = rootJson.toString().toRequestBody(mediaType)
        val httpReq = Request.Builder()
            .url(url)
            .addHeader("x-api-key", apiKey)
            .addHeader("anthropic-version", "2023-06-01")
            .addHeader("Content-Type", "application/json")
            .post(body)
            .build()

        val response = httpClient.newCall(httpReq).execute()
        val responseBody = response.body?.string().orEmpty()

        if (!response.isSuccessful) {
            return AiResponse(
                success = false,
                content = "",
                errorMessage = "Claude API HTTP ${response.code}: $responseBody"
            )
        }

        val json = JSONObject(responseBody)
        val contentArr = json.optJSONArray("content")
        val firstItem = contentArr?.optJSONObject(0)
        val text = firstItem?.optString("text", "").orEmpty()

        val (cleanText, toolName, toolArgs) = parseActionFraming(text)

        return AiResponse(
            success = true,
            content = cleanText,
            rawToolCallName = toolName,
            rawToolCallArgs = toolArgs
        )
    }

    private fun runLocalFallbackAgent(request: AiRequest): AiResponse {
        val lastUserMessage = request.messages.lastOrNull { it.first == "user" }?.second.orEmpty().lowercase()

        // Check if query naturally triggers an autonomous tool
        return when {
            lastUserMessage.contains("battery") || lastUserMessage.contains("batterie") ||
                    lastUserMessage.contains("telemetry") || lastUserMessage.contains("télémetrie") ||
                    lastUserMessage.contains("ram") || lastUserMessage.contains("network") ||
                    lastUserMessage.contains("réseau") || lastUserMessage.contains("device") ||
                    lastUserMessage.contains("appareil") || lastUserMessage.contains("statut") -> {
                AiResponse(
                    success = true,
                    content = "Reconnaissance de l'environnement matériel en cours...",
                    thinking = "L'utilisateur demande des informations sur l'état du système ou de la batterie. Exécution de device_telemetry.",
                    rawToolCallName = "device_telemetry",
                    rawToolCallArgs = "{}"
                )
            }
            lastUserMessage.contains("calcule") || lastUserMessage.contains("calculate") ||
                    lastUserMessage.contains("+") || lastUserMessage.contains("*") || lastUserMessage.contains("/") -> {
                // Extract math expression
                val expr = lastUserMessage.replace(Regex("[^0-9+\\-*/^.()]"), "")
                AiResponse(
                    success = true,
                    content = "Calcul mathématique en cours...",
                    thinking = "Évaluation de l'expression mathématique locale.",
                    rawToolCallName = "math_evaluator",
                    rawToolCallArgs = JSONObject().apply { put("expression", expr.ifBlank { "42 * 2" }) }.toString()
                )
            }
            lastUserMessage.contains("heure") || lastUserMessage.contains("time") || lastUserMessage.contains("date") -> {
                AiResponse(
                    success = true,
                    content = "Lecture de l'horloge système...",
                    thinking = "Extraction de l'horodatage précis du terminal.",
                    rawToolCallName = "timestamp_clock",
                    rawToolCallArgs = "{}"
                )
            }
            lastUserMessage.contains("mémorise") || lastUserMessage.contains("remember") || lastUserMessage.contains("sauvegarde") -> {
                AiResponse(
                    success = true,
                    content = "Enregistrement dans la mémoire Hermes...",
                    thinking = "Stockage persistant de la note utilisateur dans SQLite.",
                    rawToolCallName = "hermes_memory",
                    rawToolCallArgs = JSONObject().apply {
                        put("action", "store")
                        put("key", "user_note")
                        put("value", lastUserMessage)
                    }.toString()
                )
            }
            lastUserMessage.contains("brief") || lastUserMessage.contains("rapport") -> {
                AiResponse(
                    success = true,
                    content = "Génération du rapport tactique...",
                    thinking = "Formatage d'un brief de reconnaissance complet.",
                    rawToolCallName = "tactical_brief",
                    rawToolCallArgs = JSONObject().apply { put("topic", "Audit de mission") }.toString()
                )
            }
            else -> {
                val greeting = buildString {
                    appendLine("⚡ **DeepSeek Hermes v1.0.0 (PocketStrike)**")
                    appendLine("Moteur agent autonome opérationnel sur Android.")
                    appendLine()
                    appendLine("Fournisseur actif : **${request.provider.displayName}** (`${request.model}`)")
                    appendLine()
                    appendLine("Pour activer l'inférence en direct dans le cloud :")
                    appendLine("1. Ouvrez le menu **Paramètres** (icône ⚙️ ou tiroir)")
                    appendLine("2. Renseignez votre clé API (`DeepSeek`, `Gemini`, `OpenAI`, ou `Claude`)")
                    appendLine("3. Ou pointez vers un serveur local **Ollama / GGUF**")
                    appendLine()
                    appendLine("💡 **Outils disponibles sans clé** :")
                    appendLine("- *« Quel est l'état de ma batterie et du réseau ? »* → `device_telemetry`")
                    appendLine("- *« Calcule 128 * 64 »* → `math_evaluator`")
                    appendLine("- *« Mémorise mon projet Hermes »* → `hermes_memory`")
                    appendLine("- *« Donne-moi un brief tactique »* → `tactical_brief`")
                }
                AiResponse(
                    success = true,
                    content = greeting,
                    thinking = "Moteur local en attente de clé API. Présentation des capacités ReAct et des outils disponibles."
                )
            }
        }
    }

    private fun parseActionFraming(text: String): Triple<String, String?, String?> {
        val actionPattern = Regex("(?i)ACTION:\\s*([a-zA-Z0-9_]+)")
        val argsPattern = Regex("(?is)ARGS:\\s*(\\{.*?\\})")

        val actionMatch = actionPattern.find(text)
        val argsMatch = argsPattern.find(text)

        val toolName = actionMatch?.groupValues?.getOrNull(1)
        val toolArgs = argsMatch?.groupValues?.getOrNull(1)

        val cleanText = text
            .replace(actionPattern, "")
            .replace(argsPattern, "")
            .trim()

        return Triple(cleanText, toolName, toolArgs)
    }
}
