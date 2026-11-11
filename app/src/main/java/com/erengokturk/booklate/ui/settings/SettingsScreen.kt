package com.erengokturk.booklate.ui.settings

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material.icons.outlined.ErrorOutline
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Key
import androidx.compose.material.icons.outlined.Language
import androidx.compose.material.icons.automirrored.outlined.OpenInNew
import androidx.compose.material.icons.outlined.Palette
import androidx.compose.material.icons.outlined.PlayArrow
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material.icons.outlined.RecordVoiceOver
import androidx.compose.material.icons.outlined.Stop
import androidx.compose.material.icons.outlined.Storage
import androidx.compose.material.icons.outlined.Visibility
import androidx.compose.material.icons.outlined.VisibilityOff
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.erengokturk.booklate.BuildConfig
import com.erengokturk.booklate.core.LanguageCatalog
import com.erengokturk.booklate.core.Models
import com.erengokturk.booklate.di.LocalAppContainer
import com.erengokturk.booklate.ui.components.InlineError
import com.erengokturk.booklate.ui.components.SectionCard
import com.erengokturk.booklate.ui.components.TagChip
import java.util.Locale

@Composable
fun SettingsRoute() {
    val container = LocalAppContainer.current
    val viewModel: SettingsViewModel = viewModel(factory = SettingsViewModel.factory(container))
    val state by viewModel.state.collectAsStateWithLifecycle()

    SettingsScreen(
        state = state,
        onApiKeyChange = viewModel::onApiKeyChange,
        onToggleApiKeyVisibility = viewModel::toggleApiKeyVisibility,
        onSaveApiKey = viewModel::saveApiKey,
        onClearApiKey = viewModel::clearApiKey,
        onTestApiKey = viewModel::testApiKey,
        onModelChange = viewModel::setModel,
        onNativeLanguageChange = viewModel::setNativeLanguage,
        onTtsRateChange = viewModel::setTtsRate,
        onTtsPitchChange = viewModel::setTtsPitch,
        onTtsVoiceChange = viewModel::setTtsVoice,
        onTestVoice = viewModel::testVoice,
        onStopVoiceTest = viewModel::stopVoiceTest,
        onKeepScreenOnChange = viewModel::setKeepScreenOn,
        onShowPageHintsChange = viewModel::setShowPageHints,
        onRenderQualityChange = viewModel::setRenderQuality,
        onThemeModeChange = viewModel::setThemeMode,
        onRefreshCacheStats = viewModel::refreshCacheStats,
        onRequestClearCache = viewModel::requestClearCache,
        onDismissClearCache = viewModel::dismissClearCache,
        onConfirmClearCache = viewModel::confirmClearCache,
        onMessageShown = viewModel::consumeMessage,
    )
}

