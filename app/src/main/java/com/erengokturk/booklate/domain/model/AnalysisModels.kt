package com.erengokturk.booklate.domain.model

data class AnalysisRequest(
    val word: String,
    val sentence: String,
    val targetLanguage: String,
    val nativeLanguage: String,
    val bookTitle: String,
    val pageNumber: Int,
)

data class AnalysisOutcome(
    val cacheKey: String,
    val word: String,
    val analysis: WordAnalysis,
    val fromCache: Boolean,
    val createdAt: Long,
    val hitCount: Int,
    val isFavorite: Boolean,
    val sentence: String,
    /** Önbellekteki cümle, şu anki bağlamla aynı mı? */
    val contextMatches: Boolean = true,
)

data class WordLookupRecord(
    val cacheKey: String,
    val word: String,
    val targetLanguage: String,
    val nativeLanguage: String,
    val sentence: String,
    val analysis: WordAnalysis,
    val createdAt: Long,
    val lastAccessedAt: Long,
    val hitCount: Int,
    val bookTitle: String?,
    val isFavorite: Boolean,
)
