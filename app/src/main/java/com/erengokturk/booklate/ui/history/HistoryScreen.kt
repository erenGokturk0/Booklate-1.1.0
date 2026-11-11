package com.erengokturk.booklate.ui.history

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material.icons.outlined.History
import androidx.compose.material.icons.outlined.MoreVert
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.Star
import androidx.compose.material.icons.outlined.StarBorder
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.erengokturk.booklate.core.Formatters
import com.erengokturk.booklate.core.LanguageCatalog
import com.erengokturk.booklate.di.LocalAppContainer
import com.erengokturk.booklate.domain.model.WordLookupRecord
import com.erengokturk.booklate.ui.analysis.WordAnalysisDetails
import com.erengokturk.booklate.ui.components.EmptyState

@Composable
fun HistoryRoute(onOpenSettings: () -> Unit) {
    val container = LocalAppContainer.current
    val viewModel: HistoryViewModel = viewModel(factory = HistoryViewModel.factory(container))
    val records by viewModel.records.collectAsStateWithLifecycle()
    val stats by viewModel.stats.collectAsStateWithLifecycle()
    val query by viewModel.queryText.collectAsStateWithLifecycle()
    val favoritesOnly by viewModel.favoritesFilter.collectAsStateWithLifecycle()
    val selected by viewModel.selected.collectAsStateWithLifecycle()
    val message by viewModel.message.collectAsStateWithLifecycle()
    val showClear by viewModel.showClearConfirm.collectAsStateWithLifecycle()

    val snackbar = remember { SnackbarHostState() }
    LaunchedEffect(message) {
        message?.let {
            snackbar.showSnackbar(it)
            viewModel.consumeMessage()
        }
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        contentWindowInsets = androidx.compose.foundation.layout.WindowInsets(0, 0, 0, 0),
        snackbarHost = { SnackbarHost(snackbar) },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .statusBarsPadding(),
        ) {
            HistoryHeader(
                wordCount = stats.wordCount,
                favoriteCount = stats.favoriteCount,
                onClearAll = viewModel::requestClearAll,
            )
            OutlinedTextField(
                value = query,
                onValueChange = viewModel::setQuery,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp),
                placeholder = { Text("Kelime, çeviri veya cümle ara") },
                leadingIcon = { Icon(Icons.Outlined.Search, contentDescription = null) },
                trailingIcon = {
                    if (query.isNotEmpty()) {
                        IconButton(onClick = { viewModel.setQuery("") }) {
                            Icon(Icons.Outlined.Close, contentDescription = "Temizle")
                        }
                    }
                },
                singleLine = true,
                shape = RoundedCornerShape(16.dp),
            )
            Spacer(Modifier.height(10.dp))
            Row(
                modifier = Modifier.padding(horizontal = 20.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                FilterChip(
                    selected = !favoritesOnly,
                    onClick = { viewModel.setFavoritesOnly(false) },
                    label = { Text("Tümü") },
                )
                FilterChip(
                    selected = favoritesOnly,
                    onClick = { viewModel.setFavoritesOnly(true) },
                    label = { Text("Favoriler · ${stats.favoriteCount}") },
                    leadingIcon = {
                        Icon(
                            if (favoritesOnly) Icons.Outlined.Star else Icons.Outlined.StarBorder,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp),
                        )
                    },
                )
            }
            Spacer(Modifier.height(8.dp))

            if (records.isEmpty()) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
                    EmptyState(
                        title = if (favoritesOnly) "Henüz favori kelime yok" else "Kelime geçmişi boş",
                        message = if (favoritesOnly) {
                            "Bir kelimeyi inceledikten sonra yıldız simgesine dokunarak favorilere ekleyebilirsin."
                        } else {
                            "Okurken bir kelimeye dokun. Gemini'nin bağlamsal analizi burada saklanır ve kota harcamadan tekrar açılabilir."
                        },
                        icon = Icons.Outlined.History,
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(start = 20.dp, end = 20.dp, bottom = 24.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    items(records, key = { it.cacheKey }) { record ->
                        HistoryCard(
                            record = record,
                            onClick = { viewModel.open(record) },
                            onFavorite = { viewModel.toggleFavorite(record) },
                            onDelete = { viewModel.delete(record) },
                        )
                    }
                }
            }
        }

        selected?.let { record ->
            ModalBottomSheet(
                onDismissRequest = viewModel::closeDetail,
                containerColor = MaterialTheme.colorScheme.surface,
                dragHandle = null,
            ) {
                HistoryDetail(
                    record = record,
                    onClose = viewModel::closeDetail,
                    onFavorite = { viewModel.toggleFavorite(record) },
                )
            }
        }

        if (showClear) {
            AlertDialog(
                onDismissRequest = viewModel::dismissClearAll,
                title = { Text("Tüm kelime geçmişi silinsin mi?") },
                text = {
                    Text("Önbellekteki ${stats.wordCount} kelime ve favoriler silinir. Bu işlem geri alınamaz; kelimeler tekrar sorgulandığında Gemini kotası kullanılır.")
                },
                confirmButton = {
                    TextButton(onClick = viewModel::confirmClearAll) {
                        Text("Temizle", color = MaterialTheme.colorScheme.error)
                    }
                },
                dismissButton = {
                    TextButton(onClick = viewModel::dismissClearAll) { Text("Vazgeç") }
                },
            )
        }
    }
}

