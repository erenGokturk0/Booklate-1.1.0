package com.erengokturk.booklate.data.pdf

import com.erengokturk.booklate.core.PageTextBuilder
import com.erengokturk.booklate.domain.model.PageText
import com.erengokturk.booklate.domain.model.PageWord
import com.erengokturk.booklate.domain.model.WordRect
import com.tom_roush.pdfbox.text.PDFTextStripper
import com.tom_roush.pdfbox.text.TextPosition
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min

/**
 * PDFBox'un metin çıkarımını kelime kutularına dönüştürür.
 *
 * PDFBox, sayfa döndürmesini (rotation) hesaba katan ekran koordinatlarını
 * TextPosition.x / y üzerinden verir. Bu koordinatlar 72 dpi "point" cinsindendir;
 * burada sayfa genişliğine bölünerek 0..1 aralığına normalize edilir.
 */
class PdfWordStripper(
    private val pageWidth: Float,
    private val pageHeight: Float,
) : PDFTextStripper() {

    private data class RawWord(
        var text: String,
        var left: Float,
        var top: Float,
        var right: Float,
        var bottom: Float,
        val lineIndex: Int,
    )

    private val rawWords = mutableListOf<RawWord>()
    private val current = StringBuilder()
    private var lineIndex = 0
    private var left = Float.MAX_VALUE
    private var top = Float.MAX_VALUE
    private var right = -Float.MAX_VALUE
    private var bottom = -Float.MAX_VALUE
    private var pendingPrefix: RawWord? = null
    private var pageStarted = false

    init {
        sortByPosition = true
        suppressDuplicateOverlappingText = true
        lineSeparator = "\n"
        wordSeparator = " "
    }

    override fun writePageStart() {
        pageStarted = true
        lineIndex = 0
        pendingPrefix = null
        resetCurrent()
    }

    override fun writeLineSeparator() {
        flushCurrent()
        lineIndex++
    }

    override fun writePageEnd() {
        flushCurrent()
        pendingPrefix = null
    }

    override fun writeString(text: String, textPositions: MutableList<TextPosition>) {
        if (textPositions.isEmpty()) {
            // Bazı üreticiler boş TextPosition listesi gönderebilir; metni yine de işle.
            text.forEach { char ->
                if (char.isWhitespace()) flushCurrent() else appendChar(char, 0f, 0f, 1f, 1f)
            }
            return
        }

        textPositions.forEach { position ->
            val unicode = position.unicode ?: return@forEach
            if (unicode.isEmpty()) return@forEach

            val charCount = max(1, unicode.length)
            val charWidth = abs(position.width) / charCount
            val charHeight = max(abs(position.heightDir), 1f)
            val x = position.x
            val y = position.y

            unicode.forEach { char ->
                if (char.isWhitespace()) {
                    flushCurrent()
                } else {
                    appendChar(char, x, y, charWidth, charHeight)
                }
            }
        }
    }

    fun build(): PageText {
        flushCurrent()
        pendingPrefix = null
        if (!pageStarted) return PageText.EMPTY

        val normalized = rawWords
            .asSequence()
            .filter { it.text.isNotBlank() }
            .filter { it.text.length <= 120 }
            .mapNotNull { word ->
                val l = (word.left / pageWidth).coerceIn(0f, 1f)
                val t = (word.top / pageHeight).coerceIn(0f, 1f)
                val r = (word.right / pageWidth).coerceIn(0f, 1f)
                val b = (word.bottom / pageHeight).coerceIn(0f, 1f)
                if (r - l <= 0f || b - t <= 0f) return@mapNotNull null

                // Dokunma alanını biraz genişlet.
                val padX = (r - l) * 0.06f
                val padY = (b - t) * 0.18f
                PageWord(
                    index = 0,
                    text = word.text,
                    rect = WordRect(
                        left = (l - padX).coerceIn(0f, 1f),
                        top = (t - padY).coerceIn(0f, 1f),
                        right = (r + padX).coerceIn(0f, 1f),
                        bottom = (b + padY).coerceIn(0f, 1f),
                    ),
                    lineIndex = word.lineIndex,
                )
            }
            .sortedWith(compareBy({ it.lineIndex }, { it.rect.left }))
            .mapIndexed { index, word -> word.copy(index = index) }
            .toList()

        return PageTextBuilder.build(normalized)
    }

    private fun appendChar(char: Char, x: Float, y: Float, charWidth: Float, charHeight: Float) {
        // PDFBox'ta TextPosition.y taban çizgisidir (baseline), kutunun üstü değil.
        // Bu yüzden kutu, taban çizgisinden yukarı doğru charHeight kadar uzanır.
        val charTop = y - charHeight
        if (current.isEmpty()) {
            left = x
            top = charTop
            right = x + charWidth
            bottom = y
        } else {
            left = min(left, x)
            top = min(top, charTop)
            right = max(right, x + charWidth)
            bottom = max(bottom, y)
        }
        current.append(char)
    }

    private fun flushCurrent() {
        val text = current.toString().trim()
        if (text.isEmpty()) {
            resetCurrent()
            return
        }

        val isOnlyPunctuation = text.none { it.isLetterOrDigit() }
        if (isOnlyPunctuation) {
            val first = text.first()
            val isClosing = first in ".,;:!?…%)]»”\"'"
            if (isClosing) {
                val previous = rawWords.lastOrNull()
                if (previous != null && previous.lineIndex == lineIndex) {
                    previous.text += text
                    previous.right = max(previous.right, right)
                    previous.bottom = max(previous.bottom, bottom)
                    resetCurrent()
                    return
                }
            } else {
                // Açılış tırnağı/parantezi bir sonraki kelimeye eklenecek.
                pendingPrefix = RawWord(text, left, top, right, bottom, lineIndex)
                resetCurrent()
                return
            }
        }

        val prefix = pendingPrefix
        val finalText = if (prefix != null) prefix.text + text else text
        val finalLeft = if (prefix != null) min(prefix.left, left) else left
        val finalTop = if (prefix != null) min(prefix.top, top) else top
        val finalRight = if (prefix != null) max(prefix.right, right) else right
        val finalBottom = if (prefix != null) max(prefix.bottom, bottom) else bottom

        rawWords += RawWord(
            text = finalText,
            left = finalLeft,
            top = finalTop,
            right = finalRight,
            bottom = finalBottom,
            lineIndex = prefix?.lineIndex ?: lineIndex,
        )
        pendingPrefix = null
        resetCurrent()
    }

    private fun resetCurrent() {
        current.setLength(0)
        left = Float.MAX_VALUE
        top = Float.MAX_VALUE
        right = -Float.MAX_VALUE
        bottom = -Float.MAX_VALUE
    }
}
