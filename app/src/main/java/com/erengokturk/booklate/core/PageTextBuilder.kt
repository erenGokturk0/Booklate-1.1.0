package com.erengokturk.booklate.core

import com.erengokturk.booklate.domain.model.PageSentence
import com.erengokturk.booklate.domain.model.PageText
import com.erengokturk.booklate.domain.model.PageWord

/**
 * PDF kelimelerini cümlelere böler. Saf Kotlin olduğu için birim testleriyle
 * doğrulanabilir ve ileride masaüstü/web hedeflerinde yeniden kullanılabilir.
 */
object PageTextBuilder {

    fun build(
        words: List<PageWord>,
        maxWords: Int = Constants.MAX_SENTENCE_WORDS,
        maxChars: Int = Constants.MAX_SENTENCE_CHARS,
    ): PageText {
        if (words.isEmpty()) return PageText.EMPTY

        val sentences = mutableListOf<PageSentence>()
        val resultWords = words.toMutableList()

        var sentenceStartWord = 0
        var sentenceStartChar = 0
        var wordsInSentence = 0
        val builder = StringBuilder()

        fun flush(endExclusive: Int) {
            if (endExclusive <= sentenceStartWord || builder.isBlank()) return
            val sentenceIndex = sentences.size
            val text = builder.toString().trim()
            sentences += PageSentence(
                index = sentenceIndex,
                text = text,
                wordStart = sentenceStartWord,
                wordEnd = endExclusive - 1,
                startChar = sentenceStartChar,
                endChar = sentenceStartChar + text.length,
            )
            for (i in sentenceStartWord until endExclusive) {
                resultWords[i] = resultWords[i].copy(sentenceIndex = sentenceIndex)
            }
            sentenceStartWord = endExclusive
            sentenceStartChar += text.length + 1
            wordsInSentence = 0
            builder.setLength(0)
        }

        words.forEachIndexed { index, word ->
            val clean = word.text.trim()
            if (clean.isEmpty()) return@forEachIndexed

            val attachesToPrevious = clean.firstOrNull()?.let { first ->
                first in ".,;:!?…%)]»”\"'"
            } == true

            if (builder.isNotEmpty() && !attachesToPrevious) {
                builder.append(' ')
            }
            builder.append(clean)
            wordsInSentence++

            val shouldClose = TextUtils.isSentenceEnd(clean) ||
                wordsInSentence >= maxWords ||
                builder.length >= maxChars

            if (shouldClose) {
                flush(index + 1)
            }
        }

        flush(words.size)

        return PageText(words = resultWords, sentences = sentences)
    }
}
