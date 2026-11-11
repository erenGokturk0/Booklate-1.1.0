package com.erengokturk.booklate.ui.reader

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.view.WindowManager
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.ChevronLeft
import androidx.compose.material.icons.outlined.ChevronRight
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.MoreVert
import androidx.compose.material.icons.outlined.Pause
import androidx.compose.material.icons.outlined.PlayArrow
import androidx.compose.material.icons.outlined.RecordVoiceOver
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.Stop
import androidx.compose.material.icons.outlined.Translate
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Slider
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.erengokturk.booklate.core.Formatters
import com.erengokturk.booklate.di.LocalAppContainer
import com.erengokturk.booklate.domain.model.Book
import com.erengokturk.booklate.domain.model.ReaderSelection
import com.erengokturk.booklate.ui.analysis.WordAnalysisSheetContent
import com.erengokturk.booklate.ui.components.InlineError
import com.erengokturk.booklate.ui.components.LoadingPane
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

@Composable
fun ReaderRoute(
    bookId: String,
    onBack: () -> Unit,
    onOpenSettings: () -> Unit,
) {
    val container = LocalAppContainer.current
    val viewModel: ReaderViewModel = viewModel(
        key = "reader-$bookId",
        factory = ReaderViewModel.factory(bookId, container),
    )
    val state by viewModel.state.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val snackbar = remember { SnackbarHostState() }

    LaunchedEffect(state.transientError) {
        state.transientError?.let {
            snackbar.showSnackbar(it)
            viewModel.consumeError()
        }
    }
    LaunchedEffect(state.tts.error) {
        state.tts.error?.let {
            snackbar.showSnackbar(it)
            viewModel.clearTtsError()
        }
    }

    val activity = remember(context) { context.findActivity() }
    DisposableEffect(state.settings.keepScreenOn, activity) {
        if (state.settings.keepScreenOn) {
            activity?.window?.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        } else {
            activity?.window?.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        }
        onDispose {
            activity?.window?.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        }
    }

    DisposableEffect(Unit) {
        onDispose { viewModel.stopTts() }
    }

    when {
        state.loadingBook -> LoadingPane("Kitap açılıyor…")
        state.bookError != null -> {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Column(
                    modifier = Modifier.padding(32.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    InlineError(message = state.bookError ?: "Kitap açılamadı.")
                    Spacer(Modifier.height(16.dp))
                    TextButton(onClick = onBack) { Text("Kütüphaneye dön") }
                }
            }
        }
        state.book != null -> {
            ReaderContent(
                state = state,
                viewModel = viewModel,
                snackbar = snackbar,
                onBack = onBack,
                onOpenSettings = onOpenSettings,
            )
        }
    }
}

