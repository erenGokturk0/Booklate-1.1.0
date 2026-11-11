package com.erengokturk.booklate.data.analysis

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class GeminiClientTest {

    private val validJson = """
        {
          "word": "книга",
          "contextualTranslation": "kitap",
          "dictionaryMeaning": "kitap, cilt",
          "transcription": "кни́га",
          "partOfSpeech": "isim",
          "gender": "dişil",
          "number": "tekil",
          "case": "Nominatif",
          "animacy": "cansız",
          "verbAspect": "-",
          "tense": "-",
          "person": "-",
          "lemma": "книга",
          "root": "книг",
          "affixes": [
            {"affix": "-а", "type": "çekim eki", "function": "yalın hâl"}
          ],
          "grammarNotes": "Dişil isim, yalın hâlde kullanılmış.",
          "example": "Я читаю книгу.",
          "exampleTranslation": "Ben kitap okuyorum.",
          "cefrLevel": "A2",
          "synonyms": ["том"],
          "collocations": ["интересная книга (ilginç kitap)"]
        }
    """.trimIndent()

    @Test
    fun `extractJson handles plain JSON`() {
        assertEquals("""{"a":1}""", GeminiClient.extractJson("""{"a":1}"""))
    }

    @Test
    fun `extractJson strips markdown fences and surrounding text`() {
        val raw = "İşte sonuç:\n```json\n{\"a\": 1}\n```\nUmarım yardımcı olur."
        assertEquals("""{"a": 1}""", GeminiClient.extractJson(raw))
    }

    @Test
    fun `extractJson returns an empty object when only braces exist`() {
        assertNull(GeminiClient.extractJson("düz metin"))
        assertEquals("{}", GeminiClient.extractJson("{}"))
    }

    @Test
    fun `parseAnalysis maps case field and affixes`() {
        val analysis = GeminiClient.parseAnalysis(validJson)
        assertNotNull(analysis)
        requireNotNull(analysis)

        assertEquals("книга", analysis.word)
        assertEquals("kitap", analysis.contextualTranslation)
        assertEquals("Nominatif", analysis.grammaticalCase)
        assertEquals("dişil", analysis.gender)
        assertEquals(1, analysis.affixes.size)
        assertEquals("-а", analysis.affixes[0].affix)
        assertEquals("A2", analysis.cefrLevel)
        assertEquals(listOf("том"), analysis.synonyms)
        assertEquals(1, analysis.collocations.size)
        assertTrue(analysis.hasContent)
    }

    @Test
    fun `parseAnalysis rejects invalid or empty content`() {
        assertNull(GeminiClient.parseAnalysis("{}"))
        assertNull(GeminiClient.parseAnalysis("bozuk json"))
        assertNull(GeminiClient.parseAnalysis("""{"word":"книга"}"""))
    }
}
