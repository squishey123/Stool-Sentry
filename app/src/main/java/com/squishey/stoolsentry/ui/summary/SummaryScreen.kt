package com.squishey.stoolsentry.ui.summary

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.squishey.stoolsentry.R
import com.squishey.stoolsentry.domain.BristolType
import com.squishey.stoolsentry.domain.CountShare
import com.squishey.stoolsentry.domain.StoolColor
import com.squishey.stoolsentry.domain.SummaryPeriod
import com.squishey.stoolsentry.domain.SummarySnapshot
import com.squishey.stoolsentry.domain.TimeOfDay
import com.squishey.stoolsentry.ui.components.BristolGlyph
import com.squishey.stoolsentry.ui.components.ChoicePill
import com.squishey.stoolsentry.ui.components.ColorDot
import com.squishey.stoolsentry.ui.components.EmptyState
import com.squishey.stoolsentry.ui.components.ScreenBody
import com.squishey.stoolsentry.ui.components.SectionLabel
import com.squishey.stoolsentry.ui.components.ShareRow
import com.squishey.stoolsentry.ui.components.StatTile
import com.squishey.stoolsentry.ui.components.ThroneCard
import com.squishey.stoolsentry.ui.components.ThroneTopBar
import com.squishey.stoolsentry.ui.components.currentLocale
import com.squishey.stoolsentry.ui.components.rememberSkeletonFormatter
import com.squishey.stoolsentry.ui.theme.Throne
import kotlin.math.roundToInt

@Composable
fun SummaryScreen(
    state: SummaryUiState,
    onBack: () -> Unit,
    onSelectPeriod: (SummaryPeriod) -> Unit,
) {
    val snapshot = state.snapshot
    Scaffold(
        containerColor = Color.Transparent,
        topBar = {
            ThroneTopBar(title = stringResource(R.string.summary_title), onBack = onBack)
        },
    ) { padding ->
        ScreenBody(Modifier.padding(padding)) {
            if (snapshot.isEmpty) {
                EmptyState(
                    title = stringResource(R.string.summary_empty_title),
                    body = stringResource(R.string.summary_empty_body),
                    modifier = Modifier
                        .align(Alignment.Center)
                        .padding(bottom = 48.dp),
                )
            } else {
                Column(
                    Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                        .padding(horizontal = 12.dp)
                        .padding(bottom = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    SectionLabel(
                        stringResource(R.string.summary_eyebrow),
                        modifier = Modifier.padding(start = 8.dp, top = 4.dp),
                    )
                    PeriodSelector(state.period, onSelectPeriod)
                    RangeLine(snapshot)
                    HeadlineStats(snapshot)
                    AnimatedVisibility(visible = snapshot.showClinicianNote) {
                        ClinicianNote()
                    }
                    if (snapshot.logCount == 0) {
                        ThroneCard(Modifier.fillMaxWidth()) {
                            Text(
                                text = stringResource(R.string.summary_period_empty),
                                style = MaterialTheme.typography.bodyMedium,
                                color = Throne.TextMuted,
                            )
                        }
                    } else {
                        Distributions(snapshot)
                    }
                    Text(
                        text = stringResource(R.string.summary_footer),
                        style = MaterialTheme.typography.bodySmall,
                        color = Throne.TextFaint,
                        textAlign = TextAlign.Center,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 8.dp),
                    )
                }
            }
        }
    }
}

@Composable
private fun PeriodSelector(selected: SummaryPeriod, onSelect: (SummaryPeriod) -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .clip(CircleShape)
            .background(Throne.Card)
            .border(1.dp, Throne.OutlineSoft, CircleShape)
            .padding(4.dp)
            .selectableGroup(),
    ) {
        SummaryPeriod.entries.forEach { period ->
            ChoicePill(
                text = stringResource(periodLabel(period)),
                selected = period == selected,
                onClick = { onSelect(period) },
                modifier = Modifier.weight(1f),
            )
        }
    }
}

private fun periodLabel(period: SummaryPeriod): Int = when (period) {
    SummaryPeriod.DAYS_7 -> R.string.period_7
    SummaryPeriod.DAYS_30 -> R.string.period_30
    SummaryPeriod.DAYS_90 -> R.string.period_90
    SummaryPeriod.ALL -> R.string.period_all
}

@Composable
private fun RangeLine(snapshot: SummarySnapshot) {
    val start = snapshot.rangeStart ?: return
    val shortFormatter = rememberSkeletonFormatter(if (start.year == snapshot.rangeEnd.year) "MMMd" else "MMMdy")
    val longFormatter = rememberSkeletonFormatter("MMMdy")
    Text(
        text = stringResource(
            R.string.summary_range,
            start.format(shortFormatter),
            snapshot.rangeEnd.format(longFormatter),
        ),
        style = MaterialTheme.typography.bodySmall,
        color = Throne.TextMuted,
        textAlign = TextAlign.Center,
        modifier = Modifier.fillMaxWidth(),
    )
}

