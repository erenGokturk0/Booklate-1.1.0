package com.erengokturk.booklate.ui.reader

import android.graphics.Bitmap
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.erengokturk.booklate.data.analysis.MissingApiKeyException
import com.erengokturk.booklate.data.analysis.WordAnalysisRepository
import com.erengokturk.booklate.data.library.LibraryRepository
import com.erengokturk.booklate.data.local.SettingsRepository
import com.erengokturk.booklate.data.pdf.PdfSession
import com.erengokturk.booklate.di.AppContainer
import com.erengokturk.booklate.domain.model.AnalysisOutcome
import com.erengokturk.booklate.domain.model.AnalysisRequest
import com.erengokturk.booklate.domain.model.Book
import com.erengokturk.booklate.domain.model.PageText
import com.erengokturk.booklate.domain.model.PageWord
import com.erengokturk.booklate.domain.model.ReaderSelection
import com.erengokturk.booklate.domain.model.UserSettings
import com.erengokturk.booklate.tts.TtsManager
import com.erengokturk.booklate.tts.TtsState
import com.erengokturk.booklate.tts.TtsTextBuilder
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.io.File
import kotlin.math.abs

data class PageUiState(
    val bitmap: Bitmap? = null,
    val text: PageText = PageText.EMPTY,
    val isLoading: Boolean = false,
    val isTextLoading: Boolean = false,
    val error: String? = null,
    val renderWidth: Int = 0,
)

sealed interface AnalysisUiState {
    data object Idle : AnalysisUiState
    data class Loading(val word: String) : AnalysisUiState
    data class Success(val outcome: AnalysisOutcome) : AnalysisUiState
    data class Error(
        val message: String,
        val selection: ReaderSelection,
        val missingApiKey: Boolean = false,
    ) : AnalysisUiState
}

data class ReaderUiState(
    val book: Book? = null,
    val loadingBook: Boolean = true,
    val bookError: String? = null,
    val pageCount: Int = 0,
    val currentPage: Int = 0,
    val pages: Map<Int, PageUiState> = emptyMap(),
    val selection: ReaderSelection? = null,
    val analysis: AnalysisUiState = AnalysisUiState.Idle,
    val tts: TtsState = TtsState(),
    val settings: UserSettings = UserSettings(),
    val transientError: String? = null,
) {
    val currentPageState: PageUiState
        get() = pages[currentPage] ?: PageUiState(isLoading = true)
}

