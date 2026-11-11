package com.erengokturk.booklate.data.analysis

import com.erengokturk.booklate.core.Constants
import com.erengokturk.booklate.core.Models
import com.erengokturk.booklate.domain.model.AnalysisJson
import com.erengokturk.booklate.domain.model.WordAnalysis
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.serialization.Serializable
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.add
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import kotlinx.serialization.json.putJsonArray
import kotlinx.serialization.json.putJsonObject
import okhttp3.Call
import okhttp3.Callback
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.Response
import java.io.IOException
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

class GeminiException(
    override val message: String,
    val httpCode: Int? = null,
    val isQuotaError: Boolean = false,
    val isAuthError: Boolean = false,
    cause: Throwable? = null,
) : Exception(message, cause)

class MissingApiKeyException : Exception(
    "Gemini API anahtarı ayarlanmamış. Ayarlar → Yapay Zekâ bölümünden ücretsiz anahtarını ekle.",
)

@Serializable
private data class GeminiResponse(
    val candidates: List<GeminiCandidate> = emptyList(),
    val promptFeedback: PromptFeedback? = null,
    val usageMetadata: UsageMetadata? = null,
)

@Serializable
private data class GeminiCandidate(
    val content: GeminiContent? = null,
    val finishReason: String? = null,
    val index: Int = 0,
)

@Serializable
private data class GeminiContent(
    val parts: List<GeminiPart> = emptyList(),
    val role: String? = null,
)

@Serializable
private data class GeminiPart(val text: String? = null)

@Serializable
private data class PromptFeedback(val blockReason: String? = null)

@Serializable
private data class UsageMetadata(
    val promptTokenCount: Int = 0,
    val candidatesTokenCount: Int = 0,
    val totalTokenCount: Int = 0,
)

@Serializable
private data class ApiErrorEnvelope(val error: ApiError? = null)

@Serializable
private data class ApiError(
    val code: Int = 0,
    val message: String = "",
    val status: String = "",
)

data class GeminiUsage(
    val promptTokens: Int,
    val outputTokens: Int,
    val totalTokens: Int,
)

data class GeminiAnalysisResult(
    val analysis: WordAnalysis,
    val usage: GeminiUsage?,
)

