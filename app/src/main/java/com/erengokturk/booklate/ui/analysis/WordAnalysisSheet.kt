package com.erengokturk.booklate.ui.analysis

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AccountTree
import androidx.compose.material.icons.outlined.CloudOff
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.History
import androidx.compose.material.icons.outlined.Key
import androidx.compose.material.icons.outlined.Lightbulb
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material.icons.outlined.School
import androidx.compose.material.icons.outlined.Star
import androidx.compose.material.icons.outlined.StarBorder
import androidx.compose.material.icons.automirrored.outlined.VolumeUp
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.erengokturk.booklate.core.Formatters
import com.erengokturk.booklate.domain.model.AnalysisOutcome
import com.erengokturk.booklate.domain.model.ReaderSelection
import com.erengokturk.booklate.ui.components.SectionCard
import com.erengokturk.booklate.ui.components.TagChip
import com.erengokturk.booklate.ui.reader.AnalysisUiState

@Composable
fun WordAnalysisSheetContent(
    selection: ReaderSelection,
    analysis: AnalysisUiState,
    onDismiss: () -> Unit,
    onRetry: () -> Unit,
    onRefresh: () -> Unit,
    onOpenSettings: () -> Unit,
    onSpeakWord: () -> Unit,
    onSpeakSentence: () -> Unit,
    onToggleFavorite: () -> Unit,
) {
    val outcome = (analysis as? AnalysisUiState.Success)?.outcome
    val word = outcome?.analysis?.word?.ifBlank { selection.text } ?: selection.text

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .navigationBarsPadding()
            .padding(bottom = 28.dp),
    ) {
        Box(
            modifier = Modifier
                .padding(top = 10.dp, bottom = 2.dp)
                .width(38.dp)
                .height(4.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.outlineVariant)
                .align(Alignment.CenterHorizontally),
        )

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 24.dp, end = 10.dp, top = 14.dp),
            verticalAlignment = Alignment.Top,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = if (outcome?.fromCache == true) "ÖNBELLEKTEN ANALİZ" else "BAĞLAMSAL ANALİZ",
                    style = MaterialTheme.typography.labelSmall,
                    letterSpacing = 1.2.sp,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold,
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    text = word,
                    style = MaterialTheme.typography.headlineMedium,
                    maxLines = 3,
                    overflow = TextOverflow.Ellipsis,
                )
                val transcription = outcome?.analysis?.transcription.orEmpty()
                if (transcription.isNotBlank()) {
                    Spacer(Modifier.height(2.dp))
                    Text(
                        text = transcription,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            IconButton(onClick = onSpeakWord) {
                Icon(Icons.AutoMirrored.Outlined.VolumeUp, contentDescription = "Telaffuzu dinle")
            }
            if (outcome != null) {
                IconButton(onClick = onToggleFavorite) {
                    Icon(
                        imageVector = if (outcome.isFavorite) Icons.Outlined.Star else Icons.Outlined.StarBorder,
                        contentDescription = if (outcome.isFavorite) "Favoriden çıkar" else "Favorilere ekle",
                        tint = if (outcome.isFavorite) MaterialTheme.colorScheme.tertiary
                        else MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            IconButton(onClick = onDismiss) {
                Icon(Icons.Outlined.Close, contentDescription = "Kapat")
            }
        }

        Spacer(Modifier.height(6.dp))
        Surface(
            modifier = Modifier
                .padding(horizontal = 24.dp)
                .fillMaxWidth(),
            shape = RoundedCornerShape(14.dp),
            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f),
        ) {
            Text(
                text = highlightedContext(selection.sentence, selection.text),
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.padding(14.dp),
            )
        }

        Spacer(Modifier.height(18.dp))

        when (analysis) {
            AnalysisUiState.Idle -> Unit
            is AnalysisUiState.Loading -> {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp, vertical = 26.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    CircularProgressIndicator(strokeWidth = 2.dp, modifier = Modifier.size(22.dp))
                    Spacer(Modifier.width(12.dp))
                    Column {
                        Text(
                            text = "Gemini bağlamı analiz ediyor…",
                            style = MaterialTheme.typography.bodyMedium,
                        )
                        Text(
                            text = "Çeviri, dil bilgisi ve ekler hazırlanıyor.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
            is AnalysisUiState.Error -> {
                SectionCard(modifier = Modifier.padding(horizontal = 24.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = if (analysis.missingApiKey) Icons.Outlined.Key else Icons.Outlined.CloudOff,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.error,
                        )
                        Spacer(Modifier.width(10.dp))
                        Text(
                            text = if (analysis.missingApiKey) "API anahtarı gerekli" else "Analiz alınamadı",
                            style = MaterialTheme.typography.titleSmall,
                        )
                    }
                    Spacer(Modifier.height(8.dp))
                    Text(
                        text = analysis.message,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Spacer(Modifier.height(14.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        if (analysis.missingApiKey) {
                            Button(onClick = onOpenSettings) { Text("Ayarlara git") }
                        } else {
                            Button(onClick = onRetry) { Text("Tekrar dene") }
                        }
                        TextButton(onClick = onDismiss) { Text("Kapat") }
                    }
                }
            }
            is AnalysisUiState.Success -> {
                WordAnalysisDetails(
                    outcome = analysis.outcome,
                    currentContext = selection.sentence,
                    highlightText = selection.text,
                    contextMatches = analysis.outcome.contextMatches,
                    onSpeakSentence = onSpeakSentence,
                    onRefresh = onRefresh,
                    onToggleFavorite = onToggleFavorite,
                    showActions = true,
                )
            }
        }
    }
}

@Composable
fun WordAnalysisDetails(
    outcome: AnalysisOutcome,
    currentContext: String,
    highlightText: String,
    contextMatches: Boolean,
    modifier: Modifier = Modifier,
    onSpeakSentence: (() -> Unit)? = null,
    onRefresh: (() -> Unit)? = null,
    onToggleFavorite: (() -> Unit)? = null,
    showActions: Boolean = false,
    showContext: Boolean = false,
) {
    val analysis = outcome.analysis

    Column(modifier = modifier) {
        if (showContext && currentContext.isNotBlank()) {
            Surface(
                modifier = Modifier
                    .padding(horizontal = 24.dp)
                    .fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f),
            ) {
                Text(
                    text = highlightedContext(currentContext, highlightText),
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(14.dp),
                )
            }
            Spacer(Modifier.height(12.dp))
        }
        Row(
            modifier = Modifier.padding(horizontal = 24.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            TagChip(
                text = if (outcome.fromCache) "Yerel önbellek" else "Yeni · Gemini",
                containerColor = if (outcome.fromCache) MaterialTheme.colorScheme.secondaryContainer
                else MaterialTheme.colorScheme.primaryContainer,
                contentColor = if (outcome.fromCache) MaterialTheme.colorScheme.onSecondaryContainer
                else MaterialTheme.colorScheme.onPrimaryContainer,
            )
            if (analysis.cefrLevel.isNotBlank() && analysis.cefrLevel != "-") {
                TagChip(
                    text = analysis.cefrLevel,
                    containerColor = MaterialTheme.colorScheme.tertiaryContainer,
                    contentColor = MaterialTheme.colorScheme.onTertiaryContainer,
                )
            }
            Spacer(Modifier.weight(1f))
            Text(
                text = if (outcome.fromCache) {
                    "${outcome.hitCount} kez · ${Formatters.relative(outcome.createdAt)}"
                } else {
                    "API kotası kullanıldı"
                },
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

        if (outcome.fromCache) {
            Text(
                text = "Bu sonuç cihazındaki önbellekten geldi; Gemini'ye tekrar istek gönderilmedi.",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 24.dp, vertical = 6.dp),
            )
        }

        if (!contextMatches) {
            Surface(
                modifier = Modifier
                    .padding(horizontal = 24.dp, vertical = 8.dp)
                    .fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                color = MaterialTheme.colorScheme.tertiaryContainer,
            ) {
                Column(Modifier.padding(14.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Outlined.History,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onTertiaryContainer,
                            modifier = Modifier.size(18.dp),
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(
                            text = "Bu kelime farklı bir bağlamda kaydedilmiş",
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.onTertiaryContainer,
                        )
                    }
                    Spacer(Modifier.height(6.dp))
                    Text(
                        text = "Önbellekteki cümle: “${outcome.sentence}”",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onTertiaryContainer.copy(alpha = 0.85f),
                        fontStyle = FontStyle.Italic,
                    )
                    if (onRefresh != null) {
                        Spacer(Modifier.height(8.dp))
                        TextButton(onClick = onRefresh) {
                            Text("Bu bağlam için yeniden analiz et (kota harcar)")
                        }
                    }
                }
            }
        }

        Spacer(Modifier.height(12.dp))

        SectionCard(
            modifier = Modifier.padding(horizontal = 24.dp),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(18.dp),
        ) {
            Text(
                text = "CÜMLEDEKİ KARŞILIĞI",
                style = MaterialTheme.typography.labelSmall,
                letterSpacing = 1.1.sp,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.Bold,
            )
            Spacer(Modifier.height(6.dp))
            Text(
                text = analysis.contextualTranslation.ifBlank { "—" },
                style = MaterialTheme.typography.headlineSmall,
                color = MaterialTheme.colorScheme.onSurface,
            )
            if (analysis.dictionaryMeaning.isNotBlank()) {
                Spacer(Modifier.height(12.dp))
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f))
                Spacer(Modifier.height(12.dp))
                Text(
                    text = "SÖZLÜK ANLAMI",
                    style = MaterialTheme.typography.labelSmall,
                    letterSpacing = 1.1.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontWeight = FontWeight.Bold,
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    text = analysis.dictionaryMeaning,
                    style = MaterialTheme.typography.bodyLarge,
                )
            }
        }

        Spacer(Modifier.height(14.dp))

        val grammarItems = buildList {
            fun addIf(label: String, value: String) {
                if (value.isNotBlank() && value != "-" && value != "—") add(label to value)
            }
            addIf("Tür", analysis.partOfSpeech)
            addIf("Cinsiyet", analysis.gender)
            addIf("Sayı", analysis.number)
            addIf("Hâl", analysis.grammaticalCase)
            addIf("Canlılık", analysis.animacy)
            addIf("Görünüş", analysis.verbAspect)
            addIf("Zaman", analysis.tense)
            addIf("Kişi", analysis.person)
            addIf("Kök", analysis.root)
            addIf("Sözlük biçimi", analysis.lemma)
        }

        if (grammarItems.isNotEmpty()) {
            SectionCard(modifier = Modifier.padding(horizontal = 24.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Outlined.School,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp),
                        tint = MaterialTheme.colorScheme.primary,
                    )
                    Spacer(Modifier.width(8.dp))
                    Text("Dil bilgisi", style = MaterialTheme.typography.titleSmall)
                }
                Spacer(Modifier.height(12.dp))
                grammarItems.chunked(2).forEach { rowItems ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 5.dp),
                        horizontalArrangement = Arrangement.spacedBy(14.dp),
                    ) {
                        rowItems.forEach { (label, value) ->
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = label,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                                Text(
                                    text = value,
                                    style = MaterialTheme.typography.bodyMedium,
                                )
                            }
                        }
                        if (rowItems.size == 1) Spacer(Modifier.weight(1f))
                    }
                }
            }
            Spacer(Modifier.height(14.dp))
        }

        SectionCard(modifier = Modifier.padding(horizontal = 24.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    Icons.Outlined.AccountTree,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp),
                    tint = MaterialTheme.colorScheme.primary,
                )
                Spacer(Modifier.width(8.dp))
                Text("Ek ve kök analizi", style = MaterialTheme.typography.titleSmall)
            }
            Spacer(Modifier.height(10.dp))
            if (analysis.affixes.isEmpty()) {
                Text(
                    text = "Belirgin bir yapım veya çekim eki tespit edilmedi.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            } else {
                analysis.affixes.forEachIndexed { index, affix ->
                    if (index > 0) {
                        HorizontalDivider(
                            modifier = Modifier.padding(vertical = 10.dp),
                            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f),
                        )
                    }
                    Row(verticalAlignment = Alignment.Top) {
                        TagChip(
                            text = affix.affix,
                            containerColor = MaterialTheme.colorScheme.tertiaryContainer,
                            contentColor = MaterialTheme.colorScheme.onTertiaryContainer,
                        )
                        Spacer(Modifier.width(10.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = affix.type.ifBlank { "Ek" },
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                            Text(
                                text = affix.function,
                                style = MaterialTheme.typography.bodyMedium,
                            )
                        }
                    }
                }
            }
            if (analysis.root.isNotBlank() && analysis.root != "-") {
                Spacer(Modifier.height(10.dp))
                Text(
                    text = "Kök: ${analysis.root}  →  ${analysis.lemma.ifBlank { analysis.word }}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }

        if (analysis.grammarNotes.isNotBlank()) {
            Spacer(Modifier.height(14.dp))
            SectionCard(modifier = Modifier.padding(horizontal = 24.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Outlined.Lightbulb,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp),
                        tint = MaterialTheme.colorScheme.tertiary,
                    )
                    Spacer(Modifier.width(8.dp))
                    Text("Dikkat edilmesi gerekenler", style = MaterialTheme.typography.titleSmall)
                }
                Spacer(Modifier.height(8.dp))
                Text(
                    text = analysis.grammarNotes,
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
        }

        if (analysis.example.isNotBlank()) {
            Spacer(Modifier.height(14.dp))
            SectionCard(modifier = Modifier.padding(horizontal = 24.dp)) {
                Text(
                    text = "ÖRNEK",
                    style = MaterialTheme.typography.labelSmall,
                    letterSpacing = 1.1.sp,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold,
                )
                Spacer(Modifier.height(6.dp))
                Text(
                    text = analysis.example,
                    style = MaterialTheme.typography.bodyLarge,
                    fontStyle = FontStyle.Italic,
                )
                if (analysis.exampleTranslation.isNotBlank()) {
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = analysis.exampleTranslation,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }

        if (analysis.synonyms.isNotEmpty() || analysis.collocations.isNotEmpty()) {
            Spacer(Modifier.height(14.dp))
            SectionCard(modifier = Modifier.padding(horizontal = 24.dp)) {
                if (analysis.synonyms.isNotEmpty()) {
                    Text(
                        text = "Eş anlamlılar",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Spacer(Modifier.height(6.dp))
                    Row(
                        modifier = Modifier.horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        analysis.synonyms.forEach { TagChip(text = it) }
                    }
                    Spacer(Modifier.height(12.dp))
                }
                if (analysis.collocations.isNotEmpty()) {
                    Text(
                        text = "Sık kullanılan kalıplar",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Spacer(Modifier.height(6.dp))
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        analysis.collocations.forEach { item ->
                            Text("• $item", style = MaterialTheme.typography.bodyMedium)
                        }
                    }
                }
            }
        }

        if (showActions) {
            Spacer(Modifier.height(20.dp))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                FilledTonalButton(
                    onClick = onSpeakSentence ?: {},
                    modifier = Modifier.weight(1f),
                    enabled = onSpeakSentence != null,
                ) {
                    Icon(
                        Icons.AutoMirrored.Outlined.VolumeUp,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp),
                    )
                    Spacer(Modifier.width(6.dp))
                    Text("Cümleyi dinle")
                }
                OutlinedButton(
                    onClick = onRefresh ?: {},
                    enabled = onRefresh != null,
                ) {
                    Icon(
                        Icons.Outlined.Refresh,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp),
                    )
                    Spacer(Modifier.width(6.dp))
                    Text("Yenile")
                }
                if (onToggleFavorite != null) {
                    IconButton(onClick = onToggleFavorite) {
                        Icon(
                            imageVector = if (outcome.isFavorite) Icons.Outlined.Star else Icons.Outlined.StarBorder,
                            contentDescription = "Favori",
                            tint = if (outcome.isFavorite) MaterialTheme.colorScheme.tertiary
                            else MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
        }
    }
}

private fun highlightedContext(context: String, target: String): AnnotatedString {
    val needle = target.trim()
    if (needle.isEmpty()) return AnnotatedString(context)
    val lowerContext = context.lowercase()
    val lowerNeedle = needle.lowercase()
    val highlight = SpanStyle(
        background = Color(0x556F8F76),
        fontWeight = FontWeight.SemiBold,
        color = Color.Unspecified,
    )

    return buildAnnotatedString {
        append(context)
        var index = lowerContext.indexOf(lowerNeedle)
        while (index >= 0) {
            addStyle(highlight, index, index + needle.length)
            index = lowerContext.indexOf(lowerNeedle, index + needle.length)
        }
    }
}
