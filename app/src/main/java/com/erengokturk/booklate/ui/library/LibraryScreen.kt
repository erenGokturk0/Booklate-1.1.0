package com.erengokturk.booklate.ui.library

import android.content.Intent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.automirrored.outlined.ArrowForward
import androidx.compose.material.icons.outlined.AutoStories
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Language
import androidx.compose.material.icons.outlined.MoreVert
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.verticalScroll
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.erengokturk.booklate.R
import com.erengokturk.booklate.core.Formatters
import com.erengokturk.booklate.core.LanguageCatalog
import com.erengokturk.booklate.di.LocalAppContainer
import com.erengokturk.booklate.domain.model.Book
import com.erengokturk.booklate.ui.components.BookCover
import com.erengokturk.booklate.ui.components.EmptyState

@Composable
fun LibraryRoute(
    onOpenBook: (String) -> Unit,
    onOpenSettings: () -> Unit,
) {
    val container = LocalAppContainer.current
    val viewModel: LibraryViewModel = viewModel(factory = LibraryViewModel.factory(container))
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val message by viewModel.transientMessage.collectAsStateWithLifecycle()
    val context = LocalContext.current

    val picker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument(),
    ) { uri ->
        if (uri != null) {
            runCatching {
                context.contentResolver.takePersistableUriPermission(
                    uri,
                    Intent.FLAG_GRANT_READ_URI_PERMISSION,
                )
            }
            viewModel.onPdfPicked(uri)
        }
    }

    LibraryScreen(
        state = state,
        message = message,
        onQueryChange = viewModel::setQuery,
        onAddPdf = { picker.launch(arrayOf("application/pdf")) },
        onOpenBook = onOpenBook,
        onDeleteRequest = viewModel::requestDelete,
        onConfirmDelete = viewModel::confirmDelete,
        onMessageShown = viewModel::consumeMessage,
        onOpenSettings = onOpenSettings,
        onConfirmImport = viewModel::confirmImport,
        onCancelImport = viewModel::cancelImport,
        onLanguageChangeRequest = viewModel::requestLanguageChange,
        onBookLanguageChange = viewModel::setBookLanguage,
    )
}