class ReaderViewModel(
    private val bookId: String,
    private val libraryRepository: LibraryRepository,
    private val analysisRepository: WordAnalysisRepository,
    private val settingsRepository: SettingsRepository,
    private val ttsManager: TtsManager,
) : ViewModel() {

    private val _state = MutableStateFlow(ReaderUiState())
    val state: StateFlow<ReaderUiState> = _state.asStateFlow()

    private val sessionMutex = Mutex()
    private var session: PdfSession? = null
    private var book: Book? = null
    private val pageJobs = mutableMapOf<Int, Job>()
    private var analysisJob: Job? = null
    private var progressJob: Job? = null
    private var lastRequest: AnalysisRequest? = null

    init {
        observeSettings()
        observeTts()
        loadBook()
    }

    fun loadPage(index: Int, targetWidthPx: Int) {
        if (index < 0 || index >= _state.value.pageCount) return
        val existing = _state.value.pages[index]
        val sufficientBitmap = existing?.bitmap != null &&
            existing.renderWidth > 0 &&
            abs(existing.renderWidth - targetWidthPx) / existing.renderWidth.toFloat() < 0.18f
        val hasText = existing?.text?.words?.isNotEmpty() == true
        if (sufficientBitmap && hasText) return
        if (pageJobs[index]?.isActive == true) return

        pageJobs[index] = viewModelScope.launch {
            _state.update { current ->
                current.copy(
                    pages = current.pages + (index to (current.pages[index] ?: PageUiState()).copy(
                        isLoading = existing?.bitmap == null,
                        isTextLoading = !hasText,
                        error = null,
                    )),
                )
            }
            try {
                val pdf = ensureSession()
                val size = pdf.pageSize(index)
                val quality = _state.value.settings.renderQuality
                val bitmap = pdf.renderPage(index, targetWidthPx, quality)
                _state.update { current ->
                    val page = current.pages[index] ?: PageUiState()
                    current.copy(
                        pages = current.pages + (index to page.copy(
                            bitmap = bitmap,
                            isLoading = false,
                            renderWidth = targetWidthPx,
                            error = null,
                        )),
                    )
                }

                val text = pdf.extractPage(index, size)
                _state.update { current ->
                    val page = current.pages[index] ?: PageUiState()
                    current.copy(
                        pages = current.pages + (index to page.copy(
                            text = text,
                            isTextLoading = false,
                            error = null,
                        )),
                    )
                }
            } catch (error: Exception) {
                _state.update { current ->
                    val page = current.pages[index] ?: PageUiState()
                    current.copy(
                        pages = current.pages + (index to page.copy(
                            isLoading = false,
                            isTextLoading = false,
                            error = error.message ?: "Sayfa yüklenemedi.",
                        )),
                    )
                }
            }
        }
    }

    fun retryPage(index: Int, targetWidthPx: Int) {
        pageJobs.remove(index)?.cancel()
        _state.update { current ->
            val page = current.pages[index] ?: return@update current
            current.copy(pages = current.pages + (index to page.copy(error = null)))
        }
        loadPage(index, targetWidthPx)
    }

    fun onPageChanged(page: Int, offsetFraction: Float) {
        if (_state.value.book == null) return
        val safePage = page.coerceIn(0, (_state.value.pageCount - 1).coerceAtLeast(0))
        if (_state.value.tts.isActive) ttsManager.stop()
        _state.update { current ->
            // Uzak sayfaların bitmap'lerini bırak; 386 sayfalık kitaplarda
            // tüm sayfaları bellekte tutmak OOM'a yol açar. Metin katmanı kalır.
            val keepRange = (safePage - 2)..(safePage + 2)
            val prunedPages = if (current.pages.size > 5) {
                current.pages.mapValues { (index, page) ->
                    if (index !in keepRange && page.bitmap != null) {
                        page.copy(bitmap = null, renderWidth = 0)
                    } else {
                        page
                    }
                }
            } else {
                current.pages
            }
            current.copy(
                currentPage = safePage,
                pages = prunedPages,
                selection = null,
                analysis = AnalysisUiState.Idle,
            )
        }
        analysisJob?.cancel()
        progressJob?.cancel()
        progressJob = viewModelScope.launch {
            delay(350)
            libraryRepository.updateProgress(bookId, safePage, offsetFraction)
        }
    }

    fun onWordTapped(wordIndex: Int) {
        val page = _state.value.currentPageState
        val selection = buildSelection(page.text, wordIndex..wordIndex) ?: return
        if (_state.value.tts.isActive) ttsManager.stop()
        _state.update {
            it.copy(selection = selection, analysis = AnalysisUiState.Idle)
        }
        analyze(selection, forceRefresh = false)
    }

    fun onSelectionStarted(wordIndex: Int) {
        val page = _state.value.currentPageState
        val selection = buildSelection(page.text, wordIndex..wordIndex) ?: return
        if (_state.value.tts.isActive) ttsManager.stop()
        analysisJob?.cancel()
        _state.update {
            it.copy(selection = selection, analysis = AnalysisUiState.Idle)
        }
    }

    fun onSelectionExtended(startWord: Int, endWord: Int) {
        val page = _state.value.currentPageState
        val selection = buildSelection(page.text, startWord..endWord) ?: return
        _state.update { it.copy(selection = selection) }
    }

    fun onSelectionFinished(startWord: Int, endWord: Int) {
        val page = _state.value.currentPageState
        val selection = buildSelection(page.text, startWord..endWord) ?: return
        _state.update { it.copy(selection = selection) }
        // Uzun basış tek kelimeyse doğrudan analiz et; çoklu seçimde araç çubuğu bekler.
        if (selection.isSingleWord && _state.value.analysis is AnalysisUiState.Idle) {
            analyze(selection, forceRefresh = false)
        }
    }

    fun translateSelection() {
        val selection = _state.value.selection ?: return
        analyze(selection, forceRefresh = false)
    }

    fun refreshAnalysis() {
        val selection = _state.value.selection ?: return
        analyze(selection, forceRefresh = true)
    }

    fun retryAnalysis() {
        val error = _state.value.analysis as? AnalysisUiState.Error ?: return
        analyze(error.selection, forceRefresh = false)
    }

    fun clearSelection() {
        analysisJob?.cancel()
        _state.update { it.copy(selection = null, analysis = AnalysisUiState.Idle) }
    }

    fun toggleFavorite() {
        val success = _state.value.analysis as? AnalysisUiState.Success ?: return
        val newValue = !success.outcome.isFavorite
        viewModelScope.launch {
            analysisRepository.setFavorite(success.outcome.cacheKey, newValue)
            _state.update { current ->
                val currentSuccess = current.analysis as? AnalysisUiState.Success
                if (currentSuccess?.outcome?.cacheKey == success.outcome.cacheKey) {
                    current.copy(
                        analysis = AnalysisUiState.Success(
                            currentSuccess.outcome.copy(isFavorite = newValue),
                        ),
                    )
                } else {
                    current
                }
            }
        }
    }

    fun speakPage() {
        val page = _state.value.currentPageState
        val sentences = TtsTextBuilder.sentences(page.text)
        if (sentences.isEmpty()) {
            _state.update {
                it.copy(
                    transientError = if (page.isTextLoading) {
                        "Metin katmanı hazırlanıyor. Bir saniye sonra tekrar dene."
                    } else {
                        "Bu sayfada seslendirilecek metin bulunamadı. Taranmış PDF'lerde metin katmanı olmayabilir."
                    },
                )
            }
            return
        }
        ttsManager.speakSentences(sentences)
    }

    fun speakSelection() {
        val selection = _state.value.selection ?: return
        ttsManager.speakText(selection.text)
    }

    fun speakSelectedSentence() {
        val selection = _state.value.selection ?: return
        val page = _state.value.currentPageState
        val sentences = TtsTextBuilder.sentences(page.text, onlySentence = selection.sentenceIndex)
        if (sentences.isEmpty()) {
            _state.update { it.copy(transientError = "Bu cümle seslendirilemedi.") }
        } else {
            ttsManager.speakSentences(sentences)
        }
    }

    fun toggleTts() {
        when {
            _state.value.tts.isSpeaking -> ttsManager.pause()
            _state.value.tts.isPaused -> ttsManager.resume()
            else -> speakPage()
        }
    }

    fun stopTts() = ttsManager.stop()

    fun consumeError() {
        _state.update { it.copy(transientError = null) }
    }

    fun clearTtsError() = ttsManager.clearError()

    private fun analyze(selection: ReaderSelection, forceRefresh: Boolean) {
        val book = book ?: return
        val settings = _state.value.settings
        val request = AnalysisRequest(
            word = selection.text,
            sentence = selection.sentence,
            targetLanguage = book.languageCode,
            nativeLanguage = settings.nativeLanguage,
            bookTitle = book.title,
            pageNumber = _state.value.currentPage + 1,
        )
        lastRequest = request
        analysisJob?.cancel()
        _state.update {
            it.copy(analysis = AnalysisUiState.Loading(selection.text))
        }
        analysisJob = viewModelScope.launch {
            try {
                val outcome = analysisRepository.analyze(request, forceRefresh)
                _state.update { current ->
                    if (current.selection?.wordIndices == selection.wordIndices) {
                        current.copy(analysis = AnalysisUiState.Success(outcome))
                    } else {
                        current
                    }
                }
            } catch (error: MissingApiKeyException) {
                _state.update { current ->
                    current.copy(
                        analysis = AnalysisUiState.Error(
                            message = error.message ?: "Gemini API anahtarı gerekli.",
                            selection = selection,
                            missingApiKey = true,
                        ),
                    )
                }
            } catch (error: Exception) {
                _state.update { current ->
                    current.copy(
                        analysis = AnalysisUiState.Error(
                            message = error.message ?: "Analiz alınamadı.",
                            selection = selection,
                        ),
                    )
                }
            }
        }
    }

    private fun buildSelection(page: PageText, range: IntRange): ReaderSelection? {
        val start = range.first.coerceAtLeast(0)
        val end = range.last.coerceAtMost(page.words.lastIndex)
        if (page.words.isEmpty() || start > end) return null
        val words = page.words.subList(start, end + 1)
        if (words.isEmpty()) return null
        val text = words.joinToString(" ") { it.text }.trim()
        if (text.isBlank()) return null

        val firstSentenceIndex = words.first().sentenceIndex
        val lastSentenceIndex = words.last().sentenceIndex
        val context = if (firstSentenceIndex >= 0) {
            page.sentences
                .filter { it.index in firstSentenceIndex..lastSentenceIndex }
                .joinToString(" ") { it.text }
                .ifBlank { text }
        } else {
            text
        }

        return ReaderSelection(
            wordIndices = start..end,
            text = text,
            sentence = context,
            sentenceIndex = firstSentenceIndex,
        )
    }

    private suspend fun ensureSession(): PdfSession = sessionMutex.withLock {
        session ?: run {
            val currentBook = book ?: throw IllegalStateException("Kitap yüklenmedi.")
            PdfSession.open(File(currentBook.filePath)).also { session = it }
        }
    }

    private fun loadBook() {
        viewModelScope.launch {
            try {
                val loaded = libraryRepository.getBook(bookId)
                if (loaded == null) {
                    _state.update {
                        it.copy(
                            loadingBook = false,
                            bookError = "Kitap bulunamadı. Kütüphaneden kaldırılmış olabilir.",
                        )
                    }
                    return@launch
                }
                book = loaded
                _state.update {
                    it.copy(
                        book = loaded,
                        loadingBook = false,
                        pageCount = loaded.pageCount,
                        currentPage = loaded.safeLastPage,
                        bookError = null,
                    )
                }
                // TTS, kitabın kendi diline göre yapılandırılır.
                ttsManager.applySettings(_state.value.settings, loaded.languageCode)
                libraryRepository.touchOpened(bookId)
            } catch (error: Exception) {
                _state.update {
                    it.copy(
                        loadingBook = false,
                        bookError = error.message ?: "Kitap açılamadı.",
                    )
                }
            }
        }
    }

    private fun observeSettings() {
        viewModelScope.launch {
            settingsRepository.settings.collect { settings ->
                _state.update { it.copy(settings = settings) }
                ttsManager.applySettings(
                    settings,
                    targetLanguage = book?.languageCode ?: settings.targetLanguage,
                )
            }
        }
    }

    private fun observeTts() {
        viewModelScope.launch {
            ttsManager.state.collect { tts ->
                _state.update { it.copy(tts = tts) }
            }
        }
    }

    override fun onCleared() {
        analysisJob?.cancel()
        progressJob?.cancel()
        pageJobs.values.forEach { it.cancel() }
        ttsManager.stop()
        session?.close()
        session = null
        super.onCleared()
    }

    companion object {
        fun factory(bookId: String, container: AppContainer): ViewModelProvider.Factory =
            viewModelFactory {
                initializer {
                    ReaderViewModel(
                        bookId = bookId,
                        libraryRepository = container.libraryRepository,
                        analysisRepository = container.wordAnalysisRepository,
                        settingsRepository = container.settingsRepository,
                        ttsManager = container.ttsManager,
                    )
                }
            }
    }
}