@Composable
private fun HistoryHeader(
    wordCount: Int,
    favoriteCount: Int,
    onClearAll: () -> Unit,
) {
    var menuOpen by remember { mutableStateOf(false) }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 20.dp, end = 8.dp, top = 8.dp, bottom = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text("Kelimeler", style = MaterialTheme.typography.headlineMedium)
            Text(
                text = "$wordCount kelime · $favoriteCount favori · cihazında saklı",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Box {
            IconButton(onClick = { menuOpen = true }) {
                Icon(Icons.Outlined.MoreVert, contentDescription = "Menü")
            }
            DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
                DropdownMenuItem(
                    text = { Text("Tüm geçmişi temizle") },
                    leadingIcon = { Icon(Icons.Outlined.DeleteOutline, contentDescription = null) },
                    onClick = {
                        menuOpen = false
                        onClearAll()
                    },
                )
            }
        }
    }
}

@Composable
private fun HistoryCard(
    record: WordLookupRecord,
    onClick: () -> Unit,
    onFavorite: () -> Unit,
    onDelete: () -> Unit,
) {
    val language = LanguageCatalog.target(record.targetLanguage)
    Surface(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.medium,
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(
            1.dp,
            MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.55f),
        ),
    ) {
        Column(Modifier.padding(start = 16.dp, end = 6.dp, top = 12.dp, bottom = 12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = language.flag,
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.padding(end = 8.dp),
                )
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = record.word,
                        style = MaterialTheme.typography.titleMedium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Text(
                        text = record.analysis.contextualTranslation.ifBlank {
                            record.analysis.dictionaryMeaning
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                IconButton(onClick = onFavorite, modifier = Modifier.size(36.dp)) {
                    Icon(
                        imageVector = if (record.isFavorite) Icons.Outlined.Star else Icons.Outlined.StarBorder,
                        contentDescription = if (record.isFavorite) "Favoriden çıkar" else "Favorilere ekle",
                        tint = if (record.isFavorite) MaterialTheme.colorScheme.tertiary
                        else MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(20.dp),
                    )
                }
                IconButton(onClick = onDelete, modifier = Modifier.size(36.dp)) {
                    Icon(
                        Icons.Outlined.DeleteOutline,
                        contentDescription = "Geçmişten sil",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(20.dp),
                    )
                }
            }
            Spacer(Modifier.height(6.dp))
            Text(
                text = "“${record.sentence}”",
                style = MaterialTheme.typography.bodySmall,
                fontStyle = FontStyle.Italic,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(end = 10.dp),
            )
            Spacer(Modifier.height(8.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = record.bookTitle ?: "Bilinmeyen kitap",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f),
                )
                Text(
                    text = Formatters.relative(record.lastAccessedAt),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(Modifier.width(10.dp))
            }
        }
    }
}

@Composable
private fun HistoryDetail(
    record: WordLookupRecord,
    onClose: () -> Unit,
    onFavorite: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .navigationBarsPadding()
            .padding(bottom = 28.dp),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 24.dp, end = 12.dp, top = 18.dp, bottom = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "KAYITLI ANALİZ",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary,
                    letterSpacing = androidx.compose.ui.unit.TextUnit.Unspecified,
                )
                Text(
                    text = record.word,
                    style = MaterialTheme.typography.headlineMedium,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = record.analysis.transcription,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            IconButton(onClick = onFavorite) {
                Icon(
                    imageVector = if (record.isFavorite) Icons.Outlined.Star else Icons.Outlined.StarBorder,
                    contentDescription = "Favori",
                    tint = if (record.isFavorite) MaterialTheme.colorScheme.tertiary
                    else MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            IconButton(onClick = onClose) {
                Icon(Icons.Outlined.Close, contentDescription = "Kapat")
            }
        }
        WordAnalysisDetails(
            outcome = record.toAnalysisOutcome(),
            currentContext = record.sentence,
            highlightText = record.word,
            contextMatches = true,
            onToggleFavorite = onFavorite,
            showContext = true,
        )
    }
}
