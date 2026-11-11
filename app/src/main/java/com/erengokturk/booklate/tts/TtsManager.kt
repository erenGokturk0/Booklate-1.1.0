package com.erengokturk.booklate.tts

import android.content.Context
import android.os.Bundle
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import android.speech.tts.Voice
import com.erengokturk.booklate.core.LanguageCatalog
import com.erengokturk.booklate.domain.model.UserSettings
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import java.util.Locale
import java.util.UUID
import java.util.concurrent.atomic.AtomicLong

/**
 * Cihazın yerleşik TTS motorunu sarar. Harici API veya ücret yoktur.
 *
 * Cümleler tek tek kuyruğa eklenir; UtteranceProgressListener.onStart ile
 * cümle, onRangeStart ile kelime vurgusu senkronize edilir. onRangeStart
 * desteklemeyen motorlarda yalnızca cümle vurgusu çalışır.
 */
class TtsManager(context: Context) {

    private val appContext = context.applicationContext
    private val _state = MutableStateFlow(TtsState())
    val state: StateFlow<TtsState> = _state.asStateFlow()

    private var engine: TextToSpeech? = null
    private var ready = false
    private var initializing = true

    private var sentences: List<TtsSentence> = emptyList()
    private var activeSentence: Int = -1
    private var pausedAtSentence: Int = 0
    private var pauseRequested = false
    private var sessionId: String = ""
    private val utteranceCounter = AtomicLong(0L)

    private var languageTag: String = "ru-RU"
    private var speechRate: Float = 0.92f
    private var pitch: Float = 1f
    private var selectedVoice: String? = null

    init {
        val created = TextToSpeech(appContext) { status ->
            initializing = false
            ready = status == TextToSpeech.SUCCESS
            if (ready) {
                configureEngine()
            } else {
                _state.update {
                    it.copy(
                        isReady = false,
                        isAvailable = false,
                        error = "Cihazda TTS motoru başlatılamadı. Android ayarlarından bir konuşma motoru yükle.",
                    )
                }
            }
        }
        engine = created
        if (!initializing && ready) configureEngine()
    }

    fun applySettings(
        settings: UserSettings,
        targetLanguage: String = settings.targetLanguage,
    ) {
        languageTag = LanguageCatalog.resolve(targetLanguage).ttsTag
        speechRate = settings.ttsRate
        pitch = settings.ttsPitch
        selectedVoice = settings.ttsVoice
        if (ready) configureEngine()
    }

    fun speakSentences(
        newSentences: List<TtsSentence>,
        startSentence: Int = 0,
    ) {
        if (newSentences.isEmpty()) {
            _state.update { it.copy(error = "Bu sayfada seslendirilecek metin bulunamadı.") }
            return
        }
        if (!ensureReady()) return

        stopInternal(keepSentences = false)
        sentences = newSentences
        pauseRequested = false
        pausedAtSentence = startSentence.coerceIn(0, newSentences.lastIndex)
        activeSentence = pausedAtSentence
        sessionId = UUID.randomUUID().toString()

        _state.update {
            it.copy(
                isSpeaking = true,
                isPaused = false,
                currentSentenceIndex = pausedAtSentence,
                activeWordIndex = null,
                error = null,
            )
        }
        enqueueFrom(pausedAtSentence)
    }

    fun speakSentence(sentence: TtsSentence) {
        speakSentences(listOf(sentence), startSentence = 0)
    }

    fun speakText(text: String) {
        if (!ensureReady()) return
        val clean = text.trim()
        if (clean.isEmpty()) return
        stopInternal(keepSentences = false)
        sessionId = "single-${utteranceCounter.incrementAndGet()}"
        pauseRequested = false
        _state.update {
            it.copy(
                isSpeaking = true,
                isPaused = false,
                currentSentenceIndex = -1,
                activeWordIndex = null,
                error = null,
            )
        }
        val params = Bundle().apply {
            putFloat(TextToSpeech.Engine.KEY_PARAM_VOLUME, 1f)
        }
        engine?.speak(clean, TextToSpeech.QUEUE_FLUSH, params, "$sessionId:single")
    }

    fun pause() {
        if (!_state.value.isSpeaking) return
        pauseRequested = true
        pausedAtSentence = activeSentence.coerceAtLeast(0)
        engine?.stop()
        _state.update {
            it.copy(
                isSpeaking = false,
                isPaused = true,
                activeWordIndex = null,
            )
        }
    }

    fun resume() {
        if (!_state.value.isPaused || sentences.isEmpty()) return
        pauseRequested = false
        val start = pausedAtSentence.coerceIn(0, sentences.lastIndex)
        activeSentence = start
        _state.update {
            it.copy(
                isSpeaking = true,
                isPaused = false,
                currentSentenceIndex = start,
                activeWordIndex = null,
                error = null,
            )
        }
        enqueueFrom(start)
    }

    fun stop() {
        stopInternal(keepSentences = false)
        _state.update {
            it.copy(
                isSpeaking = false,
                isPaused = false,
                currentSentenceIndex = -1,
                activeWordIndex = null,
            )
        }
    }

    fun clearError() {
        _state.update { it.copy(error = null) }
    }

    fun availableVoices(): List<TtsVoiceOption> = _state.value.voices

    fun shutdown() {
        stopInternal(keepSentences = false)
        runCatching { engine?.shutdown() }
        engine = null
        ready = false
        _state.update { it.copy(isReady = false, isSpeaking = false, isPaused = false) }
    }

