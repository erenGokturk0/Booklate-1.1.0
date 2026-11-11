package com.erengokturk.booklate.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.erengokturk.booklate.core.LanguageCatalog
import com.erengokturk.booklate.data.analysis.CacheStats
import com.erengokturk.booklate.data.analysis.WordAnalysisRepository
import com.erengokturk.booklate.data.local.SettingsRepository
import com.erengokturk.booklate.di.AppContainer
import com.erengokturk.booklate.domain.model.UserSettings
import com.erengokturk.booklate.tts.TtsManager
import com.erengokturk.booklate.tts.TtsVoiceOption
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

sealed interface ApiTestState {
    data object Idle : ApiTestState
    data object Testing : ApiTestState
    data class Success(val message: String) : ApiTestState
    data class Failure(val message: String) : ApiTestState
}

data class SettingsUiState(
    val settings: UserSettings = UserSettings(),
    val apiKeyDraft: String = "",
    val showApiKey: Boolean = false,
    val apiTest: ApiTestState = ApiTestState.Idle,
    val voices: List<TtsVoiceOption> = emptyList(),
    val ttsReady: Boolean = false,
    val ttsError: String? = null,
    val isTestingVoice: Boolean = false,
    val cacheStats: CacheStats = CacheStats(wordCount = 0, favoriteCount = 0),
    val message: String? = null,
    val showClearCacheConfirm: Boolean = false,
)