@Composable
private fun ReaderContent(
    state: ReaderUiState,
    viewModel: ReaderViewModel,
    snackbar: SnackbarHostState,
    onBack: () -> Unit,
    onOpenSettings: () -> Unit,
) {
    val book = state.book ?: return
    var chromeVisible by remember { mutableStateOf(true) }
    var menuOpen by remember { mutableStateOf(false) }
    var showInfo by remember { mutableStateOf(false) }
    val pagerState = rememberPagerState(
        initialPage = book.safeLastPage,
        initialPageOffsetFraction = book.lastPageOffset.coerceIn(0f, 0.99f),
        pageCount = { state.pageCount.coerceAtLeast(1) },
    )
    val scope = rememberCoroutineScope()
    val selection = state.selection
    val showSelectionBar = selection != null && !selection.isSingleWord &&
        state.analysis is AnalysisUiState.Idle
    val showSheet = selection != null && state.analysis !is AnalysisUiState.Idle

    BackHandler(enabled = selection != null) {
        viewModel.clearSelection()
    }

    LaunchedEffect(pagerState) {
        snapshotFlow { pagerState.settledPage }
            .distinctUntilChanged()
            .collect { page -> viewModel.onPageChanged(page, 0f) }
    }

    BoxWithConstraints(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.surfaceVariant),
    ) {
        val density = LocalDensity.current
        val screenWidthPx = with(density) { maxWidth.roundToPx() }
        val renderWidth = screenWidthPx.coerceAtLeast(720)
        val statusTop = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()
        val topPadding = if (chromeVisible) statusTop + 58.dp else statusTop + 8.dp
        val bottomPadding = if (chromeVisible) 196.dp else 12.dp

        HorizontalPager(
            state = pagerState,
            modifier = Modifier.fillMaxSize(),
            pageSpacing = 12.dp,
            beyondViewportPageCount = 1,
        ) { page ->
            val pageState = state.pages[page] ?: PageUiState(isLoading = true)
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(top = topPadding, bottom = bottomPadding, start = 6.dp, end = 6.dp),
                contentAlignment = Alignment.Center,
            ) {
                LaunchedEffect(page, renderWidth) {
                    viewModel.loadPage(page, renderWidth)
                }

                if (pageState.bitmap != null) {
                    AndroidView(
                        factory = { context ->
                            PdfPageView(context).apply {
                                listener = readerListener(
                                    viewModel = viewModel,
                                    onEmptyTap = {
                                        if (state.selection != null) viewModel.clearSelection()
                                        else chromeVisible = !chromeVisible
                                    },
                                )
                            }
                        },
                        update = { view ->
                            view.listener = readerListener(
                                viewModel = viewModel,
                                onEmptyTap = {
                                    if (state.selection != null) viewModel.clearSelection()
                                    else chromeVisible = !chromeVisible
                                },
                            )
                            view.setPage(
                                newBitmap = pageState.bitmap,
                                newWords = pageState.text.words,
                                key = "${book.id}:$page",
                            )
                            view.setHighlights(
                                selected = if (page == state.currentPage) {
                                    selection?.wordIndices
                                } else {
                                    null
                                },
                                active = if (page == state.currentPage) {
                                    state.tts.activeWordIndex
                                } else {
                                    null
                                },
                            )
                        },
                        modifier = Modifier.fillMaxSize(),
                    )
                } else if (pageState.error != null) {
                    Column(
                        modifier = Modifier.padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        InlineError(
                            message = pageState.error,
                            onRetry = { viewModel.retryPage(page, renderWidth) },
                        )
                    }
                } else {
                    CircularProgressIndicator(strokeWidth = 2.dp, modifier = Modifier.size(28.dp))
                }

                if (pageState.isTextLoading && pageState.bitmap != null) {
                    Surface(
                        modifier = Modifier
                            .align(Alignment.TopCenter)
                            .padding(top = 8.dp),
                        shape = CircleShape,
                        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.88f),
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            CircularProgressIndicator(
                                strokeWidth = 1.5.dp,
                                modifier = Modifier.size(12.dp),
                            )
                            Spacer(Modifier.width(8.dp))
                            Text(
                                text = "Metin katmanı hazırlanıyor…",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                } else if (!pageState.isTextLoading &&
                    pageState.bitmap != null &&
                    pageState.text.words.isEmpty() &&
                    page == state.currentPage
                ) {
                    Surface(
                        modifier = Modifier
                            .align(Alignment.TopCenter)
                            .padding(top = 8.dp),
                        shape = CircleShape,
                        color = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.92f),
                    ) {
                        Text(
                            text = "Metin katmanı yok · taranmış PDF olabilir",
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onTertiaryContainer,
                        )
                    }
                }
            }
        }

        AnimatedVisibility(
            visible = chromeVisible,
            enter = fadeIn() + slideInVertically { -it / 2 },
            exit = fadeOut() + slideOutVertically { -it / 2 },
            modifier = Modifier.align(Alignment.TopCenter),
        ) {
            ReaderTopBar(
                book = book,
                currentPage = state.currentPage,
                pageCount = state.pageCount,
                menuOpen = menuOpen,
                onMenuOpenChange = { menuOpen = it },
                onBack = onBack,
                onOpenSettings = onOpenSettings,
                onInfo = { showInfo = true },
            )
        }

        AnimatedVisibility(
            visible = chromeVisible,
            enter = fadeIn() + slideInVertically { it / 2 },
            exit = fadeOut() + slideOutVertically { it / 2 },
            modifier = Modifier.align(Alignment.BottomCenter),
        ) {
            ReaderControls(
                currentPage = state.currentPage,
                pageCount = state.pageCount,
                tts = state.tts,
                speed = state.settings.ttsRate,
                onSeek = { page -> scope.launch { pagerState.scrollToPage(page) } },
                onPrevious = {
                    scope.launch { pagerState.animateScrollToPage((state.currentPage - 1).coerceAtLeast(0)) }
                },
                onNext = {
                    scope.launch {
                        pagerState.animateScrollToPage(
                            (state.currentPage + 1).coerceAtMost(state.pageCount - 1),
                        )
                    }
                },
                onToggleTts = viewModel::toggleTts,
                onStopTts = viewModel::stopTts,
                onOpenSettings = onOpenSettings,
                onSpeakPage = viewModel::speakPage,
            )
        }

        if (!chromeVisible && selection == null) {
            Surface(
                onClick = { chromeVisible = true },
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .navigationBarsPadding()
                    .padding(bottom = 12.dp),
                shape = CircleShape,
                color = MaterialTheme.colorScheme.surface.copy(alpha = 0.82f),
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(
                        Icons.Outlined.RecordVoiceOver,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp),
                    )
                    Spacer(Modifier.width(6.dp))
                    Text("Kontroller", style = MaterialTheme.typography.labelSmall)
                }
            }
        }

        AnimatedVisibility(
            visible = showSelectionBar,
            enter = fadeIn() + slideInVertically { it / 2 },
            exit = fadeOut() + slideOutVertically { it / 2 },
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .navigationBarsPadding()
                .padding(bottom = if (chromeVisible) 204.dp else 16.dp, start = 16.dp, end = 16.dp),
        ) {
            selection?.let { selected ->
                SelectionBar(
                    selection = selected,
                    onTranslate = viewModel::translateSelection,
                    onSpeak = viewModel::speakSelection,
                    onClose = viewModel::clearSelection,
                )
            }
        }

        if (showSheet) {
            selection?.let { sheetSelection ->
                ModalBottomSheet(
                    onDismissRequest = viewModel::clearSelection,
                    containerColor = MaterialTheme.colorScheme.surface,
                    dragHandle = null,
                ) {
                    WordAnalysisSheetContent(
                        selection = sheetSelection,
                        analysis = state.analysis,
                        onDismiss = viewModel::clearSelection,
                        onRetry = viewModel::retryAnalysis,
                        onRefresh = viewModel::refreshAnalysis,
                        onOpenSettings = {
                            viewModel.clearSelection()
                            onOpenSettings()
                        },
                        onSpeakWord = viewModel::speakSelection,
                        onSpeakSentence = viewModel::speakSelectedSentence,
                        onToggleFavorite = viewModel::toggleFavorite,
                    )
                }
            }
        }

        SnackbarHost(
            hostState = snackbar,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = if (chromeVisible) 210.dp else 64.dp, start = 16.dp, end = 16.dp),
        )

        if (showInfo) {
            BookInfoDialog(book = book, onDismiss = { showInfo = false })
        }
    }
}

