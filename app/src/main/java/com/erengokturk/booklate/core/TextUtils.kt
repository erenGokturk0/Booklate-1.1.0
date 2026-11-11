package com.erengokturk.booklate.core

import java.text.Normalizer
import java.security.MessageDigest
import java.util.Locale

data class AppLanguage(
    val code: String,
    val label: String,
    val nativeLabel: String,
    val ttsTag: String,
    val flag: String,
)

object LanguageCatalog {
    val turkish = AppLanguage("tr", "Türkçe", "Türkçe", "tr-TR", "🇹🇷")
    val english = AppLanguage("en", "İngilizce", "English", "en-US", "🇬🇧")
    val russian = AppLanguage("ru", "Rusça", "Русский", "ru-RU", "🇷🇺")
    val spanish = AppLanguage("es", "İspanyolca", "Español", "es-ES", "🇪🇸")
    val italian = AppLanguage("it", "İtalyanca", "Italiano", "it-IT", "🇮🇹")
    val german = AppLanguage("de", "Almanca", "Deutsch", "de-DE", "🇩🇪")
    val french = AppLanguage("fr", "Fransızca", "Français", "fr-FR", "🇫🇷")
    val japanese = AppLanguage("ja", "Japonca", "日本語", "ja-JP", "🇯🇵")
    // Dilin doğru adı "Azerbaycan Dili"dir.
    val azerbaijani = AppLanguage("az", "Azerbaycan Dili", "Azərbaycan dili", "az-AZ", "🇦🇿")
    val kazakh = AppLanguage("kk", "Kazakça", "Қазақша", "kk-KZ", "🇰🇿")
    val turkmen = AppLanguage("tk", "Türkmence", "Türkmençe", "tk-TM", "🇹🇲")
    val uzbek = AppLanguage("uz", "Özbekçe", "Oʻzbekcha", "uz-UZ", "🇺🇿")
    val kyrgyz = AppLanguage("ky", "Kırgızca", "Кыргызча", "ky-KG", "🇰🇬")
    val chinese = AppLanguage("zh", "Çince", "中文", "zh-CN", "🇨🇳")
    val polish = AppLanguage("pl", "Lehçe", "Polski", "pl-PL", "🇵🇱")

    /** Hem kitap dili hem ana dil olarak seçilebilen tüm diller. */
    val all: List<AppLanguage> = listOf(
        turkish, english, russian, spanish, italian, german, french, japanese,
        azerbaijani, kazakh, turkmen, uzbek, kyrgyz, chinese, polish,
    )

    /** Kitap / öğrenilen dil seçenekleri. */
    val targets: List<AppLanguage> = all

    /** Ana dil (açıklama ve arayüz dili) seçenekleri. */
    val natives: List<AppLanguage> = all

    fun byCode(code: String): AppLanguage? =
        all.firstOrNull { it.code.equals(code.trim(), ignoreCase = true) }

    fun target(code: String): AppLanguage = byCode(code) ?: turkish

    fun native(code: String): AppLanguage = byCode(code) ?: turkish

    fun resolve(code: String): AppLanguage = byCode(code) ?: turkish
}

data class GeminiModelOption(
    val id: String,
    val label: String,
    val note: String,
)

object Models {
    val geminiOptions = listOf(
        GeminiModelOption(
            id = "gemini-3.5-flash-lite",
            label = "Gemini 3.5 Flash Lite",
            note = "En hızlı · ücretsiz kota için önerilen",
        ),
        GeminiModelOption(
            id = "gemini-3.6-flash",
            label = "Gemini 3.6 Flash",
            note = "Dengeli hız ve kalite",
        ),
        GeminiModelOption(
            id = "gemini-3.8-flash",
            label = "Gemini 3.8 Flash",
            note = "En yüksek kalite, daha yavaş",
        ),
    )

    /**
     * Yeni API anahtarlarında kullanılamayan eski modelleri çalışan
     * güncel modellere eşler. Kullanıcı ayarlarında kayıtlı eski model
     * kalmışsa uygulama otomatik olarak geçerli modele geçer.
     */
    private val retiredModels = mapOf(
        "gemini-2.0-flash" to "gemini-3.5-flash-lite",
        "gemini-2.5-flash" to "gemini-3.5-flash-lite",
        "gemini-2.5-flash-lite" to "gemini-3.5-flash-lite",
        "gemini-2.5-pro" to "gemini-3.8-flash",
    )

    fun normalizeModel(id: String): String {
        val clean = id.trim().removePrefix("models/")
        return retiredModels[clean] ?: clean
    }

