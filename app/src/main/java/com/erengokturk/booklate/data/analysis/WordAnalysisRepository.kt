package com.erengokturk.booklate.data.analysis

import com.erengokturk.booklate.core.Constants
import com.erengokturk.booklate.core.TextUtils
import com.erengokturk.booklate.data.local.SettingsRepository
import com.erengokturk.booklate.data.local.WordLookupDao
import com.erengokturk.booklate.data.local.WordLookupEntity
import com.erengokturk.booklate.domain.model.AnalysisJson
import com.erengokturk.booklate.domain.model.AnalysisOutcome
import com.erengokturk.booklate.domain.model.AnalysisRequest
import com.erengokturk.booklate.domain.model.WordLookupRecord
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.util.Locale

data class CacheStats(val wordCount: Int, val favoriteCount: Int)

class WordAnalysisRepository(
    private val wordLookupDao: WordLookupDao,
    private val geminiClient: GeminiClient,
    private val settingsRepository: SettingsRepository,
) {
    private val networkMutex = Mutex()

    /**
     * Kelimeyi önce yerel önbellekte arar; yoksa Gemini'ye gönderir.
     * Aynı kelime tekrar sorgulandığında API kotası harcanmaz.
     *
     * Not: Bağlam değişmişse [AnalysisOutcome.contextMatches] false döner.
     * Arayüz kullanıcıya bunu bildirir ve isterse "bu bağlam için yenile" sunar.
     */
    suspend fun analyze(
        request: AnalysisRequest,
        forceRefresh: Boolean = false,
    ): AnalysisOutcome = networkMutex.withLock {
        val settings = settingsRepository.settings.first()
        val word = request.word.trim().take(120)
        val normalized = TextUtils.foldForCache(word)
        if (normalized.isBlank()) {
            throw IllegalArgumentException("Geçerli bir kelime seçilemedi.")
        }

        val cacheKey = TextUtils.cacheKey(
            word = word,
            targetLanguage = request.targetLanguage,
            nativeLanguage = request.nativeLanguage,
        )

        if (!forceRefresh) {
            cachedOutcome(cacheKey, request)?.let { return@withLock it }
        }

        val apiKey = settings.geminiApiKey.trim()
        if (apiKey.isBlank()) throw MissingApiKeyException()

        val result = geminiClient.analyze(
            apiKey = apiKey,
            model = settings.geminiModel,
            prompt = GeminiPrompt.user(request),
        )

        val analysis = result.analysis.copy(
            word = result.analysis.word.ifBlank { word },
            contextualTranslation = result.analysis.contextualTranslation.trim(),
            dictionaryMeaning = result.analysis.dictionaryMeaning.trim(),
        )

        val now = System.currentTimeMillis()
        val existing = wordLookupDao.find(cacheKey)
        val sentence = TextUtils.truncateContext(request.sentence, Constants.MAX_CONTEXT_CHARS)
        val entity = WordLookupEntity.from(
            cacheKey = cacheKey,
            word = word,
            normalizedWord = normalized,
            sentence = sentence,
            targetLanguage = request.targetLanguage,
            nativeLanguage = request.nativeLanguage,
            analysis = analysis,
            bookTitle = request.bookTitle,
            now = now,
        ).copy(
            createdAt = existing?.createdAt ?: now,
            isFavorite = existing?.isFavorite ?: false,
            hitCount = existing?.hitCount ?: 0,
        )
        wordLookupDao.upsert(entity)

        AnalysisOutcome(
            cacheKey = cacheKey,
            word = word,
            analysis = analysis,
            fromCache = false,
            createdAt = entity.createdAt,
            hitCount = entity.hitCount,
            isFavorite = entity.isFavorite,
            sentence = sentence,
            contextMatches = true,
        )
    }

    suspend fun testApiKey(apiKey: String): Boolean {
        val key = apiKey.trim()
        if (key.isBlank()) throw MissingApiKeyException()
        return geminiClient.testApiKey(key)
    }

    fun history(query: String): Flow<List<WordLookupRecord>> {
        val flow = if (query.isBlank()) wordLookupDao.observeAll()
        else wordLookupDao.search(query.trim())
        return flow.map { entities -> entities.mapNotNull { it.toDomain() } }
    }

    fun favorites(): Flow<List<WordLookupRecord>> =
        wordLookupDao.observeFavorites().map { entities ->
            entities.mapNotNull { it.toDomain() }
        }

    suspend fun setFavorite(cacheKey: String, favorite: Boolean) =
        wordLookupDao.setFavorite(cacheKey, favorite)

    suspend fun delete(cacheKey: String) = wordLookupDao.delete(cacheKey)

    suspend fun clearAll() = wordLookupDao.clear()

    suspend fun stats(): CacheStats = CacheStats(
        wordCount = wordLookupDao.count(),
        favoriteCount = wordLookupDao.favoriteCount(),
    )

    private suspend fun cachedOutcome(
        cacheKey: String,
        request: AnalysisRequest,
    ): AnalysisOutcome? {
        val entity = wordLookupDao.find(cacheKey) ?: return null
        val analysis = AnalysisJson.decode(entity.analysisJson)
        if (analysis == null) {
            wordLookupDao.delete(cacheKey)
            return null
        }

        val now = System.currentTimeMillis()
        wordLookupDao.touch(cacheKey, now)
        val sameContext = comparableContext(entity.sentence) == comparableContext(request.sentence)

        return AnalysisOutcome(
            cacheKey = cacheKey,
            word = entity.word,
            analysis = analysis,
            fromCache = true,
            createdAt = entity.createdAt,
            hitCount = entity.hitCount + 1,
            isFavorite = entity.isFavorite,
            sentence = entity.sentence,
            contextMatches = sameContext,
        )
    }

    private fun comparableContext(value: String): String =
        value.replace(Regex("\\s+"), " ")
            .trim()
            .lowercase(Locale.ROOT)
}
