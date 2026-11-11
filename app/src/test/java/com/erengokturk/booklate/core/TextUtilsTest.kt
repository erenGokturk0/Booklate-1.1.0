package com.erengokturk.booklate.core

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class TextUtilsTest {

    @Test
    fun `foldForCache normalizes case punctuation and whitespace`() {
        assertEquals("кот", TextUtils.foldForCache("  КОТ!  "))
        assertEquals("hello", TextUtils.foldForCache("«Hello»"))
        assertEquals("kitap", TextUtils.foldForCache("kitap\u200B"))
    }

    @Test
    fun `cacheKey is stable and language sensitive`() {
        val first = TextUtils.cacheKey("Кот!", "ru", "tr")
        val second = TextUtils.cacheKey("кот", "ru", "tr")
        assertEquals(first, second)
        assertNotEquals(first, TextUtils.cacheKey("кот", "en", "tr"))
        assertNotEquals(first, TextUtils.cacheKey("кот", "ru", "en"))
    }

    @Test
    fun `cacheKey changes when prompt or schema version changes`() {
        val base = TextUtils.cacheKey("кот", "ru", "tr")
        assertNotEquals(
            base,
            TextUtils.cacheKey("кот", "ru", "tr", promptVersion = Constants.PROMPT_VERSION + 1),
        )
        assertNotEquals(
            base,
            TextUtils.cacheKey("кот", "ru", "tr", schemaVersion = Constants.CACHE_SCHEMA_VERSION + 1),
        )
    }

    @Test
    fun `isSentenceEnd accepts common endings`() {
        assertTrue(TextUtils.isSentenceEnd("Привет!"))
        assertTrue(TextUtils.isSentenceEnd("Как дела?"))
        assertTrue(TextUtils.isSentenceEnd("Это конец."))
        assertTrue(TextUtils.isSentenceEnd("Он сказал: «Да».»"))
        assertTrue(TextUtils.isSentenceEnd("Это многоточие…"))
        assertFalse(TextUtils.isSentenceEnd("обычное слово"))
        assertFalse(TextUtils.isSentenceEnd("слово,"))
    }

    @Test
    fun `truncateContext keeps whole words and adds ellipsis`() {
        val result = TextUtils.truncateContext("one two three four five", maxChars = 12)
        assertEquals("one two…", result)
        assertTrue(result.length <= 13)
    }

    @Test
    fun `truncateContext leaves short text untouched`() {
        assertEquals("kısa metin", TextUtils.truncateContext("kısa   metin", maxChars = 50))
    }

    @Test
    fun `initials builds at most two letters`() {
        assertEquals("SB", TextUtils.initials("Savaş ve Barış"))
        assertEquals("К", TextUtils.initials("Книга"))
        assertEquals("B", TextUtils.initials("   "))
    }

    @Test
    fun `normalizeModel replaces retired Gemini models`() {
        assertEquals("gemini-3.5-flash-lite", Models.normalizeModel("gemini-2.5-flash-lite"))
        assertEquals("gemini-3.5-flash-lite", Models.normalizeModel("models/gemini-2.5-flash"))
        assertEquals("gemini-3.5-flash-lite", Models.normalizeModel("gemini-2.0-flash"))
        assertEquals("gemini-3.8-flash", Models.normalizeModel("gemini-2.5-pro"))
        assertEquals("gemini-3.6-flash", Models.normalizeModel("gemini-3.6-flash"))
    }

    @Test
    fun `language catalog contains every supported language`() {
        val codes = LanguageCatalog.all.map { it.code }
        assertEquals(
            listOf(
                "tr", "en", "ru", "es", "it", "de", "fr", "ja",
                "az", "kk", "tk", "uz", "ky", "zh", "pl",
            ),
            codes,
        )
        assertEquals(codes.size, codes.distinct().size)
        assertTrue(LanguageCatalog.all.all { it.ttsTag.isNotBlank() })
        assertTrue(LanguageCatalog.all.all { it.label.isNotBlank() && it.nativeLabel.isNotBlank() })
    }

    @Test
    fun `language catalog resolves codes case insensitively`() {
        assertEquals(LanguageCatalog.chinese, LanguageCatalog.resolve(" ZH "))
        assertEquals(LanguageCatalog.kazakh, LanguageCatalog.resolve("kk"))
        assertEquals(LanguageCatalog.turkish, LanguageCatalog.resolve("unknown"))
        assertEquals(null, LanguageCatalog.byCode("xx"))
    }

    @Test
    fun `language catalog is shared by books and native language`() {
        assertEquals(LanguageCatalog.all, LanguageCatalog.targets)
        assertEquals(LanguageCatalog.all, LanguageCatalog.natives)
    }
}