@Composable
private fun LibraryScreen(
    state: LibraryUiState,
    message: String?,
    onQueryChange: (String) -> Unit,
    onAddPdf: () -> Unit,
    onOpenBook: (String) -> Unit,
    onDeleteRequest: (Book?) -> Unit,
    onConfirmDelete: () -> Unit,
    onMessageShown: () -> Unit,
    onOpenSettings: () -> Unit,
    onConfirmImport: (String) -> Unit,
    onCancelImport: () -> Unit,
    onLanguageChangeRequest: (Book?) -> Unit,
    onBookLanguageChange: (String) -> Unit,
) {
    val snackbar = remember { SnackbarHostState() }
    var showInfo by remember { mutableStateOf(false) }

    LaunchedEffect(message) {
        if (message != null) {
            snackbar.showSnackbar(message)
            onMessageShown()
        }
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        contentWindowInsets = androidx.compose.foundation.layout.WindowInsets(0, 0, 0, 0),
        snackbarHost = { SnackbarHost(snackbar) },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = onAddPdf,
                icon = { Icon(Icons.Outlined.Add, contentDescription = null) },
                text = { Text("PDF ekle") },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
            )
        },
    ) { padding ->
        LazyVerticalGrid(
            columns = GridCells.Adaptive(minSize = 148.dp),
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .statusBarsPadding(),
            contentPadding = PaddingValues(
                start = 20.dp,
                end = 20.dp,
                top = 12.dp,
                bottom = 108.dp,
            ),
            horizontalArrangement = Arrangement.spacedBy(18.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp),
        ) {
            item(span = { GridItemSpan(maxLineSpan) }) {
                LibraryHeader(
                    books = state.books,
                    query = state.query,
                    onQueryChange = onQueryChange,
                    onShowInfo = { showInfo = true },
                )
            }

            val continueBook = state.books.firstOrNull()
            if (continueBook != null && state.query.isBlank()) {
                item(span = { GridItemSpan(maxLineSpan) }) {
                    ContinueReadingCard(book = continueBook, onClick = { onOpenBook(continueBook.id) })
                }
            }

            if (state.isLoading) {
                item(span = { GridItemSpan(maxLineSpan) }) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 40.dp),
                        contentAlignment = Alignment.Center,
                    ) { CircularProgressIndicator() }
                }
            } else if (state.filteredBooks.isEmpty()) {
                item(span = { GridItemSpan(maxLineSpan) }) {
                    EmptyState(
                        title = if (state.query.isBlank()) "Kütüphanen henüz boş" else "Sonuç bulunamadı",
                        message = if (state.query.isBlank()) {
                            "PDF formatındaki kitabını ekle; bir kelimeye dokunarak bağlamsal çevirisini, dil bilgisini ve eklerini anında öğren."
                        } else {
                            "Farklı bir kitap adı veya yazar adı dene."
                        },
                        actionLabel = if (state.query.isBlank()) "İlk kitabını ekle" else null,
                        onAction = if (state.query.isBlank()) onAddPdf else null,
                    )
                }
            } else {
                items(
                    items = state.filteredBooks,
                    key = { it.id },
                ) { book ->
                    BookCard(
                        book = book,
                        onClick = { onOpenBook(book.id) },
                        onDelete = { onDeleteRequest(book) },
                        onChangeLanguage = { onLanguageChangeRequest(book) },
                    )
                }
            }
        }

        state.importState?.let { importing ->
            AlertDialog(
                onDismissRequest = {},
                icon = { CircularProgressIndicator(modifier = Modifier.size(30.dp), strokeWidth = 2.5.dp) },
                title = { Text("Kitap ekleniyor") },
                text = {
                    Column {
                        Text(
                            text = "PDF cihazına kopyalanıyor ve ilk sayfa kapağı hazırlanıyor.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        Spacer(Modifier.height(16.dp))
                        if (importing.isIndeterminate) {
                            LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                        } else {
                            LinearProgressIndicator(
                                progress = { importing.progress },
                                modifier = Modifier.fillMaxWidth(),
                            )
                        }
                    }
                },
                confirmButton = {
                    TextButton(onClick = {}, enabled = false) {
                        Text(if (importing.isIndeterminate) "Kopyalanıyor…" else "%${(importing.progress * 100).toInt()}")
                    }
                },
            )
        }

        state.deleteTarget?.let { book ->
            AlertDialog(
                onDismissRequest = { onDeleteRequest(null) },
                title = { Text("Kitabı sil?") },
                text = {
                    Text("“${book.title}” kütüphaneden ve cihazdan kaldırılacak. Orijinal PDF dosyana dokunulmaz.")
                },
                confirmButton = {
                    TextButton(onClick = onConfirmDelete) {
                        Text("Sil", color = MaterialTheme.colorScheme.error)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { onDeleteRequest(null) }) { Text("Vazgeç") }
                },
            )
        }

        state.pendingImport?.let {
            LanguagePickerDialog(
                title = "Kitap hangi dilde?",
                message = "Seçtiğin dil yalnızca bu kitap için geçerli olur. Kelime analizi ve seslendirme bu dile göre yapılır.",
                initialSelection = state.importLanguage,
                confirmLabel = "Kitabı ekle",
                onConfirm = onConfirmImport,
                onDismiss = onCancelImport,
            )
        }

        state.languageTarget?.let { book ->
            LanguagePickerDialog(
                title = "Kitabın dilini değiştir",
                message = "“${book.title}” için yeni dil. Yeni analizler seçtiğin dilde yapılır; önbellekteki eski sonuçlar korunur.",
                initialSelection = book.languageCode,
                confirmLabel = "Kaydet",
                onConfirm = onBookLanguageChange,
                onDismiss = { onLanguageChangeRequest(null) },
            )
        }

        if (showInfo) {
            AlertDialog(
                onDismissRequest = { showInfo = false },
                icon = { Icon(Icons.Outlined.AutoStories, contentDescription = null) },
                title = { Text("Booklate nasıl çalışır?") },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        InfoLine("1", "PDF kitabını kütüphaneye ekle. Dosya cihazında kalır.")
                        InfoLine("2", "Okurken tek bir kelimeye dokun; Gemini bağlamsal çeviriyi, dil bilgisini ve ekleri açıklar.")
                        InfoLine("3", "Uzun basıp sürükleyerek birden fazla kelime seçebilirsin.")
                        InfoLine("4", "Seslendir düğmesiyle sayfayı cihazının TTS motoru okur; kelimeler eşzamanlı vurgulanır.")
                        Text(
                            text = "Sorgulanan kelimeler cihazda önbelleğe alınır; aynı kelime için tekrar API kotası harcanmaz.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                },
                confirmButton = {
                    TextButton(onClick = { showInfo = false }) { Text("Anladım") }
                },
            )
        }
    }
}