private fun readerListener(
    viewModel: ReaderViewModel,
    onEmptyTap: () -> Unit,
): PdfPageView.Listener =
    object : PdfPageView.Listener {
        override fun onWordTapped(wordIndex: Int) {
            viewModel.onWordTapped(wordIndex)
        }

        override fun onSelectionStarted(wordIndex: Int) {
            viewModel.onSelectionStarted(wordIndex)
        }

        override fun onSelectionExtended(startWord: Int, endWord: Int) {
            viewModel.onSelectionExtended(startWord, endWord)
        }

        override fun onSelectionFinished(startWord: Int, endWord: Int) {
            viewModel.onSelectionFinished(startWord, endWord)
        }

        override fun onEmptyTap() {
            onEmptyTap()
        }
    }

@Composable
private fun ReaderTopBar(
    book: Book,
    currentPage: Int,
    pageCount: Int,
    menuOpen: Boolean,
    onMenuOpenChange: (Boolean) -> Unit,
    onBack: () -> Unit,
    onOpenSettings: () -> Unit,
    onInfo: () -> Unit,
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                Brush.verticalGradient(
                    listOf(
                        MaterialTheme.colorScheme.surface.copy(alpha = 0.97f),
                        MaterialTheme.colorScheme.surface.copy(alpha = 0.0f),
                    ),
                ),
            )
            .statusBarsPadding()
            .padding(horizontal = 4.dp, vertical = 4.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Outlined.ArrowBack, contentDescription = "Geri")
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = book.title,
                    style = MaterialTheme.typography.titleSmall,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = "${currentPage + 1} / ${pageCount.coerceAtLeast(1)}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Box {
                IconButton(onClick = { onMenuOpenChange(true) }) {
                    Icon(Icons.Outlined.MoreVert, contentDescription = "Menü")
                }
                DropdownMenu(expanded = menuOpen, onDismissRequest = { onMenuOpenChange(false) }) {
                    DropdownMenuItem(
                        text = { Text("Seslendirme ve dil ayarları") },
                        leadingIcon = { Icon(Icons.Outlined.Settings, contentDescription = null) },
                        onClick = {
                            onMenuOpenChange(false)
                            onOpenSettings()
                        },
                    )
                    DropdownMenuItem(
                        text = { Text("Kitap bilgisi") },
                        leadingIcon = { Icon(Icons.Outlined.Info, contentDescription = null) },
                        onClick = {
                            onMenuOpenChange(false)
                            onInfo()
                        },
                    )
                }
            }
        }
    }
}

