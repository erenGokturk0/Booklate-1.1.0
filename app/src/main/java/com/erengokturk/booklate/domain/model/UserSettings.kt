package com.erengokturk.booklate.domain.model

data class UserSettings(
    val geminiApiKey: String = "",
    val geminiModel: String = "gemini-3.5-flash-lite",
    val targetLanguage: String = "ru",
    val nativeLanguage: String = "tr",
    val ttsRate: Float = 0.92f,
    val ttsPitch: Float = 1.0f,
    val ttsVoice: String? = null,
    val themeMode: String = "system",
    val keepScreenOn: Boolean = true,
    val renderQuality: Float = 1.5f,
    val showPageHints: Boolean = true,
)
