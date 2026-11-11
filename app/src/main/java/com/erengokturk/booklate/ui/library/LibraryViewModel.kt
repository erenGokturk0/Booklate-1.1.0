package com.erengokturk.booklate.ui.library

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.erengokturk.booklate.core.LanguageCatalog
import com.erengokturk.booklate.data.library.LibraryRepository
import com.erengokturk.booklate.data.local.SettingsRepository
import com.erengokturk.booklate.di.AppContainer
import com.erengokturk.booklate.domain.model.Book
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class ImportState(
    val progress: Float,
    val isIndeterminate: Boolean,
)

data class LibraryUiState(
    val isLoading: Boolean = true,
    val books: List<Book> = emptyList(),
    val filteredBooks: List<Book> = emptyList(),
    val query: String = "",
    val importState: ImportState? = null,
    val deleteTarget: Book? = null,
    /** PDF seçildikten sonra dil seçimi bekleyen dosya. */
    val pendingImport: Uri? = null,
    val importLanguage: String = "ru",
    /** Dil değişikliği yapılan kitap. */
    val languageTarget: Book? = null,
)

class LibraryViewModel(
    private val libraryRepository: LibraryRepository,
    private val settingsRepository: SettingsRepository,
) : ViewModel() {

    private data class Extras(
        val importState: ImportState? = null,
        val deleteTarget: Book? = null,
        val pendingImport: Uri? = null,
        val importLanguage: String = "ru",
        val languageTarget: Book? = null,
        val defaultLanguage: String = "ru",
    )

    private val query = MutableStateFlow("")
    private val extras = MutableStateFlow(Extras())
    private val message = MutableStateFlow<String?>(null)

    val transientMessage: StateFlow<String?> = message

    private val books = libraryRepository.books.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = emptyList(),
    )

    init {
        viewModelScope.launch {
            settingsRepository.settings.collect { settings ->
                extras.update { it.copy(defaultLanguage = settings.targetLanguage) }
            }
        }
    }

    val uiState: StateFlow<LibraryUiState> = combine(
        books,
        query,
        extras,
    ) { allBooks, searchQuery, extra ->
        val filtered = if (searchQuery.isBlank()) {
            allBooks
        } else {
            val q = searchQuery.trim().lowercase()
            allBooks.filter { book ->
                book.title.lowercase().contains(q) ||
                    book.author?.lowercase()?.contains(q) == true ||
                    book.fileName.lowercase().contains(q)
            }
        }
        LibraryUiState(
            isLoading = false,
            books = allBooks,
            filteredBooks = filtered,
            query = searchQuery,
            importState = extra.importState,
            deleteTarget = extra.deleteTarget,
            pendingImport = extra.pendingImport,
            importLanguage = extra.importLanguage,
            languageTarget = extra.languageTarget,
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = LibraryUiState(),
    )

    fun setQuery(value: String) {
        query.value = value
    }

    /** PDF seçildikten sonra dil seçimi istenir. */
    fun onPdfPicked(uri: Uri) {
        extras.update {
            it.copy(
                pendingImport = uri,
                importLanguage = LanguageCatalog.byCode(it.defaultLanguage)?.code
                    ?: it.defaultLanguage,
            )
        }
    }

    fun cancelImport() {
        extras.update { it.copy(pendingImport = null) }
    }

    fun confirmImport(languageCode: String) {
        val current = extras.value
        val uri = current.pendingImport ?: return
        if (current.importState != null) return

        extras.update {
            it.copy(pendingImport = null, importState = ImportState(0f, isIndeterminate = true))
        }
        viewModelScope.launch {
            try {
                val book = libraryRepository.importBook(uri, languageCode) { progress ->
                    extras.update { extra ->
                        extra.copy(
                            importState = ImportState(
                                progress = progress,
                                isIndeterminate = progress <= 0f,
                            ),
                        )
                    }
                }
                settingsRepository.setDefaultBookLanguage(languageCode)
                extras.update { it.copy(importState = null) }
                val language = LanguageCatalog.resolve(book.languageCode)
                message.value =
                    "“${book.title}” kütüphaneye eklendi · ${language.flag} ${language.label}"
            } catch (error: Exception) {
                extras.update { it.copy(importState = null) }
                message.value = error.message ?: "Kitap eklenemedi."
            }
        }
    }

    fun requestDelete(book: Book?) {
        extras.update { it.copy(deleteTarget = book) }
    }

    fun confirmDelete() {
        val book = extras.value.deleteTarget ?: return
        extras.update { it.copy(deleteTarget = null) }
        viewModelScope.launch {
            runCatching { libraryRepository.deleteBook(book) }
                .onSuccess { message.value = "“${book.title}” kütüphaneden kaldırıldı." }
                .onFailure { message.value = "Kitap silinemedi: ${it.message}" }
        }
    }

    fun requestLanguageChange(book: Book?) {
        extras.update { it.copy(languageTarget = book) }
    }

    fun setBookLanguage(code: String) {
        val book = extras.value.languageTarget ?: return
        extras.update { it.copy(languageTarget = null) }
        viewModelScope.launch {
            runCatching {
                libraryRepository.updateLanguage(book.id, code)
                settingsRepository.setDefaultBookLanguage(code)
            }
                .onSuccess {
                    val language = LanguageCatalog.resolve(code)
                    message.value =
                        "“${book.title}” dili ${language.flag} ${language.label} olarak güncellendi."
                }
                .onFailure { message.value = "Dil güncellenemedi: ${it.message}" }
        }
    }

    fun consumeMessage() {
        message.value = null
    }

    companion object {
        fun factory(container: AppContainer): ViewModelProvider.Factory = viewModelFactory {
            initializer {
                LibraryViewModel(
                    libraryRepository = container.libraryRepository,
                    settingsRepository = container.settingsRepository,
                )
            }
        }
    }
}
