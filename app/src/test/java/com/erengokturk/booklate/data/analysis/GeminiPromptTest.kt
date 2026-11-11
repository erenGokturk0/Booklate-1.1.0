package com.erengokturk.booklate.data.analysis

import com.erengokturk.booklate.domain.model.AnalysisRequest
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class GeminiPromptTest {

    private fun request(target: String, native: String) = AnalysisRequest(
        word = "книга",
        sentence = "Я читаю книгу.",
        targetLanguage = target,
        nativeLanguage = native,
        bookTitle = "Метро 2035",
        pageNumber = 8,
    )

    @Test
    fun `prompt writes explanations in the selected native language`() {
        val prompt = GeminiPrompt.user(request(target = "ru", native = "es"))

        assertTrue(prompt.contains("ANADİL: İspanyolca"))
        assertTrue(prompt.contains("HEDEF DİL: Rusça"))
        assertTrue(prompt.contains("İspanyolca karşılığı"))
        assertTrue(prompt.contains("İspanyolca çevirisi"))
        assertFalse(prompt.contains("Türkçe karşılığı"))
    }

    @Test
    fun `prompt follows the book language instead of a global target`() {
        val japanese = GeminiPrompt.user(request(target = "ja", native = "tr"))
        val polish = GeminiPrompt.user(request(target = "pl", native = "tr"))

        assertTrue(japanese.contains("HEDEF DİL: Japonca (日本語)"))
        assertTrue(polish.contains("HEDEF DİL: Lehçe (Polski)"))
        assertTrue(japanese.contains("Türkçe karşılığı"))
    }

    @Test
    fun `prompt marks grammar categories as optional`() {
        val prompt = GeminiPrompt.user(request(target = "en", native = "tr"))

        assertTrue(prompt.contains("yoksa \"-\""))
    }
}