@Composable
private fun LibraryHeader(
    books: List<Book>,
    query: String,
    onQueryChange: (String) -> Unit,
    onShowInfo: () -> Unit,
) {
    val totalPages = remember(books) { books.sumOf { it.pageCount } }
    Column {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Kütüphanem",
                    style = MaterialTheme.typography.headlineMedium,
                )
                Text(
                    text = if (books.isEmpty()) stringResource(R.string.app_tagline)
                    else "${books.size} kitap · $totalPages sayfa",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            IconButton(onClick = onShowInfo) {
                Icon(
                    Icons.Outlined.Info,
                    contentDescription = "Nasıl çalışır?",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        Spacer(Modifier.height(18.dp))
        OutlinedTextField(
            value = query,
            onValueChange = onQueryChange,
            modifier = Modifier.fillMaxWidth(),
            placeholder = { Text("Kitap veya yazar ara") },
            leadingIcon = { Icon(Icons.Outlined.Search, contentDescription = null) },
            trailingIcon = {
                if (query.isNotEmpty()) {
                    IconButton(onClick = { onQueryChange("") }) {
                        Icon(Icons.Outlined.Close, contentDescription = "Temizle")
                    }
                }
            },
            singleLine = true,
            shape = RoundedCornerShape(16.dp),
        )
    }
}

@Composable
private fun ContinueReadingCard(book: Book, onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        shape = MaterialTheme.shapes.large,
        color = MaterialTheme.colorScheme.secondaryContainer,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            BookCover(
                coverPath = book.coverPath,
                title = book.title,
                modifier = Modifier
                    .width(58.dp)
                    .aspectRatio(0.68f),
                cornerRadius = 8.dp,
            )
            Spacer(Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "OKUMAYA DEVAM ET",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.7f),
                    fontWeight = FontWeight.Bold,
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    text = book.title,
                    style = MaterialTheme.typography.titleMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Spacer(Modifier.height(8.dp))
                LinearProgressIndicator(
                    progress = { book.progress },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(4.dp)
                        .clip(CircleShape),
                    color = MaterialTheme.colorScheme.primary,
                    trackColor = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.12f),
                )
                Spacer(Modifier.height(6.dp))
                Text(
                    text = if (book.progress >= 1f) "Tamamlandı"
                    else "Sayfa ${book.displayPage} / ${book.pageCount}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.8f),
                )
            }
            Spacer(Modifier.width(8.dp))
            Icon(
                Icons.AutoMirrored.Outlined.ArrowForward,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSecondaryContainer,
            )
        }
    }
}

@Composable
private fun BookCard(
    book: Book,
    onClick: () -> Unit,
    onDelete: () -> Unit,
    onChangeLanguage: () -> Unit,
) {
    var menuOpen by remember { mutableStateOf(false) }
    val language = LanguageCatalog.resolve(book.languageCode)

    Column(modifier = Modifier.fillMaxWidth()) {
        Box {
            BookCover(
                coverPath = book.coverPath,
                title = book.title,
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(0.68f)
                    .clickable(onClick = onClick),
            )
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(6.dp),
            ) {
                IconButton(
                    onClick = { menuOpen = true },
                    modifier = Modifier
                        .size(32.dp)
                        .background(Color.Black.copy(alpha = 0.32f), CircleShape),
                ) {
                    Icon(
                        Icons.Outlined.MoreVert,
                        contentDescription = "Kitap menüsü",
                        tint = Color.White,
                        modifier = Modifier.size(18.dp),
                    )
                }
                DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
                    DropdownMenuItem(
                        text = { Text("Dili değiştir · ${language.label}") },
                        leadingIcon = { Icon(Icons.Outlined.Language, contentDescription = null) },
                        onClick = {
                            menuOpen = false
                            onChangeLanguage()
                        },
                    )
                    DropdownMenuItem(
                        text = { Text("Kütüphaneden sil") },
                        leadingIcon = { Icon(Icons.Outlined.DeleteOutline, contentDescription = null) },
                        onClick = {
                            menuOpen = false
                            onDelete()
                        },
                    )
                }
            }
            if (book.progress > 0f) {
                LinearProgressIndicator(
                    progress = { book.progress },
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .fillMaxWidth()
                        .height(4.dp),
                    color = MaterialTheme.colorScheme.primary,
                    trackColor = Color.Black.copy(alpha = 0.25f),
                )
            }
        }
        Spacer(Modifier.height(10.dp))
        Text(
            text = book.title,
            style = MaterialTheme.typography.titleSmall,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
        )
        Spacer(Modifier.height(3.dp))
        Text(
            text = book.author ?: "${book.pageCount} sayfa",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        Spacer(Modifier.height(4.dp))
        Text(
            text = "${language.flag} ${language.label}",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.primary,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
private fun LanguagePickerDialog(
    title: String,
    message: String,
    initialSelection: String,
    confirmLabel: String,
    onConfirm: (String) -> Unit,
    onDismiss: () -> Unit,
) {
    var selected by remember(initialSelection) { mutableStateOf(initialSelection) }

    AlertDialog(
        onDismissRequest = onDismiss,
        icon = { Icon(Icons.Outlined.Language, contentDescription = null) },
        title = { Text(title) },
        text = {
            Column(
                modifier = Modifier
                    .heightIn(max = 420.dp)
                    .verticalScroll(rememberScrollState()),
            ) {
                Text(
                    text = message,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(Modifier.height(12.dp))
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    LanguageCatalog.all.forEach { language ->
                        FilterChip(
                            selected = selected == language.code,
                            onClick = { selected = language.code },
                            label = { Text("${language.flag} ${language.label}") },
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = { onConfirm(selected) }) { Text(confirmLabel) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Vazgeç") }
        },
    )
}

@Composable
private fun InfoLine(number: String, text: String) {
    Row(verticalAlignment = Alignment.Top) {
        Surface(
            shape = CircleShape,
            color = MaterialTheme.colorScheme.primaryContainer,
            modifier = Modifier.size(24.dp),
        ) {
            Box(contentAlignment = Alignment.Center) {
                Text(
                    text = number,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                    fontFamily = FontFamily.Serif,
                    fontWeight = FontWeight.Bold,
                )
            }
        }
        Spacer(Modifier.width(10.dp))
        Text(
            text = text,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}
