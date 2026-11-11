package com.erengokturk.booklate.domain.model

/**
 * PDF sayfasındaki bir kelimenin normalize edilmiş (0..1 aralığında) konumu.
 * Normalize koordinatlar, bitmap çözünürlüğünden bağımsız çizim yapmayı sağlar.
 */
data class WordRect(
    val left: Float,
    val top: Float,
    val right: Float,
    val bottom: Float,
) {
    val width: Float get() = (right - left).coerceAtLeast(0f)
    val height: Float get() = (bottom - top).coerceAtLeast(0f)
    val centerX: Float get() = (left + right) / 2f
    val centerY: Float get() = (top + bottom) / 2f

    fun contains(x: Float, y: Float, slop: Float = 0f): Boolean =
        x >= left - slop && x <= right + slop && y >= top - slop && y <= bottom + slop

    fun distanceSquaredTo(x: Float, y: Float): Float {
        val dx = centerX - x
        val dy = centerY - y
        return dx * dx + dy * dy
    }
}

data class PageWord(
    val index: Int,
    val text: String,
    val rect: WordRect,
    val lineIndex: Int,
    val sentenceIndex: Int = -1,
)

data class PageSentence(
    val index: Int,
    val text: String,
    val wordStart: Int,
    val wordEnd: Int,
    val startChar: Int,
    val endChar: Int,
) {
    val wordIndices: IntRange get() = wordStart..wordEnd
    val wordCount: Int get() = wordEnd - wordStart + 1
}

data class PageText(
    val words: List<PageWord>,
    val sentences: List<PageSentence>,
) {
    fun sentenceForWord(wordIndex: Int): PageSentence? {
        val word = words.getOrNull(wordIndex) ?: return null
        return sentences.getOrNull(word.sentenceIndex)
    }

    fun wordsForRange(range: IntRange): List<PageWord> =
        range.mapNotNull { words.getOrNull(it) }

    companion object {
        val EMPTY = PageText(emptyList(), emptyList())
    }
}

/**
 * Kullanıcının tek kelime veya çoklu kelime seçimini taşır.
 */
data class ReaderSelection(
    val wordIndices: IntRange,
    val text: String,
    val sentence: String,
    val sentenceIndex: Int,
) {
    val isSingleWord: Boolean get() = wordIndices.first == wordIndices.last
}