class GeminiClient(
    private val httpClient: OkHttpClient,
    private val json: Json = Json {
        ignoreUnknownKeys = true
        isLenient = true
        coerceInputValues = true
    },
) {
    private val jsonMediaType = "application/json; charset=utf-8".toMediaType()

    suspend fun analyze(
        apiKey: String,
        model: String,
        prompt: String,
    ): GeminiAnalysisResult {
        if (apiKey.isBlank()) throw MissingApiKeyException()

        val payload = buildJsonObject {
            putJsonObject("systemInstruction") {
                putJsonArray("parts") {
                    add(buildJsonObject { put("text", GeminiPrompt.system()) })
                }
            }
            putJsonArray("contents") {
                add(
                    buildJsonObject {
                        put("role", "user")
                        putJsonArray("parts") {
                            add(buildJsonObject { put("text", prompt) })
                        }
                    },
                )
            }
            putJsonObject("generationConfig") {
                put("temperature", 0.2)
                put("topP", 0.9)
                put("maxOutputTokens", 1800)
                put("responseMimeType", "application/json")
                put("responseSchema", GeminiPrompt.schema())
            }
        }

        val normalizedModel = Models.normalizeModel(
            model.trim().removePrefix("models/"),
        ).ifBlank { Constants.DEFAULT_MODEL }
        val request = Request.Builder()
            .url("${Constants.GEMINI_BASE_URL}/models/$normalizedModel:generateContent")
            .header("x-goog-api-key", apiKey)
            .header("Accept", "application/json")
            .post(payload.toString().toRequestBody(jsonMediaType))
            .build()

        httpClient.newCall(request).await().use { response ->
            val body = response.body?.string().orEmpty()
            if (!response.isSuccessful) {
                throw mapHttpError(response.code, body)
            }

            val parsed = runCatching { json.decodeFromString<GeminiResponse>(body) }
                .getOrElse {
                    throw GeminiException("Gemini yanıtı okunamadı. Lütfen tekrar deneyin.")
                }

            parsed.promptFeedback?.blockReason?.let { reason ->
                throw GeminiException("İstek güvenlik filtresine takıldı ($reason). Farklı bir kelime deneyin.")
            }

            val candidate = parsed.candidates.firstOrNull()
                ?: throw GeminiException("Gemini boş bir yanıt döndürdü. Tekrar deneyin.")

            val finishReason = candidate.finishReason
            if (finishReason != null && finishReason != "STOP" && finishReason != "FINISH_REASON_UNSPECIFIED") {
                throw GeminiException(
                    when (finishReason) {
                        "MAX_TOKENS" -> "Yanıt çok uzun oldu. Daha kısa bir seçim deneyin."
                        "SAFETY", "RECITATION" -> "İçerik güvenlik filtresine takıldı."
                        else -> "Gemini yanıtı tamamlayamadı ($finishReason)."
                    },
                )
            }

            val rawText = candidate.content?.parts
                ?.mapNotNull { it.text }
                ?.joinToString("")
                ?.trim()
                .orEmpty()

            if (rawText.isBlank()) {
                throw GeminiException("Gemini metin döndürmedi. Tekrar deneyin.")
            }

            val analysis = parseAnalysis(rawText)
                ?: throw GeminiException("Gemini yanıtı beklenen JSON biçiminde değil. Tekrar deneyin.")

            return GeminiAnalysisResult(
                analysis = analysis,
                usage = parsed.usageMetadata?.let {
                    GeminiUsage(
                        promptTokens = it.promptTokenCount,
                        outputTokens = it.candidatesTokenCount,
                        totalTokens = it.totalTokenCount,
                    )
                },
            )
        }
    }

    suspend fun testApiKey(apiKey: String): Boolean {
        if (apiKey.isBlank()) throw MissingApiKeyException()
        val request = Request.Builder()
            .url("${Constants.GEMINI_BASE_URL}/models?pageSize=1")
            .header("x-goog-api-key", apiKey)
            .get()
            .build()
        httpClient.newCall(request).await().use { response ->
            if (response.isSuccessful) return true
            throw mapHttpError(response.code, response.body?.string().orEmpty())
        }
    }

    companion object {
        private val errorJson = Json { ignoreUnknownKeys = true }

        fun parseAnalysis(rawText: String): WordAnalysis? {
            val candidate = extractJson(rawText) ?: return null
            return AnalysisJson.decode(candidate)?.takeIf { it.hasContent }
        }

        fun extractJson(rawText: String): String? {
            var text = rawText.trim()
            if (text.startsWith("```")) {
                text = text.removePrefix("```json")
                    .removePrefix("```JSON")
                    .removePrefix("```")
                val fence = text.lastIndexOf("```")
                if (fence >= 0) text = text.substring(0, fence)
                text = text.trim()
            }

            val start = text.indexOf('{')
            val end = text.lastIndexOf('}')
            if (start < 0 || end <= start) return null
            return text.substring(start, end + 1)
        }

        private fun mapHttpError(code: Int, body: String): GeminiException {
            val message = runCatching {
                errorJson
                    .decodeFromString<ApiErrorEnvelope>(body)
                    .error?.message
            }.getOrNull()?.takeIf { it.isNotBlank() }

            val friendly = when (code) {
                400 -> "Gemini isteği reddedildi. API anahtarını ve seçilen modeli kontrol et."
                401, 403 -> "API anahtarı geçersiz veya Gemini API erişimi kapalı."
                404 -> "Seçilen Gemini modeli bulunamadı. Ayarlardan başka bir model seç."
                429 -> "Ücretsiz Gemini kotası doldu. Bir süre sonra tekrar dene."
                in 500..599 -> "Gemini sunucuları şu anda yanıt veremiyor. Biraz sonra tekrar dene."
                else -> "Gemini isteği başarısız oldu (HTTP $code)."
            }
            return GeminiException(
                message = if (message.isNullOrBlank()) friendly else "$friendly\n\nAyrıntı: $message",
                httpCode = code,
                isQuotaError = code == 429,
                isAuthError = code == 401 || code == 403,
            )
        }
    }
}

private suspend fun Call.await(): Response = suspendCancellableCoroutine { continuation ->
    enqueue(object : Callback {
        override fun onFailure(call: Call, error: IOException) {
            if (continuation.isCancelled) return
            continuation.resumeWithException(
                GeminiException(
                    "İnternete bağlanılamadı. Bağlantını kontrol edip tekrar dene.",
                    cause = error,
                ),
            )
        }

        override fun onResponse(call: Call, response: Response) {
            if (continuation.isActive) {
                continuation.resume(response)
            } else {
                response.close()
            }
        }
    })
    continuation.invokeOnCancellation { runCatching { cancel() } }
}
