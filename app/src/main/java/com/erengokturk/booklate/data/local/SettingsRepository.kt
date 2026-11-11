package com.erengokturk.booklate.data.local

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.erengokturk.booklate.BuildConfig
import com.erengokturk.booklate.core.Models
import com.erengokturk.booklate.domain.model.UserSettings
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import java.io.IOException

private val Context.booklateDataStore: DataStore<Preferences> by preferencesDataStore(name = "booklate_settings")

class SettingsRepository(private val context: Context) {

    private object Keys {
        val apiKey = stringPreferencesKey("gemini_api_key")
        val model = stringPreferencesKey("gemini_model")
        val targetLanguage = stringPreferencesKey("target_language")
        val nativeLanguage = stringPreferencesKey("native_language")
        val ttsRate = floatPreferencesKey("tts_rate")
        val ttsPitch = floatPreferencesKey("tts_pitch")
        val ttsVoice = stringPreferencesKey("tts_voice")
        val themeMode = stringPreferencesKey("theme_mode")
        val keepScreenOn = booleanPreferencesKey("keep_screen_on")
        val renderQuality = floatPreferencesKey("render_quality")
        val showPageHints = booleanPreferencesKey("show_page_hints")
    }

    val settings: Flow<UserSettings> = context.booklateDataStore.data
        .catch { error ->
            if (error is IOException) emit(androidx.datastore.preferences.core.emptyPreferences())
            else throw error
        }
        .map { prefs ->
            UserSettings(
                geminiApiKey = prefs[Keys.apiKey]
                    ?: BuildConfig.GEMINI_API_KEY.orEmpty(),
                geminiModel = Models.normalizeModel(
                    prefs[Keys.model] ?: BuildConfig.DEFAULT_GEMINI_MODEL,
                ),
                targetLanguage = prefs[Keys.targetLanguage] ?: "ru",
                nativeLanguage = prefs[Keys.nativeLanguage] ?: "tr",
                ttsRate = prefs[Keys.ttsRate] ?: 0.92f,
                ttsPitch = prefs[Keys.ttsPitch] ?: 1.0f,
                ttsVoice = prefs[Keys.ttsVoice],
                themeMode = prefs[Keys.themeMode] ?: "system",
                keepScreenOn = prefs[Keys.keepScreenOn] ?: true,
                renderQuality = prefs[Keys.renderQuality] ?: 1.5f,
                showPageHints = prefs[Keys.showPageHints] ?: true,
            )
        }

    suspend fun setApiKey(value: String) = edit { it[Keys.apiKey] = value.trim() }

    suspend fun setModel(value: String) = edit { it[Keys.model] = value }

    /**
     * Yeni kitap eklerken önerilen kitap dili. Her kitabın kendi dili
     * olduğu için bu değer yalnızca varsayılan seçimdir.
     */
    suspend fun setDefaultBookLanguage(value: String) = edit {
        it[Keys.targetLanguage] = value
    }

    suspend fun setTargetLanguage(value: String) = setDefaultBookLanguage(value)

    suspend fun setNativeLanguage(value: String) = edit { it[Keys.nativeLanguage] = value }

    suspend fun setTtsRate(value: Float) = edit { it[Keys.ttsRate] = value.coerceIn(0.5f, 2.0f) }

    suspend fun setTtsPitch(value: Float) = edit { it[Keys.ttsPitch] = value.coerceIn(0.5f, 2.0f) }

    suspend fun setTtsVoice(value: String?) = edit {
        if (value.isNullOrBlank()) it.remove(Keys.ttsVoice) else it[Keys.ttsVoice] = value
    }

    suspend fun setThemeMode(value: String) = edit { it[Keys.themeMode] = value }

    suspend fun setKeepScreenOn(value: Boolean) = edit { it[Keys.keepScreenOn] = value }

    suspend fun setRenderQuality(value: Float) = edit { it[Keys.renderQuality] = value }

    suspend fun setShowPageHints(value: Boolean) = edit { it[Keys.showPageHints] = value }

    private suspend fun edit(block: (androidx.datastore.preferences.core.MutablePreferences) -> Unit) {
        context.booklateDataStore.edit { block(it) }
    }
}
