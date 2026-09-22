package com.unfinished.app.ai

import android.content.Context
import com.unfinished.app.data.LocalCipher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

data class AiProviderConfig(
    val baseUrl: String = "",
    val model: String = "",
    val apiKey: String = ""
)

data class CueSuggestion(
    val type: String,
    val value: String,
    val source: String
)

class AiProviderConfigStore(context: Context) {
    private val prefs = context.getSharedPreferences("unfinished_ai", Context.MODE_PRIVATE)
    private val cipher = LocalCipher()

    fun read(): AiProviderConfig = AiProviderConfig(
        baseUrl = prefs.getString("baseUrl", "") ?: "",
        model = prefs.getString("model", "") ?: "",
        apiKey = prefs.getString("apiKey", "")?.let { encrypted ->
            runCatching { cipher.decrypt(encrypted) }.getOrDefault("")
        } ?: ""
    )

    fun save(config: AiProviderConfig) {
        prefs.edit()
            .putString("baseUrl", config.baseUrl.trim())
            .putString("model", config.model.trim())
            .putString(
                "apiKey",
                if (config.apiKey.isBlank()) "" else cipher.encrypt(config.apiKey.trim())
            )
            .apply()
    }
}

class CueSuggestionProvider(private val configStore: AiProviderConfigStore) {
    suspend fun suggest(content: String): List<CueSuggestion> = withContext(Dispatchers.IO) {
        val config = configStore.read()
        if (config.baseUrl.isBlank() || config.apiKey.isBlank()) {
            return@withContext fallback(content)
        }
        runCatching { request(config, content) }.getOrElse { fallback(content) }
    }

    suspend fun testConnection(): Boolean = withContext(Dispatchers.IO) {
        val config = configStore.read()
        if (config.baseUrl.isBlank() || config.apiKey.isBlank()) return@withContext false
        runCatching {
            request(config, "请只返回：连接成功", maxTokens = 20)
            true
        }.getOrDefault(false)
    }

    private fun request(
        config: AiProviderConfig,
        content: String,
        maxTokens: Int = 240
    ): List<CueSuggestion> {
        val connection = (URL(endpoint(config.baseUrl)).openConnection() as HttpURLConnection).apply {
            requestMethod = "POST"
            connectTimeout = 8_000
            readTimeout = 20_000
            doOutput = true
            setRequestProperty("Content-Type", "application/json")
            setRequestProperty("Authorization", "Bearer ${config.apiKey}")
        }
        val messages = JSONArray()
            .put(JSONObject().put("role", "system").put("content", SYSTEM_PROMPT))
            .put(JSONObject().put("role", "user").put("content", content))
        val body = JSONObject()
            .put("model", config.model.ifBlank { "default" })
            .put("temperature", 0.2)
            .put("max_tokens", maxTokens)
            .put("stream", false)
            .put("messages", messages)
        connection.outputStream.use { stream -> stream.write(body.toString().toByteArray(Charsets.UTF_8)) }
        val code = connection.responseCode
        val input = if (code in 200..299) connection.inputStream else connection.errorStream
        val response = input?.bufferedReader()?.use { it.readText() }.orEmpty()
        connection.disconnect()
        if (code !in 200..299) error("AI provider returned HTTP $code")
        val message = JSONObject(response)
            .optJSONArray("choices")
            ?.optJSONObject(0)
            ?.optJSONObject("message")
            ?.optString("content")
            .orEmpty()
        return parseSuggestions(message).ifEmpty { fallback(content) }
    }

    private fun endpoint(baseUrl: String): String {
        val normalized = baseUrl.trim().trimEnd('/')
        return if (normalized.endsWith("/chat/completions")) normalized else "$normalized/chat/completions"
    }

    private fun parseSuggestions(message: String): List<CueSuggestion> = message
        .lineSequence()
        .map { it.trim().replace(Regex("^[-*•\\d.)、]+\\s*"), "") }
        .filter { it.length in 2..48 }
        .distinct()
        .take(5)
        .map { CueSuggestion(type = "KEYWORD", value = it, source = "AI_SUGGESTED") }
        .toList()

    private fun fallback(content: String): List<CueSuggestion> = content
        .split(Regex("[，。！？；：、,.!?;:\\s]+"))
        .map { it.trim() }
        .filter { it.length in 2..16 }
        .distinct()
        .take(3)
        .map { CueSuggestion(type = "KEYWORD", value = it, source = "AI_SUGGESTED") }

    companion object {
        private const val SYSTEM_PROMPT =
            "你是个人记忆线索助手。根据用户输入提取最多五条中性的回忆线索，每行一条。只输出线索文本，不要编号，不要分析情绪，不要编造事实。"
    }
}
