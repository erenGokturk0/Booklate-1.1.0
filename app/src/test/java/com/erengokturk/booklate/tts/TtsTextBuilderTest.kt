package com.erengokturk.booklate.tts

import com.erengokturk.booklate.core.PageTextBuilder
import com.erengokturk.booklate.domain.model.PageWord
import com.erengokturk.booklate.domain.model.WordRect
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class TtsTextBuilderTest {

    private fun word(index: Int, text: String): PageWord = PageWord(
        index = index,
        text = text,
        rect = WordRect(
            left = 0.05f * index,
            top = 0.1f,
            right = 0.05f * index + 0.04f,
            bottom = 0.14f,
        ),
        lineIndex = 0,
    )

    @Test
    fun `word ranges point at the right substring`() {
        val page = PageTextBuilder.build(
            listOf(
                word(0, "Привет,"),
                word(1, "мир!"),
                word(2, "Как"),
                word(3, "дела?"),
            ),
        )

        val sentences = TtsTextBuilder.sentences(page)
        assertEquals(2, sentences.size)
        assertEquals("Привет, мир!", sentences[0].text)
        assertEquals("Как дела?", sentences[1].text)

        sentences.forEach { sentence ->
            sentence.words.forEach { range ->
                val original = page.words[range.wordIndex].text
                assertEquals(original, sentence.text.substring(range.startChar, range.endChar))
            }
        }
    }

    @Test
    fun `can build only the requested sentence`() {
        val page = PageTextBuilder.build(
            listOf(
                word(0, "Birinci."),
                word(1, "İkinci."),
                word(2, "Üçüncü."),
            ),
        )

        val sentences = TtsTextBuilder.sentences(page, onlySentence = 1)
        assertEquals(1, sentences.size)
        assertEquals("İkinci.", sentences[0].text)
        assertEquals(1, sentences[0].sentenceIndex)
    }

    @Test
    fun `returns nothing for empty page`() {
        assertTrue(TtsTextBuilder.sentences(com.erengokturk.booklate.domain.model.PageText.EMPTY).isEmpty())
    }
}
