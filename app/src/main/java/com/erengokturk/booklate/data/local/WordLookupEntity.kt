package com.erengokturk.booklate.data.local

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import com.erengokturk.booklate.domain.model.AnalysisJson
import com.erengokturk.booklate.domain.model.WordAnalysis
import com.erengokturk.booklate.domain.model.WordLookupRecord

@Entity(
    tableName = "word_lookups",
    indices = [
        Index("normalizedWord"),
        Index("lastAccessedAt"),
        Index("isFavorite"),
    ],
)
data class WordLookupEntity(
    /** word + hedef dil + ana dil + istem sürümü üzerinden SHA-256. */
    @PrimaryKey val cacheKey: String,
    val word: String,
    val normalizedWord: String,
    val translation: String,
    val targetLanguage: String,
    val nativeLanguage: String,
    val sentence: String,
    val analysisJson: String,
    val bookTitle: String?,
    val createdAt: Long,
    val lastAccessedAt: Long,
    val hitCount: Int,
    val isFavorite: Boolean,
) {
    fun toDomain(): WordLookupRecord? {
        val analysis = AnalysisJson.decode(analysisJson) ?: return null
        return WordLookupRecord(
            cacheKey = cacheKey,
            word = word,
            targetLanguage = targetLanguage,
            nativeLanguage = nativeLanguage,
            sentence = sentence,
            analysis = analysis,
            createdAt = createdAt,
            lastAccessedAt = lastAccessedAt,
            hitCount = hitCount,
            bookTitle = bookTitle,
            isFavorite = isFavorite,
        )
    }

    companion object {
        fun from(
            cacheKey: String,
            word: String,
            normalizedWord: String,
            sentence: String,
            targetLanguage: String,
            nativeLanguage: String,
            analysis: WordAnalysis,
            bookTitle: String?,
            now: Long,
        ): WordLookupEntity = WordLookupEntity(
            cacheKey = cacheKey,
            word = word,
            normalizedWord = normalizedWord,
            translation = analysis.contextualTranslation,
            targetLanguage = targetLanguage,
            nativeLanguage = nativeLanguage,
            sentence = sentence,
            analysisJson = AnalysisJson.encode(analysis),
            bookTitle = bookTitle,
            createdAt = now,
            lastAccessedAt = now,
            hitCount = 0,
            isFavorite = false,
        )
    }
}
