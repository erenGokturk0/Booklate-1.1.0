package com.erengokturk.booklate.core

import com.erengokturk.booklate.domain.model.PageWord
import com.erengokturk.booklate.domain.model.WordRect
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class PageTextBuilderTest {

    private fun word(index: Int, text: String, line: Int = 0): PageWord = PageWord(
        index = index,
        text = text,
        rect = WordRect(
            left = 0.1f * index,
            top = 0.1f * line,
            right = 0.1f * index + 0.08f,
            bottom = 0.1f * line + 0.04f,
        ),
        lineIndex = line,
    )

    @Test
    fun `groups words into sentences on punctuation`() {
        val page = PageTextBuilder.build(
            listOf(
                word(0, "Это"),
                word(1, "конец."),
                word(2, "Новое"),
                word(3, "предложение."),
            ),
        )

        assertEquals(2, page.sentences.size)
        assertEquals("Это конец.", page.sentences[0].text)
        assertEquals("Новое предложение.", page.sentences[1].text)
        assertEquals(0, page.words[0].sentenceIndex)
        assertEquals(0, page.words[1].sentenceIndex)
        assertEquals(1, page.words[2].sentenceIndex)
        assertEquals(1, page.words[3].sentenceIndex)
        assertEquals(0..1, page.sentences[0].wordIndices)
        assertEquals(2..3, page.sentences[1].wordIndices)
    }

    @Test
    fun `attaches comma to previous word without a space`() {
        val page = PageTextBuilder.build(
            listOf(
                word(0, "слово"),
                word(1, ","),
                word(2, "ещё"),
                word(3, "слово."),
            ),
        )

        assertEquals(1, page.sentences.size)
        assertEquals("слово, ещё слово.", page.sentences[0].text)
    }

    @Test
    fun `splits very long sentences by word budget`() {
        val words = (0 until 5).map { word(it, "w$it") }
        val page = PageTextBuilder.build(words, maxWords = 2, maxChars = 10_000)

        assertEquals(3, page.sentences.size)
        assertEquals(0..1, page.sentences[0].wordIndices)
        assertEquals(2..3, page.sentences[1].wordIndices)
        assertEquals(4..4, page.sentences[2].wordIndices)
        assertEquals("w0 w1", page.sentences[0].text)
    }

    @Test
    fun `empty input produces empty page`() {
        val page = PageTextBuilder.build(emptyList())
        assertTrue(page.words.isEmpty())
        assertTrue(page.sentences.isEmpty())
    }

    @Test
    fun `sentence character ranges are consistent`() {
        val page = PageTextBuilder.build(
            listOf(
                word(0, "Привет,"),
                word(1, "мир!"),
                word(2, "Как"),
                word(3, "дела?"),
            ),
        )

        page.sentences.forEach { sentence ->
            assertEquals(sentence.text.length, sentence.endChar - sentence.startChar)
            assertTrue(sentence.endChar <= page.sentences.sumOf { it.text.length + 1 })
        }
    }
}
