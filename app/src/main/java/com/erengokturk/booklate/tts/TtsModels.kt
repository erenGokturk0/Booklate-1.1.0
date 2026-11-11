package com.erengokturk.booklate.tts

import com.erengokturk.booklate.core.TextUtils
import com.erengokturk.booklate.domain.model.PageText

data class TtsWordRange(
    val wordIndex: Int,
    val startChar: Int,
    val endChar: Int,
)

data class TtsSentence(
    val sentenceIndex: Int,
    val text: String,
    val words: List<TtsWordRange>,
)

data class TtsVoiceOption(
    val name: String,
    val label: String,
    val requiresNetwork: Boolean,
)

data class TtsState(
    val isAvailable: Boolean = true,
    val isReady: Boolean = false,
    val isSpeaking: Boolean = false,
    val isPaused: Boolean = false,
    val currentSentenceIndex: Int = -1,
    val activeWordIndex: Int? = null,
    val languageAvailable: Boolean = true,
    val error: String? = null,
    val voices: List<TtsVoiceOption> = emptyList(),
) {
    val isActive: Boolean get() = isSpeaking || isPaused
}

object TtsTextBuilder {
    fun sentences(page: PageText, onlySentence: Int? = null): List<TtsSentence> {
        if (page.words.isEmpty()) return emptyList()
        return page.sentences.mapNotNull { sentence ->
            if (onlySentence != null && sentence.index != onlySentence) return@mapNotNull null
            val words = page.words.subList(
                sentence.wordStart.coerceIn(0, page.words.size),
                (sentence.wordEnd + 1).coerceIn(0, page.words.size),
            )
            if (words.isEmpty()) return@mapNotNull null

            val builder = StringBuilder()
            val ranges = mutableListOf<TtsWordRange>()
            words.forEachIndexed { index, word ->
                if (builder.isNotEmpty()) builder.append(' ')
                val start = builder.length
                builder.append(word.text)
                ranges += TtsWordRange(
                    wordIndex = word.index,
                    startChar = start,
                    endChar = builder.length,
                )
                if (index == words.lastIndex) {
                    // Noktalama zaten kelimenin içinde; ek işlem gerekmiyor.
                }
            }
            val text = builder.toString()
            if (!TextUtils.isMeaningfulForSpeech(text)) return@mapNotNull null
            TtsSentence(
                sentenceIndex = sentence.index,
                text = text,
                words = ranges,
            )
        }
    }
}
