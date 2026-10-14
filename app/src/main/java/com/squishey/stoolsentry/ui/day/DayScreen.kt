package com.squishey.stoolsentry.ui.day

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.squishey.stoolsentry.R
import com.squishey.stoolsentry.domain.BowelLog
import com.squishey.stoolsentry.domain.Symptoms
import com.squishey.stoolsentry.ui.components.BristolGlyph
import com.squishey.stoolsentry.ui.components.ColorDot
import com.squishey.stoolsentry.ui.components.EmptyState
import com.squishey.stoolsentry.ui.components.GoldButton
import com.squishey.stoolsentry.ui.components.IconAction
import com.squishey.stoolsentry.ui.components.ScreenBody
import com.squishey.stoolsentry.ui.components.SmallChip
import com.squishey.stoolsentry.ui.components.ThroneCard
import com.squishey.stoolsentry.ui.components.ThroneTopBar
import com.squishey.stoolsentry.ui.components.rememberSkeletonFormatter
import com.squishey.stoolsentry.ui.components.rememberTimeFormatter
import com.squishey.stoolsentry.ui.theme.QuoteStyle
import com.squishey.stoolsentry.ui.theme.Throne
import java.time.LocalDate

@Composable
fun DayScreen(
    date: LocalDate,
    today: LocalDate,
    logs: List<BowelLog>?,
    onBack: () -> Unit,
    onEdit: (Long) -> Unit,
    onAdd: () -> Unit,
    onDelete: (Long) -> Unit,
) {
    val isFuture = date.isAfter(today)
    val titleFormatter = rememberSkeletonFormatter(if (date.year == today.year) "EEEEMMMMd" else "EEEEMMMMdy")
    val timeFormatter = rememberTimeFormatter()
    var pendingDelete by rememberSaveable { mutableStateOf<Long?>(null) }

    val subtitle = when {
        logs.isNullOrEmpty() -> stringResource(R.string.day_no_logs)
        else -> pluralStringResource(R.plurals.day_log_count, logs.size, logs.size)
    }

    Scaffold(
        containerColor = Color.Transparent,
        topBar = {
            ThroneTopBar(
                title = date.format(titleFormatter),
                subtitle = subtitle,
                onBack = onBack,
            )
        },
        floatingActionButton = {
            if (!isFuture && !logs.isNullOrEmpty()) {
                GoldButton(
                    text = stringResource(R.string.day_add),
                    onClick = onAdd,
                    icon = R.drawable.ic_add,
                    height = 56.dp,
                    modifier = Modifier.shadow(14.dp, CircleShape, ambientColor = Throne.Gold, spotColor = Throne.Gold),
                )
            }
        },
    ) { padding ->
        ScreenBody(Modifier.padding(padding)) {
            when {
                logs == null -> Unit
                logs.isEmpty() -> EmptyState(
                    title = stringResource(R.string.day_empty_title),
                    body = stringResource(if (isFuture) R.string.day_empty_future else R.string.day_empty_body),
                    modifier = Modifier
                        .align(Alignment.Center)
                        .padding(bottom = 48.dp),
                    action = if (isFuture) {
                        null
                    } else {
                        {
                            GoldButton(
                                text = stringResource(R.string.day_add),
                                onClick = onAdd,
                                icon = R.drawable.ic_add,
                            )
                        }
                    },
                )
                else -> LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(start = 12.dp, end = 12.dp, top = 8.dp, bottom = 80.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    items(logs, key = { it.id }) { log ->
                        LogCard(
                            log = log,
                            time = log.localTime().format(timeFormatter),
                            onEdit = { onEdit(log.id) },
                            onDelete = { pendingDelete = log.id },
                            modifier = Modifier.animateItem(),
                        )
                    }
                }
            }
        }
    }

    val deleting = pendingDelete?.let { id -> logs?.firstOrNull { it.id == id } }
    if (deleting != null) {
        AlertDialog(
            onDismissRequest = { pendingDelete = null },
            title = { Text(stringResource(R.string.day_delete_title)) },
            text = {
                Text(stringResource(R.string.day_delete_body, deleting.localTime().format(timeFormatter)))
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        onDelete(deleting.id)
                        pendingDelete = null
                    },
                ) { Text(stringResource(R.string.action_delete), color = Throne.Rose) }
            },
            dismissButton = {
                TextButton(onClick = { pendingDelete = null }) { Text(stringResource(R.string.action_cancel)) }
            },
            containerColor = Throne.CardRaised,
            titleContentColor = Throne.Porcelain,
            textContentColor = Throne.TextMuted,
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun LogCard(
    log: BowelLog,
    time: String,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val swatch = Color(log.color.swatchArgb)
    val tileColor = if (swatch.luminance() < 0.06f) Throne.CardHighest else Throne.Espresso.copy(alpha = 0.55f)
    val symptomLabels = symptomLabels(log.symptoms)
    ThroneCard(
        modifier = modifier
            .fillMaxWidth()
            .animateContentSize(),
        onClick = onEdit,
        onClickLabel = stringResource(R.string.day_edit_log, time),
        contentPadding = PaddingValues(start = 16.dp, top = 16.dp, bottom = 16.dp, end = 4.dp),
    ) {
        Row(verticalAlignment = Alignment.Top) {
            Box(
                Modifier
                    .size(width = 68.dp, height = 56.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(tileColor),
                contentAlignment = Alignment.Center,
            ) {
                BristolGlyph(
                    type = log.bristolType,
                    tint = swatch,
                    modifier = Modifier.size(width = 54.dp, height = 38.dp),
                )
            }
            Spacer(Modifier.width(14.dp))
            Column(Modifier.weight(1f)) {
                Text(time, style = MaterialTheme.typography.titleLarge, color = Throne.Porcelain)
                Spacer(Modifier.height(2.dp))
                Text(
                    text = stringResource(R.string.day_type_size, log.bristolType.label, log.size.label),
                    style = MaterialTheme.typography.bodyMedium,
                    color = Throne.Porcelain,
                )
                Spacer(Modifier.height(4.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    ColorDot(log.color, size = 12.dp)
                    Spacer(Modifier.width(6.dp))
                    Text(log.color.label, style = MaterialTheme.typography.bodySmall, color = Throne.TextMuted)
                }
            }
            IconAction(
                icon = R.drawable.ic_delete,
                contentDescription = stringResource(R.string.day_delete_log, time),
                onClick = onDelete,
                tint = Throne.TextFaint,
            )
        }
        if (symptomLabels.isNotEmpty()) {
            Spacer(Modifier.height(12.dp))
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp),
                modifier = Modifier.padding(end = 12.dp),
            ) {
                symptomLabels.forEach { (label, alert) ->
                    SmallChip(
                        text = label,
                        tone = if (alert) Throne.Rose else Throne.Porcelain,
                        background = if (alert) Throne.RoseContainer else Throne.CardRaised,
                    )
                }
            }
        }
        if (log.notes.isNotBlank()) {
            Spacer(Modifier.height(12.dp))
            Text(
                text = log.notes,
                style = QuoteStyle,
                color = Throne.TextMuted,
                maxLines = 3,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(end = 12.dp),
            )
        }
    }
}

@Composable
private fun symptomLabels(symptoms: Symptoms): List<Pair<String, Boolean>> = buildList {
    if (symptoms.urgent) add(stringResource(R.string.symptom_urgent) to false)
    if (symptoms.blood) add(stringResource(R.string.symptom_blood) to true)
    if (symptoms.painful) add(stringResource(R.string.symptom_painful) to false)
    if (symptoms.straining) add(stringResource(R.string.symptom_straining) to false)
    if (symptoms.incomplete) add(stringResource(R.string.symptom_incomplete) to false)
    if (symptoms.mucus) add(stringResource(R.string.symptom_mucus) to false)
}