@Composable
private fun HeadlineStats(snapshot: SummarySnapshot) {
    val locale = currentLocale()
    val noValue = stringResource(R.string.no_value)
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            StatTile(
                label = stringResource(R.string.summary_logs),
                value = snapshot.logCount.toString(),
                modifier = Modifier.weight(1f),
            )
            StatTile(
                label = stringResource(R.string.summary_per_day),
                value = String.format(locale, "%.1f", snapshot.averagePerDay),
                modifier = Modifier.weight(1f),
            )
        }
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            StatTile(
                label = stringResource(R.string.summary_since_last),
                value = snapshot.daysSinceLast?.toString() ?: noValue,
                modifier = Modifier.weight(1f),
            )
            val gap = snapshot.longestGapDays
            StatTile(
                label = stringResource(R.string.summary_longest_gap),
                value = gap?.toString() ?: noValue,
                unit = gap?.let { pluralStringResource(R.plurals.summary_days_unit, it) },
                modifier = Modifier.weight(1f),
            )
        }
    }
}

@Composable
private fun Distributions(snapshot: SummarySnapshot) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        DistributionCard(stringResource(R.string.summary_bristol)) {
            snapshot.bristol.forEach { share ->
                val type = share.key.toIntOrNull()?.let(BristolType::fromNumber)
                ShareRow(
                    label = share.label,
                    value = shareValue(share),
                    fraction = share.fraction.toFloat(),
                    caption = type?.description,
                    leading = type?.let {
                        { BristolGlyph(it, Modifier.size(width = 36.dp, height = 24.dp)) }
                    },
                )
            }
        }
        DistributionCard(stringResource(R.string.summary_size)) {
            snapshot.sizes.forEach { share ->
                ShareRow(label = share.label, value = shareValue(share), fraction = share.fraction.toFloat())
            }
        }
        DistributionCard(stringResource(R.string.summary_color)) {
            snapshot.colors.filter { it.count > 0 }.forEach { share ->
                val color = runCatching { StoolColor.valueOf(share.key) }.getOrNull()
                ShareRow(
                    label = share.label,
                    value = shareValue(share),
                    fraction = share.fraction.toFloat(),
                    leading = color?.let { { ColorDot(it, size = 16.dp) } },
                )
            }
        }
        DistributionCard(stringResource(R.string.summary_time_of_day)) {
            snapshot.timeOfDay.forEach { share ->
                val range = runCatching { TimeOfDay.valueOf(share.key) }.getOrNull()?.let { timeRange(it) }
                ShareRow(
                    label = share.label,
                    value = shareValue(share),
                    fraction = share.fraction.toFloat(),
                    caption = range?.let { stringResource(it) },
                )
            }
        }
        DistributionCard(
            title = stringResource(R.string.summary_symptoms),
            subtitle = stringResource(R.string.summary_symptoms_hint),
        ) {
            val rates = snapshot.symptomRates
            listOf(
                R.string.symptom_urgent to rates.urgent,
                R.string.symptom_blood to rates.blood,
                R.string.symptom_painful to rates.painful,
                R.string.symptom_straining to rates.straining,
                R.string.symptom_incomplete to rates.incomplete,
                R.string.symptom_mucus to rates.mucus,
            ).forEach { (label, rate) ->
                ShareRow(
                    label = stringResource(label),
                    value = stringResource(R.string.summary_percent, (rate * 100).roundToInt()),
                    fraction = rate.toFloat(),
                )
            }
        }
    }
}

private fun timeRange(time: TimeOfDay): Int = when (time) {
    TimeOfDay.MORNING -> R.string.time_morning_range
    TimeOfDay.AFTERNOON -> R.string.time_afternoon_range
    TimeOfDay.EVENING -> R.string.time_evening_range
    TimeOfDay.NIGHT -> R.string.time_night_range
}

@Composable
private fun shareValue(share: CountShare): String =
    stringResource(R.string.summary_share, share.count, (share.fraction * 100).roundToInt())

@Composable
private fun DistributionCard(
    title: String,
    subtitle: String? = null,
    content: @Composable ColumnScope.() -> Unit,
) {
    ThroneCard(
        modifier = Modifier.fillMaxWidth(),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 10.dp),
    ) {
        SectionLabel(title)
        if (subtitle != null) {
            Spacer(Modifier.height(4.dp))
            Text(subtitle, style = MaterialTheme.typography.bodySmall, color = Throne.TextFaint)
        }
        Spacer(Modifier.height(6.dp))
        content()
    }
}

@Composable
private fun ClinicianNote() {
    ThroneCard(
        modifier = Modifier.fillMaxWidth(),
        color = Throne.RoseContainer,
        contentPadding = PaddingValues(16.dp),
    ) {
        Row(verticalAlignment = Alignment.Top) {
            Icon(
                painter = painterResource(R.drawable.ic_info),
                contentDescription = null,
                tint = Throne.Rose,
                modifier = Modifier.size(22.dp),
            )
            Spacer(Modifier.width(12.dp))
            Column {
                Text(
                    text = stringResource(R.string.summary_clinician_title),
                    style = MaterialTheme.typography.titleMedium,
                    color = Throne.Rose,
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    text = stringResource(R.string.summary_clinician_body),
                    style = MaterialTheme.typography.bodyMedium,
                    color = Throne.Porcelain.copy(alpha = 0.88f),
                )
            }
        }
    }
}