@Composable
private fun ReaderControls(
    currentPage: Int,
    pageCount: Int,
    tts: com.erengokturk.booklate.tts.TtsState,
    speed: Float,
    onSeek: (Int) -> Unit,
    onPrevious: () -> Unit,
    onNext: () -> Unit,
    onToggleTts: () -> Unit,
    onStopTts: () -> Unit,
    onOpenSettings: () -> Unit,
    onSpeakPage: () -> Unit,
) {
    var sliderValue by remember(currentPage, pageCount) {
        mutableFloatStateOf(currentPage.toFloat().coerceIn(0f, (pageCount - 1).coerceAtLeast(0).toFloat()))
    }

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(topStart = 26.dp, topEnd = 26.dp),
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 4.dp,
        shadowElevation = 16.dp,
    ) {
        Column(
            modifier = Modifier
                .navigationBarsPadding()
                .padding(horizontal = 18.dp, vertical = 12.dp),
        ) {
            if (pageCount > 1) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "${currentPage + 1}",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Slider(
                        value = sliderValue,
                        onValueChange = { sliderValue = it },
                        onValueChangeFinished = {
                            onSeek(sliderValue.roundToInt())
                        },
                        valueRange = 0f..(pageCount - 1).toFloat(),
                        modifier = Modifier
                            .weight(1f)
                            .padding(horizontal = 8.dp),
                    )
                    Text(
                        text = "$pageCount",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }

            if (tts.isActive) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Box(
                        modifier = Modifier
                            .size(7.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primary),
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = if (tts.isPaused) "Seslendirme duraklatıldı"
                        else "Seslendiriliyor · cümle ${tts.currentSentenceIndex + 1}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Spacer(Modifier.weight(1f))
                    IconButton(onClick = onStopTts, modifier = Modifier.size(30.dp)) {
                        Icon(
                            Icons.Outlined.Stop,
                            contentDescription = "Seslendirmeyi durdur",
                            modifier = Modifier.size(18.dp),
                        )
                    }
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                IconButton(
                    onClick = onPrevious,
                    enabled = currentPage > 0,
                    modifier = Modifier.size(40.dp),
                ) {
                    Icon(Icons.Outlined.ChevronLeft, contentDescription = "Önceki sayfa")
                }
                FilledTonalButton(
                    onClick = onToggleTts,
                    modifier = Modifier.weight(1f),
                ) {
                    Icon(
                        imageVector = when {
                            tts.isSpeaking -> Icons.Outlined.Pause
                            tts.isPaused -> Icons.Outlined.PlayArrow
                            else -> Icons.Outlined.RecordVoiceOver
                        },
                        contentDescription = null,
                        modifier = Modifier.size(19.dp),
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = when {
                            tts.isSpeaking -> "Duraklat"
                            tts.isPaused -> "Devam et"
                            else -> "Sayfayı oku"
                        },
                        maxLines = 1,
                    )
                }
                IconButton(
                    onClick = onNext,
                    enabled = currentPage < pageCount - 1,
                    modifier = Modifier.size(40.dp),
                ) {
                    Icon(Icons.Outlined.ChevronRight, contentDescription = "Sonraki sayfa")
                }
            }

            Spacer(Modifier.height(4.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                AssistChip(
                    onClick = onOpenSettings,
                    label = { Text("${Formatters.speed(speed)} hız") },
                    leadingIcon = {
                        Icon(
                            Icons.Outlined.Settings,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp),
                        )
                    },
                )
                Spacer(Modifier.weight(1f))
                TextButton(onClick = onSpeakPage) {
                    Icon(
                        Icons.Outlined.Translate,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp),
                    )
                    Spacer(Modifier.width(6.dp))
                    Text("Sayfayı seslendir")
                }
            }
        }
    }
}