    fun geminiLabel(id: String): String =
        geminiOptions.firstOrNull { it.id == id }?.label ?: id
}

object Constants {
    const val GEMINI_BASE_URL = "https://generativelanguage.googleapis.com/v1beta"
    const val PROMPT_VERSION = 4
    const val CACHE_SCHEMA_VERSION = 2
    const val MAX_CONTEXT_CHARS = 600
    const val MAX_SENTENCE_WORDS = 48
    const val MAX_SENTENCE_CHARS = 520
    const val DEFAULT_MODEL = "gemini-3.5-flash-lite"
    const val MIN_RENDER_WIDTH = 720
    const val MAX_RENDER_WIDTH = 2400
}

object TextUtils {
    private val edgePunctuation = Regex("^[^\\p{L}\\p{N}]+|[^\\p{L}\\p{N}]+$")
    private val whitespace = Regex("\\s+")
    private val zeroWidth = Regex("[\\u200B-\\u200D\\uFEFF]")
    private val stressMarks = Regex("[\\u0300\\u0301\\u0302\\u0303\\u0304\\u0306\\u0308]")

    /**
     * Önbellek anahtarı için kelimeyi sadeleştirir. Görüntülenecek metni değiştirmez.
     */
    fun foldForCache(raw: String): String {
        val normalized = Normalizer.normalize(raw, Normalizer.Form.NFC)
            .replace(zeroWidth, "")
            .replace(stressMarks, "")
            .replace(whitespace, " ")
            .trim()
            .replace(edgePunctuation, "")
            .lowercase(Locale.ROOT)
        return normalized.trim()
    }

    fun cacheKey(
        word: String,
        targetLanguage: String,
        nativeLanguage: String,
        promptVersion: Int = Constants.PROMPT_VERSION,
        schemaVersion: Int = Constants.CACHE_SCHEMA_VERSION,
    ): String {
        val payload = "booklate|v$schemaVersion|p$promptVersion|" +
            "${foldForCache(word)}|${targetLanguage.lowercase()}|${nativeLanguage.lowercase()}"
        return sha256(payload)
    }

    fun sha256(value: String): String {
        val digest = MessageDigest.getInstance("SHA-256").digest(value.toByteArray(Charsets.UTF_8))
        return digest.joinToString(separator = "") { "%02x".format(it) }
    }

    fun isSentenceEnd(raw: String): Boolean {
        val text = raw.trim()
            .trimEnd('"', '\'', '”', '’', '»', ')', ']', '}')
        val last = text.lastOrNull() ?: return false
        return last == '.' || last == '!' || last == '?' || last == '…' || last == '。'
    }

    fun isMeaningfulForSpeech(raw: String): Boolean {
        val letters = raw.count { it.isLetter() }
        return letters >= 2 && raw.any { it.isLetterOrDigit() }
    }

    fun truncateContext(raw: String, maxChars: Int = Constants.MAX_CONTEXT_CHARS): String {
        val clean = raw.replace(whitespace, " ").trim()
        if (clean.length <= maxChars) return clean
        val cut = clean.take(maxChars)
        val lastSpace = cut.lastIndexOf(' ')
        return (if (lastSpace > maxChars / 2) cut.take(lastSpace) else cut).trimEnd() + "…"
    }

    fun shortenForSnippet(raw: String, maxChars: Int = 150): String =
        truncateContext(raw, maxChars)

    fun wordCount(raw: String): Int = raw.split(whitespace).count { it.isNotBlank() }

    private val initialsStopWords = setOf(
        "ve", "ile", "bir", "bu", "da", "de", "için",
        "the", "and", "of", "a", "an", "to", "in",
        "der", "die", "das", "und", "ein", "eine",
        "le", "la", "les", "des", "et", "un", "une",
        "el", "los", "las", "y", "del", "una",
    )

    fun initials(title: String): String {
        val words = title.trim().split(whitespace).filter { it.isNotBlank() }
        if (words.isEmpty()) return "B"
        val significant = words.filter { word ->
            val clean = word.trim { !it.isLetterOrDigit() }
            clean.length >= 2 && clean.lowercase(Locale.ROOT) !in initialsStopWords
        }
        val picked = (significant.ifEmpty { words }).take(2)
        return picked
            .mapNotNull { word -> word.firstOrNull { it.isLetterOrDigit() }?.uppercaseChar() }
            .joinToString("")
            .ifBlank { "B" }
    }
}
