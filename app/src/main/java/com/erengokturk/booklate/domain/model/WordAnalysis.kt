package com.erengokturk.booklate.domain.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

@Serializable
data class AffixAnalysis(
    val affix: String = "",
    val type: String = "",
    val function: String = "",
)

/**
 * Gemini'nin döndürdüğü yapılandırılmış kelime analizi.
 * Tüm alanlar varsayılan değerli; model beklenmeyen bir alanı atlarsa uygulama çökmez.
 */
@Serializable
data class WordAnalysis(
    val word: String = "",
    val contextualTranslation: String = "",
    val dictionaryMeaning: String = "",
    val transcription: String = "",
    val partOfSpeech: String = "",
    val gender: String = "-",
    val number: String = "-",
    @SerialName("case") val grammaticalCase: String = "-",
    val animacy: String = "-",
    val verbAspect: String = "-",
    val tense: String = "-",
    val person: String = "-",
    val lemma: String = "",
    val root: String = "",
    val affixes: List<AffixAnalysis> = emptyList(),
    val grammarNotes: String = "",
    val example: String = "",
    val exampleTranslation: String = "",
    val cefrLevel: String = "",
    val synonyms: List<String> = emptyList(),
    val collocations: List<String> = emptyList(),
) {
    val hasContent: Boolean
        get() = contextualTranslation.isNotBlank() || dictionaryMeaning.isNotBlank()
}

object AnalysisJson {
    val json: Json = Json {
        ignoreUnknownKeys = true
        isLenient = true
        coerceInputValues = true
        encodeDefaults = true
        explicitNulls = false
    }

    fun encode(analysis: WordAnalysis): String =
        json.encodeToString(analysis)

    fun decode(raw: String): WordAnalysis? =
        runCatching { json.decodeFromString<WordAnalysis>(raw) }.getOrNull()
}