@Composable
private fun SelectionBar(
    selection: ReaderSelection,
    onTranslate: () -> Unit,
    onSpeak: () -> Unit,
    onClose: () -> Unit,
) {
    Surface(
        shape = RoundedCornerShape(18.dp),
        color = MaterialTheme.colorScheme.surface,
        shadowElevation = 10.dp,
        tonalElevation = 3.dp,
    ) {
        Row(
            modifier = Modifier.padding(start = 16.dp, end = 6.dp, top = 8.dp, bottom = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = selection.text,
                    style = MaterialTheme.typography.bodyMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = "${selection.text.split(" ").size} kelime seçildi",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            TextButton(onClick = onTranslate) { Text("Çevir") }
            IconButton(onClick = onSpeak, modifier = Modifier.size(38.dp)) {
                Icon(Icons.Outlined.RecordVoiceOver, contentDescription = "Seçimi dinle")
            }
            IconButton(onClick = onClose, modifier = Modifier.size(38.dp)) {
                Icon(Icons.Outlined.Close, contentDescription = "Seçimi kapat")
            }
        }
    }
}

@Composable
private fun BookInfoDialog(book: Book, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(book.title, style = MaterialTheme.typography.titleLarge) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                if (!book.author.isNullOrBlank()) Text("Yazar: ${book.author}")
                val language = com.erengokturk.booklate.core.LanguageCatalog.resolve(book.languageCode)
                Text("Kitap dili: ${language.flag} ${language.label}")
                Text("Sayfa: ${book.pageCount}")
                Text("Dosya: ${book.fileName}")
                Text("Boyut: ${Formatters.bytes(book.fileSize)}")
                Text("Eklenme: ${Formatters.date(book.createdAt)}")
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text("Kapat") } },
    )
}

private tailrec fun Context.findActivity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.findActivity()
    else -> null
}