    private fun enqueueFrom(start: Int) {
        val target = engine ?: return
        val params = Bundle().apply {
            putFloat(TextToSpeech.Engine.KEY_PARAM_VOLUME, 1f)
        }

        for (index in start until sentences.size) {
            val sentence = sentences[index]
            val mode = if (index == start) TextToSpeech.QUEUE_FLUSH else TextToSpeech.QUEUE_ADD
            val utteranceId = "$sessionId:$index"
            target.speak(sentence.text, mode, params, utteranceId)
        }
    }

    private fun ensureReady(): Boolean {
        if (!ready) {
            _state.update {
                it.copy(
                    error = if (initializing) "Konuşma motoru hazırlanıyor, bir saniye sonra tekrar dene."
                    else "Konuşma motoru kullanılamıyor. Ayarlardan TTS ses paketini kontrol et.",
                )
            }
            return false
        }
        return true
    }

    private fun configureEngine() {
        val target = engine ?: return
        val locale = Locale.forLanguageTag(languageTag)
        val languageResult = target.setLanguage(locale)
        val languageAvailable = languageResult != TextToSpeech.LANG_MISSING_DATA &&
            languageResult != TextToSpeech.LANG_NOT_SUPPORTED

        target.setSpeechRate(speechRate.coerceIn(0.5f, 2.0f))
        target.setPitch(pitch.coerceIn(0.5f, 2.0f))
        target.setOnUtteranceProgressListener(progressListener)

        val voices = target.voices
            ?.filter { it.locale.language.equals(locale.language, ignoreCase = true) }
            ?.sortedBy { it.name }
            ?.map { voice -> voice.toOption() }
            .orEmpty()

        val voice = selectedVoice?.let { name ->
            target.voices?.firstOrNull { it.name == name }
        }
        if (voice != null) {
            runCatching { target.setVoice(voice) }
        }

        _state.update {
            it.copy(
                isReady = true,
                isAvailable = true,
                languageAvailable = languageAvailable,
                voices = voices,
                error = if (languageAvailable) null else
                    "Bu dil için cihazda ses paketi bulunamadı. Android Ayarlar → Diller → Metin okuma çıktısı bölümünden yükleyebilirsin.",
            )
        }
    }

    private val progressListener = object : UtteranceProgressListener() {
        override fun onStart(utteranceId: String?) {
            val index = utteranceSentenceIndex(utteranceId) ?: return
            activeSentence = index
            pausedAtSentence = index
            _state.update {
                it.copy(
                    isSpeaking = true,
                    isPaused = false,
                    currentSentenceIndex = index,
                    activeWordIndex = null,
                )
            }
        }

        override fun onDone(utteranceId: String?) {
            if (pauseRequested) return
            val isSingle = utteranceId?.endsWith(":single") == true
            val index = utteranceSentenceIndex(utteranceId)
            if (isSingle || (index != null && index >= sentences.lastIndex)) {
                _state.update {
                    it.copy(
                        isSpeaking = false,
                        isPaused = false,
                        currentSentenceIndex = -1,
                        activeWordIndex = null,
                    )
                }
            }
        }

        @Deprecated("Deprecated in Java")
        override fun onError(utteranceId: String?) {
            handleSpeechError(utteranceId)
        }

        override fun onError(utteranceId: String?, errorCode: Int) {
            handleSpeechError(utteranceId)
        }

        override fun onRangeStart(utteranceId: String?, start: Int, end: Int, frame: Int) {
            val index = utteranceSentenceIndex(utteranceId) ?: return
            val sentence = sentences.getOrNull(index) ?: return
            val word = sentence.words.firstOrNull { start >= it.startChar && start < it.endChar }
                ?: sentence.words.minByOrNull { range ->
                    val distance = when {
                        start < range.startChar -> range.startChar - start
                        start > range.endChar -> start - range.endChar
                        else -> 0
                    }
                    distance
                }
            if (word != null) {
                _state.update { it.copy(activeWordIndex = word.wordIndex) }
            }
        }
    }

    private fun handleSpeechError(utteranceId: String?) {
        pauseRequested = false
        _state.update {
            it.copy(
                isSpeaking = false,
                isPaused = false,
                activeWordIndex = null,
                error = "Seslendirme sırasında bir hata oluştu. Farklı bir ses veya hız dene.",
            )
        }
    }

    private fun utteranceSentenceIndex(utteranceId: String?): Int? {
        if (utteranceId == null) return null
        val suffix = utteranceId.substringAfterLast(':')
        return suffix.toIntOrNull()
    }

    private fun stopInternal(keepSentences: Boolean) {
        pauseRequested = false
        runCatching { engine?.stop() }
        activeSentence = -1
        if (!keepSentences) {
            sentences = emptyList()
            pausedAtSentence = 0
        }
    }

    private fun Voice.toOption(): TtsVoiceOption {
        val network = isNetworkConnectionRequired
        val qualityLabel = when {
            quality >= Voice.QUALITY_VERY_HIGH -> "çok yüksek"
            quality >= Voice.QUALITY_HIGH -> "yüksek"
            quality >= Voice.QUALITY_NORMAL -> "normal"
            else -> "düşük"
        }
        val base = locale.displayName.replaceFirstChar {
            if (it.isLowerCase()) it.titlecase() else it.toString()
        }
        return TtsVoiceOption(
            name = name,
            label = "$base · $qualityLabel${if (network) " · çevrimiçi" else ""}",
            requiresNetwork = network,
        )
    }
}