class SettingsViewModel(
    private val settingsRepository: SettingsRepository,
    private val analysisRepository: WordAnalysisRepository,
    private val ttsManager: TtsManager,
) : ViewModel() {

    private val _state = MutableStateFlow(SettingsUiState())
    val state: StateFlow<SettingsUiState> = _state.asStateFlow()

    private var apiKeyDirty = false

    init {
        viewModelScope.launch {
            settingsRepository.settings.collect { settings ->
                _state.update { current ->
                    current.copy(
                        settings = settings,
                        apiKeyDraft = if (apiKeyDirty) current.apiKeyDraft else settings.geminiApiKey,
                    )
                }
                ttsManager.applySettings(settings)
            }
        }

        viewModelScope.launch {
            ttsManager.state.collect { tts ->
                _state.update { current ->
                    current.copy(
                        voices = tts.voices,
                        ttsReady = tts.isReady,
                        ttsError = tts.error,
                        isTestingVoice = tts.isActive,
                    )
                }
            }
        }

        refreshCacheStats()
    }

    // --- Gemini -----------------------------------------------------------

    fun onApiKeyChange(value: String) {
        apiKeyDirty = true
        _state.update {
            it.copy(
                apiKeyDraft = value,
                apiTest = ApiTestState.Idle,
            )
        }
    }

    fun toggleApiKeyVisibility() {
        _state.update { it.copy(showApiKey = !it.showApiKey) }
    }

    fun saveApiKey() {
        val key = _state.value.apiKeyDraft.trim()
        viewModelScope.launch {
            settingsRepository.setApiKey(key)
            apiKeyDirty = false
            _state.update {
                it.copy(
                    apiKeyDraft = key,
                    message = if (key.isBlank()) {
                        "API anahtarı kaldırıldı. Kelime analizi için anahtar gerekir."
                    } else {
                        "API anahtarı bu cihazda kaydedildi."
                    },
                )
            }
        }
    }

    fun clearApiKey() {
        apiKeyDirty = false
        _state.update { it.copy(apiKeyDraft = "", apiTest = ApiTestState.Idle) }
        viewModelScope.launch {
            settingsRepository.setApiKey("")
            _state.update { it.copy(message = "API anahtarı kaldırıldı.") }
        }
    }

    fun testApiKey() {
        val key = _state.value.apiKeyDraft.trim()
        if (key.isBlank()) {
            _state.update {
                it.copy(apiTest = ApiTestState.Failure("Önce bir API anahtarı gir."))
            }
            return
        }
        if (_state.value.apiTest is ApiTestState.Testing) return

        _state.update { it.copy(apiTest = ApiTestState.Testing) }
        viewModelScope.launch {
            _state.update {
                it.copy(
                    apiTest = try {
                        if (analysisRepository.testApiKey(key)) {
                            ApiTestState.Success("Bağlantı başarılı. Anahtar çalışıyor.")
                        } else {
                            ApiTestState.Failure("Anahtar reddedildi. Değeri kontrol edip tekrar dene.")
                        }
                    } catch (error: Exception) {
                        ApiTestState.Failure(
                            error.message ?: "Bağlantı kurulamadı. İnternet bağlantını kontrol et.",
                        )
                    },
                )
            }
        }
    }

    fun setModel(modelId: String) {
        viewModelScope.launch { settingsRepository.setModel(modelId) }
    }

    // --- Diller -----------------------------------------------------------

    fun setNativeLanguage(code: String) {
        if (code == _state.value.settings.nativeLanguage) return
        viewModelScope.launch {
            settingsRepository.setNativeLanguage(code)
            val language = LanguageCatalog.resolve(code)
            _state.update {
                it.copy(message = "Ana dil ${language.label} olarak ayarlandı.")
            }
        }
    }

    // --- Okuma ve TTS -----------------------------------------------------

    fun setTtsRate(value: Float) {
        viewModelScope.launch { settingsRepository.setTtsRate(value) }
    }

    fun setTtsPitch(value: Float) {
        viewModelScope.launch { settingsRepository.setTtsPitch(value) }
    }

    fun setTtsVoice(voiceName: String?) {
        ttsManager.stop()
        viewModelScope.launch {
            settingsRepository.setTtsVoice(voiceName)
            _state.update { it.copy(message = "Ses seçimi güncellendi.") }
        }
    }

    fun testVoice() {
        val target = LanguageCatalog.resolve(_state.value.settings.targetLanguage)
        if (!_state.value.ttsReady) {
            _state.update {
                it.copy(message = "Konuşma motoru henüz hazır değil. Bir saniye sonra tekrar dene.")
            }
            return
        }
        ttsManager.stop()
        ttsManager.speakText(sampleTextFor(target.code))
    }

    fun stopVoiceTest() {
        ttsManager.stop()
    }

    fun setKeepScreenOn(value: Boolean) {
        viewModelScope.launch { settingsRepository.setKeepScreenOn(value) }
    }

    fun setShowPageHints(value: Boolean) {
        viewModelScope.launch { settingsRepository.setShowPageHints(value) }
    }

    fun setRenderQuality(value: Float) {
        viewModelScope.launch { settingsRepository.setRenderQuality(value) }
    }

    // --- Görünüm ----------------------------------------------------------

    fun setThemeMode(value: String) {
        viewModelScope.launch { settingsRepository.setThemeMode(value) }
    }

    // --- Veri -------------------------------------------------------------

    fun refreshCacheStats() {
        viewModelScope.launch {
            _state.update { it.copy(cacheStats = analysisRepository.stats()) }
        }
    }

    fun requestClearCache() {
        _state.update { it.copy(showClearCacheConfirm = true) }
    }

    fun dismissClearCache() {
        _state.update { it.copy(showClearCacheConfirm = false) }
    }

    fun confirmClearCache() {
        viewModelScope.launch {
            analysisRepository.clearAll()
            refreshCacheStats()
            _state.update {
                it.copy(
                    showClearCacheConfirm = false,
                    message = "Kelime önbelleği ve favoriler temizlendi.",
                )
            }
        }
    }

    fun consumeMessage() {
        _state.update { it.copy(message = null) }
    }

    private fun sampleTextFor(code: String): String = when (code) {
        "ru" -> "Привет! Это проверка голоса Booklate."
        "en" -> "Hello! This is a Booklate voice check."
        "tr" -> "Merhaba! Bu bir Booklate ses kontrolü."
        "de" -> "Hallo! Dies ist ein Booklate-Stimmentest."
        "fr" -> "Bonjour ! Ceci est un test de la voix Booklate."
        "es" -> "¡Hola! Esta es una prueba de voz de Booklate."
        "it" -> "Ciao! Questa è una prova vocale di Booklate."
        "ja" -> "こんにちは！これはBooklateの音声テストです。"
        "az" -> "Salam! Bu Booklate səs yoxlamasıdır."
        "kk" -> "Сәлем! Бұл Booklate дыбыс тексерісі."
        "tk" -> "Salam! Bu Booklate ses barlagy."
        "uz" -> "Salom! Bu Booklate ovoz sinovi."
        "ky" -> "Салам! Бул Booklate үн текшерүүсү."
        "zh" -> "你好！这是 Booklate 的语音测试。"
        "pl" -> "Cześć! To jest test głosu Booklate."
        else -> "Booklate"
    }

    companion object {
        fun factory(container: AppContainer): ViewModelProvider.Factory = viewModelFactory {
            initializer {
                SettingsViewModel(
                    settingsRepository = container.settingsRepository,
                    analysisRepository = container.wordAnalysisRepository,
                    ttsManager = container.ttsManager,
                )
            }
        }
    }
}