@Composable
private fun SettingsScreen(
    state: SettingsUiState,
    onApiKeyChange: (String) -> Unit,
    onToggleApiKeyVisibility: () -> Unit,
    onSaveApiKey: () -> Unit,
    onClearApiKey: () -> Unit,
    onTestApiKey: () -> Unit,
    onModelChange: (String) -> Unit,
    onNativeLanguageChange: (String) -> Unit,
    onTtsRateChange: (Float) -> Unit,
    onTtsPitchChange: (Float) -> Unit,
    onTtsVoiceChange: (String?) -> Unit,
    onTestVoice: () -> Unit,
    onStopVoiceTest: () -> Unit,
    onKeepScreenOnChange: (Boolean) -> Unit,
    onShowPageHintsChange: (Boolean) -> Unit,
    onRenderQualityChange: (Float) -> Unit,
    onThemeModeChange: (String) -> Unit,
    onRefreshCacheStats: () -> Unit,
    onRequestClearCache: () -> Unit,
    onDismissClearCache: () -> Unit,
    onConfirmClearCache: () -> Unit,
    onMessageShown: () -> Unit,
) {
    val context = LocalContext.current
    val snackbar = remember { SnackbarHostState() }
    var showVoiceDialog by remember { mutableStateOf(false) }

    LaunchedEffect(state.message) {
        state.message?.let {
            snackbar.showSnackbar(it)
            onMessageShown()
        }
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        contentWindowInsets = androidx.compose.foundation.layout.WindowInsets(0, 0, 0, 0),
        snackbarHost = { SnackbarHost(snackbar) },
    ) { padding ->
        androidx.compose.foundation.lazy.LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .statusBarsPadding(),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(
                start = 20.dp,
                end = 20.dp,
                top = 12.dp,
                bottom = 32.dp,
            ),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            item {
                Column {
                    Text(
                        text = "Ayarlar",
                        style = MaterialTheme.typography.headlineMedium,
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = "Dili, sesi ve görünümü kendine göre ayarla.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }

            if (state.settings.geminiApiKey.isBlank()) {
                item { ApiKeyMissingBanner(onOpenStudio = { openAiStudio(context) }) }
            }

            item {
                SectionCard {
                    SectionTitle(
                        icon = Icons.Outlined.Key,
                        title = "Gemini yapay zekâ",
                        subtitle = "Bağlamsal çeviri, dil bilgisi ve ek çözümlemesi için.",
                    )
                    Spacer(Modifier.height(14.dp))

                    OutlinedTextField(
                        value = state.apiKeyDraft,
                        onValueChange = onApiKeyChange,
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text("Gemini API anahtarı") },
                        placeholder = { Text("AIza…") },
                        singleLine = true,
                        visualTransformation = if (state.showApiKey) {
                            VisualTransformation.None
                        } else {
                            PasswordVisualTransformation()
                        },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                        trailingIcon = {
                            IconButton(onClick = onToggleApiKeyVisibility) {
                                Icon(
                                    imageVector = if (state.showApiKey) {
                                        Icons.Outlined.VisibilityOff
                                    } else {
                                        Icons.Outlined.Visibility
                                    },
                                    contentDescription = if (state.showApiKey) "Anahtarı gizle" else "Anahtarı göster",
                                )
                            }
                        },
                    )
                    Spacer(Modifier.height(8.dp))
                    Text(
                        text = "Anahtar yalnızca bu cihazda saklanır ve doğrudan Google'a gönderilir. Booklate sunucusu yoktur.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Spacer(Modifier.height(12.dp))

                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Button(
                            onClick = onSaveApiKey,
                            enabled = state.apiKeyDraft.trim() != state.settings.geminiApiKey.trim(),
                        ) {
                            Text("Kaydet")
                        }
                        OutlinedButton(
                            onClick = onTestApiKey,
                            enabled = state.apiTest !is ApiTestState.Testing &&
                                state.apiKeyDraft.isNotBlank(),
                        ) {
                            if (state.apiTest is ApiTestState.Testing) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(16.dp),
                                    strokeWidth = 2.dp,
                                )
                                Spacer(Modifier.width(8.dp))
                            }
                            Text("Bağlantıyı test et")
                        }
                        if (state.settings.geminiApiKey.isNotBlank()) {
                            TextButton(onClick = onClearApiKey) { Text("Kaldır") }
                        }
                    }

                    when (val test = state.apiTest) {
                        ApiTestState.Idle -> Unit
                        ApiTestState.Testing -> {
                            Spacer(Modifier.height(10.dp))
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(16.dp),
                                    strokeWidth = 2.dp,
                                )
                                Spacer(Modifier.width(8.dp))
                                Text(
                                    text = "Gemini'ye bağlanılıyor…",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                        }
                        is ApiTestState.Success -> {
                            Spacer(Modifier.height(10.dp))
                            StatusBanner(
                                message = test.message,
                                success = true,
                            )
                        }
                        is ApiTestState.Failure -> {
                            Spacer(Modifier.height(10.dp))
                            InlineError(message = test.message)
                        }
                    }

                    Spacer(Modifier.height(12.dp))
                    TextButton(
                        onClick = { openAiStudio(context) },
                        contentPadding = androidx.compose.foundation.layout.PaddingValues(0.dp),
                    ) {
                        Icon(
                            Icons.AutoMirrored.Outlined.OpenInNew,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp),
                        )
                        Spacer(Modifier.width(6.dp))
                        Text("Ücretsiz anahtar al · Google AI Studio")
                    }

                    Spacer(Modifier.height(18.dp))
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                    Spacer(Modifier.height(16.dp))

                    Text(
                        text = "Model",
                        style = MaterialTheme.typography.titleSmall,
                    )
                    Spacer(Modifier.height(2.dp))
                    Text(
                        text = "Ücretsiz kota modeller arasında paylaşılır; günlük limitler Google tarafında değişebilir.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Spacer(Modifier.height(8.dp))

                    Models.geminiOptions.forEach { option ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onModelChange(option.id) }
                                .padding(vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            RadioButton(
                                selected = state.settings.geminiModel == option.id,
                                onClick = { onModelChange(option.id) },
                            )
                            Spacer(Modifier.width(4.dp))
                            Column {
                                Text(
                                    text = option.label,
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Medium,
                                )
                                Text(
                                    text = option.note,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                        }
                    }

                    if (Models.geminiOptions.none { it.id == state.settings.geminiModel }) {
                        Spacer(Modifier.height(6.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "Özel model:",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                            Spacer(Modifier.width(8.dp))
                            TagChip(text = state.settings.geminiModel)
                        }
                    }
                }
            }

            item {
                SectionCard {
                    SectionTitle(
                        icon = Icons.Outlined.Language,
                        title = "Ana dilim",
                        subtitle = "Açıklamalar, dil bilgisi notları ve çeviriler bu dilde yazılır.",
                    )
                    Spacer(Modifier.height(14.dp))

                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        LanguageCatalog.natives.forEach { language ->
                            FilterChip(
                                selected = state.settings.nativeLanguage == language.code,
                                onClick = { onNativeLanguageChange(language.code) },
                                label = { Text("${language.flag} ${language.label}") },
                            )
                        }
                    }

                    Spacer(Modifier.height(14.dp))
                    Text(
                        text = "Şu an: ${LanguageCatalog.native(state.settings.nativeLanguage).nativeLabel}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )

                    Spacer(Modifier.height(10.dp))
                    Surface(
                        shape = MaterialTheme.shapes.small,
                        color = MaterialTheme.colorScheme.surfaceVariant,
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Text(
                            text = "Kitap dili ayrı seçilir: PDF eklerken sorulur, sonradan kütüphanede kitabın ⋮ menüsünden “Dili değiştir” ile güncellenebilir.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(12.dp),
                        )
                    }
                }
            }

            item {
                SectionCard {
                    SectionTitle(
                        icon = Icons.Outlined.RecordVoiceOver,
                        title = "Sesli okuma",
                        subtitle = "Cihazının yerleşik konuşma motorunu kullanır; ek ücret yoktur.",
                    )
                    Spacer(Modifier.height(16.dp))

                    var rate by remember(state.settings.ttsRate) {
                        mutableFloatStateOf(state.settings.ttsRate)
                    }
                    SettingSlider(
                        title = "Konuşma hızı",
                        valueLabel = String.format(Locale.US, "%.2f×", rate),
                        value = rate,
                        range = 0.5f..1.6f,
                        steps = 21,
                        onValueChange = { rate = it },
                        onValueChangeFinished = { onTtsRateChange(rate) },
                    )

                    Spacer(Modifier.height(6.dp))
                    var pitch by remember(state.settings.ttsPitch) {
                        mutableFloatStateOf(state.settings.ttsPitch)
                    }
                    SettingSlider(
                        title = "Ses perdesi",
                        valueLabel = String.format(Locale.US, "%.2f", pitch),
                        value = pitch,
                        range = 0.6f..1.4f,
                        steps = 15,
                        onValueChange = { pitch = it },
                        onValueChangeFinished = { onTtsPitchChange(pitch) },
                    )

                    Spacer(Modifier.height(6.dp))
                    val selectedVoiceLabel = remember(state.settings.ttsVoice, state.voices) {
                        state.voices.firstOrNull { it.name == state.settings.ttsVoice }?.label
                            ?: "Otomatik (önerilen)"
                    }
                    Text(
                        text = "Ses",
                        style = MaterialTheme.typography.titleSmall,
                    )
                    Spacer(Modifier.height(6.dp))
                    OutlinedButton(
                        onClick = { showVoiceDialog = true },
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Text(
                            text = selectedVoiceLabel,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f),
                        )
                        Text("Değiştir")
                    }
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = if (state.ttsReady) {
                            "Son açtığın kitabın dili için ${state.voices.size} ses bulundu."
                        } else {
                            "Konuşma motoru hazırlanıyor… Ses listesi birazdan dolar."
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )

                    Spacer(Modifier.height(10.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(
                            onClick = if (state.isTestingVoice) onStopVoiceTest else onTestVoice,
                        ) {
                            Icon(
                                imageVector = if (state.isTestingVoice) {
                                    Icons.Outlined.Stop
                                } else {
                                    Icons.Outlined.PlayArrow
                                },
                                contentDescription = null,
                                modifier = Modifier.size(18.dp),
                            )
                            Spacer(Modifier.width(6.dp))
                            Text(if (state.isTestingVoice) "Durdur" else "Sesi dene")
                        }
                        if (state.settings.ttsVoice != null) {
                            TextButton(onClick = { onTtsVoiceChange(null) }) {
                                Text("Otomatiğe dön")
                            }
                        }
                    }

                    state.ttsError?.let { error ->
                        Spacer(Modifier.height(10.dp))
                        InlineError(message = error)
                    }

                    Spacer(Modifier.height(14.dp))
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                    Spacer(Modifier.height(6.dp))

                    SwitchRow(
                        title = "Okurken ekranı açık tut",
                        description = "Uzun okuma oturumlarında ekranın kapanmasını engeller.",
                        checked = state.settings.keepScreenOn,
                        onCheckedChange = onKeepScreenOnChange,
                    )
                    SwitchRow(
                        title = "Dokunma ipuçlarını göster",
                        description = "Okuma ekranında kısa kullanım ipuçları görünür.",
                        checked = state.settings.showPageHints,
                        onCheckedChange = onShowPageHintsChange,
                    )
                }
            }

            item {
                SectionCard {
                    SectionTitle(
                        icon = Icons.Outlined.Palette,
                        title = "Görünüm ve sayfa kalitesi",
                        subtitle = "Sayfa görüntüsü netliği ile bellek kullanımı arasında denge kur.",
                    )
                    Spacer(Modifier.height(12.dp))

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        ThemeChip(
                            label = "Sistem",
                            selected = state.settings.themeMode == "system",
                            onClick = { onThemeModeChange("system") },
                        )
                        ThemeChip(
                            label = "Açık",
                            selected = state.settings.themeMode == "light",
                            onClick = { onThemeModeChange("light") },
                        )
                        ThemeChip(
                            label = "Koyu",
                            selected = state.settings.themeMode == "dark",
                            onClick = { onThemeModeChange("dark") },
                        )
                    }

                    Spacer(Modifier.height(18.dp))
                    var quality by remember(state.settings.renderQuality) {
                        mutableFloatStateOf(state.settings.renderQuality)
                    }
                    SettingSlider(
                        title = "Sayfa çözünürlüğü",
                        valueLabel = String.format(Locale.US, "%.1f×", quality),
                        value = quality,
                        range = 1.0f..2.5f,
                        steps = 14,
                        onValueChange = { quality = it },
                        onValueChangeFinished = { onRenderQualityChange(quality) },
                    )
                    Text(
                        text = "Yüksek değer yazıyı daha net gösterir; daha fazla bellek ve işlem gücü kullanır.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }

            item {
                SectionCard {
                    SectionTitle(
                        icon = Icons.Outlined.Storage,
                        title = "Kelime önbelleği",
                        subtitle = "Aynı kelime tekrar sorgulandığında API kotası harcanmaz.",
                    )
                    Spacer(Modifier.height(12.dp))

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "${state.cacheStats.wordCount} kelime · ${state.cacheStats.favoriteCount} favori",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Medium,
                            )
                            Text(
                                text = "Kelimeler, cümle bağlamı ve analiz sonuçları cihazda saklanır.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                        IconButton(onClick = onRefreshCacheStats) {
                            Icon(
                                Icons.Outlined.Refresh,
                                contentDescription = "İstatistikleri yenile",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }

                    Spacer(Modifier.height(10.dp))
                    OutlinedButton(
                        onClick = onRequestClearCache,
                        modifier = Modifier.fillMaxWidth(),
                        enabled = state.cacheStats.wordCount > 0,
                    ) {
                        Icon(
                            Icons.Outlined.DeleteOutline,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.error,
                            modifier = Modifier.size(18.dp),
                        )
                        Spacer(Modifier.width(8.dp))
                        Text("Önbelleği ve favorileri temizle", color = MaterialTheme.colorScheme.error)
                    }
                    Spacer(Modifier.height(6.dp))
                    Text(
                        text = "Kitapların ve okuma ilerlemen silinmez.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }

            item {
                SectionCard {
                    SectionTitle(
                        icon = Icons.Outlined.Info,
                        title = "Hakkında ve gizlilik",
                        subtitle = "Booklate ${BuildConfig.VERSION_NAME}",
                    )
                    Spacer(Modifier.height(12.dp))
                    Text(
                        text = "PDF dosyaların, okuma konumun ve kelime kayıtların yalnızca bu cihazda tutulur. " +
                            "Booklate'in sunucusu, reklamı veya analitiği yoktur.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Spacer(Modifier.height(8.dp))
                    Text(
                        text = "Bir kelimeye dokunduğunda yalnızca kelime ve içinde geçtiği cümle, seçtiğin " +
                            "Gemini modeline gönderilir. PDF dosyasının tamamı yüklenmez.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Spacer(Modifier.height(8.dp))
                    Text(
                        text = "Taranmış (görüntü tabanlı) PDF'lerde metin katmanı bulunmaz; bu kitaplarda kelime " +
                            "seçimi ve sesli okuma çalışmaz. OCR desteği yol haritasındadır.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Spacer(Modifier.height(12.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        TagChip(text = "PDFBox · Apache 2.0", containerColor = MaterialTheme.colorScheme.secondaryContainer)
                        TagChip(text = "Gemini API", containerColor = MaterialTheme.colorScheme.surfaceVariant)
                    }
                }
            }
        }
    }

    if (showVoiceDialog) {
        VoicePickerDialog(
            voices = state.voices,
            selected = state.settings.ttsVoice,
            onSelect = {
                onTtsVoiceChange(it)
                showVoiceDialog = false
            },
            onDismiss = { showVoiceDialog = false },
        )
    }

    if (state.showClearCacheConfirm) {
        AlertDialog(
            onDismissRequest = onDismissClearCache,
            icon = { Icon(Icons.Outlined.DeleteOutline, contentDescription = null) },
            title = { Text("Önbellek temizlensin mi?") },
            text = {
                Text(
                    "Tüm kelime analizleri ve favoriler silinecek. Aynı kelimeler tekrar sorgulandığında " +
                        "Gemini kotası kullanılır. Kitapların ve okuma ilerlemen korunur.",
                )
            },
            confirmButton = {
                TextButton(onClick = onConfirmClearCache) {
                    Text("Temizle", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = onDismissClearCache) { Text("Vazgeç") }
            },
        )
    }
}

@Composable
private fun ApiKeyMissingBanner(onOpenStudio: () -> Unit) {
    Surface(
        shape = MaterialTheme.shapes.large,
        color = MaterialTheme.colorScheme.tertiaryContainer,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                Icons.Outlined.Key,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onTertiaryContainer,
            )
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Kelime analizi için anahtar gerekli",
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onTertiaryContainer,
                )
                Text(
                    text = "Google AI Studio'dan ücretsiz bir Gemini anahtarı alabilirsin.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onTertiaryContainer.copy(alpha = 0.85f),
                )
            }
            TextButton(onClick = onOpenStudio) { Text("Al") }
        }
    }
}

@Composable
private fun SectionTitle(
    icon: ImageVector,
    title: String,
    subtitle: String,
) {
    Row(verticalAlignment = Alignment.Top) {
        Surface(
            shape = CircleShape,
            color = MaterialTheme.colorScheme.primaryContainer,
            modifier = Modifier.size(34.dp),
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onPrimaryContainer,
                    modifier = Modifier.size(18.dp),
                )
            }
        }
        Spacer(Modifier.width(12.dp))
        Column {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun SettingSlider(
    title: String,
    valueLabel: String,
    value: Float,
    range: ClosedFloatingPointRange<Float>,
    steps: Int,
    onValueChange: (Float) -> Unit,
    onValueChangeFinished: () -> Unit,
) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleSmall,
            modifier = Modifier.weight(1f),
        )
        TagChip(text = valueLabel)
    }
    Slider(
        value = value,
        onValueChange = onValueChange,
        onValueChangeFinished = onValueChangeFinished,
        valueRange = range,
        steps = steps,
    )
}

@Composable
private fun SwitchRow(
    title: String,
    description: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium,
            )
            Text(
                text = description,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Spacer(Modifier.width(12.dp))
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}

@Composable
private fun ThemeChip(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
) {
    FilterChip(
        selected = selected,
        onClick = onClick,
        label = { Text(label) },
    )
}

@Composable
private fun StatusBanner(message: String, success: Boolean) {
    Surface(
        shape = MaterialTheme.shapes.medium,
        color = if (success) MaterialTheme.colorScheme.secondaryContainer
        else MaterialTheme.colorScheme.errorContainer,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                imageVector = if (success) Icons.Outlined.CheckCircle else Icons.Outlined.ErrorOutline,
                contentDescription = null,
                tint = if (success) {
                    MaterialTheme.colorScheme.onSecondaryContainer
                } else {
                    MaterialTheme.colorScheme.onErrorContainer
                },
                modifier = Modifier.size(18.dp),
            )
            Spacer(Modifier.width(8.dp))
            Text(
                text = message,
                style = MaterialTheme.typography.bodySmall,
                color = if (success) {
                    MaterialTheme.colorScheme.onSecondaryContainer
                } else {
                    MaterialTheme.colorScheme.onErrorContainer
                },
            )
        }
    }
}

@Composable
private fun VoicePickerDialog(
    voices: List<com.erengokturk.booklate.tts.TtsVoiceOption>,
    selected: String?,
    onSelect: (String?) -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Ses seç") },
        text = {
            Column(
                modifier = Modifier
                    .heightIn(max = 380.dp)
                    .verticalScroll(rememberScrollState()),
            ) {
                VoiceRow(
                    label = "Otomatik (önerilen)",
                    description = "Dil için cihazın en uygun sesini kullanır.",
                    selected = selected == null,
                    onClick = { onSelect(null) },
                )
                if (voices.isEmpty()) {
                    Spacer(Modifier.height(8.dp))
                    Text(
                        text = "Bu dil için henüz ses listesi yok. Android Ayarlar → Diller → " +
                            "Metin okuma çıktısı bölümünden ses paketini yükleyebilirsin.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                voices.forEach { voice ->
                    VoiceRow(
                        label = voice.label,
                        description = voice.name,
                        selected = selected == voice.name,
                        onClick = { onSelect(voice.name) },
                    )
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("Kapat") }
        },
    )
}

@Composable
private fun VoiceRow(
    label: String,
    description: String,
    selected: Boolean,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        RadioButton(selected = selected, onClick = onClick)
        Spacer(Modifier.width(4.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = label,
                style = MaterialTheme.typography.bodyMedium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = description,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

private fun openAiStudio(context: android.content.Context) {
    runCatching {
        context.startActivity(
            Intent(Intent.ACTION_VIEW, Uri.parse("https://aistudio.google.com/app/apikey")),
        )
    }
}
