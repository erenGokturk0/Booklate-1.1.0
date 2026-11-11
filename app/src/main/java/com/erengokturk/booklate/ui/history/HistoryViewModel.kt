package com.erengokturk.booklate.ui.history

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.erengokturk.booklate.data.analysis.CacheStats
import com.erengokturk.booklate.data.analysis.WordAnalysisRepository
import com.erengokturk.booklate.di.AppContainer
import com.erengokturk.booklate.domain.model.AnalysisOutcome
import com.erengokturk.booklate.domain.model.WordLookupRecord
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

@OptIn(ExperimentalCoroutinesApi::class)
class HistoryViewModel(
    private val repository: WordAnalysisRepository,
) : ViewModel() {

    private val query = MutableStateFlow("")
    private val favoritesOnly = MutableStateFlow(false)
    private val _selected = MutableStateFlow<WordLookupRecord?>(null)
    private val _stats = MutableStateFlow(CacheStats(0, 0))
    private val _message = MutableStateFlow<String?>(null)
    private val _showClearConfirm = MutableStateFlow(false)

    val selected: StateFlow<WordLookupRecord?> = _selected.asStateFlow()
    val stats: StateFlow<CacheStats> = _stats.asStateFlow()
    val message: StateFlow<String?> = _message.asStateFlow()
    val showClearConfirm: StateFlow<Boolean> = _showClearConfirm.asStateFlow()
    val queryText: StateFlow<String> = query.asStateFlow()
    val favoritesFilter: StateFlow<Boolean> = favoritesOnly.asStateFlow()

    val records: StateFlow<List<WordLookupRecord>> =
        combine(query, favoritesOnly) { q, favorites -> q to favorites }
            .flatMapLatest { (q, favorites) ->
                if (favorites) repository.favorites() else repository.history(q)
            }
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5_000),
                initialValue = emptyList(),
            )

    init {
        refreshStats()
    }

    fun setQuery(value: String) {
        query.value = value
    }

    fun setFavoritesOnly(value: Boolean) {
        favoritesOnly.value = value
    }

    fun open(record: WordLookupRecord) {
        _selected.value = record
    }

    fun closeDetail() {
        _selected.value = null
    }

    fun toggleFavorite(record: WordLookupRecord) {
        viewModelScope.launch {
            val newValue = !record.isFavorite
            repository.setFavorite(record.cacheKey, newValue)
            if (_selected.value?.cacheKey == record.cacheKey) {
                _selected.value = _selected.value?.copy(isFavorite = newValue)
            }
            refreshStats()
        }
    }

    fun delete(record: WordLookupRecord) {
        viewModelScope.launch {
            repository.delete(record.cacheKey)
            if (_selected.value?.cacheKey == record.cacheKey) _selected.value = null
            _message.value = "“${record.word}” geçmişten silindi."
            refreshStats()
        }
    }

    fun requestClearAll() {
        _showClearConfirm.value = true
    }

    fun dismissClearAll() {
        _showClearConfirm.value = false
    }

    fun confirmClearAll() {
        _showClearConfirm.value = false
        viewModelScope.launch {
            repository.clearAll()
            _selected.value = null
            _message.value = "Kelime önbelleği ve geçmiş temizlendi."
            refreshStats()
        }
    }

    fun consumeMessage() {
        _message.value = null
    }

    private fun refreshStats() {
        viewModelScope.launch {
            _stats.value = runCatching { repository.stats() }.getOrDefault(CacheStats(0, 0))
        }
    }

    companion object {
        fun factory(container: AppContainer): ViewModelProvider.Factory = viewModelFactory {
            initializer { HistoryViewModel(container.wordAnalysisRepository) }
        }
    }
}

fun WordLookupRecord.toAnalysisOutcome(): AnalysisOutcome = AnalysisOutcome(
    cacheKey = cacheKey,
    word = word,
    analysis = analysis,
    fromCache = true,
    createdAt = createdAt,
    hitCount = hitCount,
    isFavorite = isFavorite,
    sentence = sentence,
    contextMatches = true,
)
