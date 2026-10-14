package com.squishey.stoolsentry.ui.calendar

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.SizeTransform
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.squishey.stoolsentry.R
import com.squishey.stoolsentry.domain.BowelLog
import com.squishey.stoolsentry.ui.components.BrandMark
import com.squishey.stoolsentry.ui.components.GoldButton
import com.squishey.stoolsentry.ui.components.GoldGradient
import com.squishey.stoolsentry.ui.components.IconAction
import com.squishey.stoolsentry.ui.components.ScreenBody
import com.squishey.stoolsentry.ui.components.StatTile
import com.squishey.stoolsentry.ui.components.ThroneCard
import com.squishey.stoolsentry.ui.components.ThroneTopBar
import com.squishey.stoolsentry.ui.components.currentLocale
import com.squishey.stoolsentry.ui.components.rememberSkeletonFormatter
import com.squishey.stoolsentry.ui.components.rememberTimeFormatter
import com.squishey.stoolsentry.ui.theme.Throne
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.TextStyle
import java.time.temporal.ChronoUnit

@Composable
fun CalendarScreen(
    state: CalendarUiState,
    hourOfDay: Int,
    onSelectDay: (LocalDate) -> Unit,
    onPreviousMonth: () -> Unit,
    onNextMonth: () -> Unit,
    onToday: () -> Unit,
    onLog: () -> Unit,
    onSummary: () -> Unit,
    onSettings: () -> Unit,
) {
    Scaffold(
        containerColor = Color.Transparent,
        topBar = {
            ThroneTopBar(
                title = stringResource(R.string.app_name),
                leading = { BrandMark(width = 30.dp) },
                actions = {
                    IconAction(R.drawable.ic_insights, stringResource(R.string.calendar_summary), onSummary)
                    IconAction(R.drawable.ic_settings, stringResource(R.string.calendar_settings), onSettings)
                },
            )
        },
        floatingActionButton = {
            LogButton(state = state, onLog = onLog)
        },
    ) { padding ->
        ScreenBody(Modifier.padding(padding)) {
            Column(
                Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 12.dp)
                    .padding(bottom = 72.dp),
            ) {
                Greeting(state = state, hourOfDay = hourOfDay)
                Spacer(Modifier.height(16.dp))
                MonthCard(
                    state = state,
                    onSelectDay = onSelectDay,
                    onPreviousMonth = onPreviousMonth,
                    onNextMonth = onNextMonth,
                    onToday = onToday,
                )
                Spacer(Modifier.height(12.dp))
                if (state.loaded && state.latestLog == null) {
                    FirstRunCard()
                } else if (state.loaded) {
                    MonthGlance(state)
                }
            }
        }
    }
}

@Composable
private fun Greeting(state: CalendarUiState, hourOfDay: Int) {
    val greeting = when (hourOfDay) {
        in 5..11 -> R.string.greeting_morning
        in 12..16 -> R.string.greeting_afternoon
        in 17..22 -> R.string.greeting_evening
        else -> R.string.greeting_night
    }
    Column(Modifier.padding(horizontal = 8.dp, vertical = 4.dp)) {
        Text(
            text = stringResource(greeting),
            style = MaterialTheme.typography.headlineMedium,
            color = Throne.Porcelain,
        )
        Spacer(Modifier.height(6.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                painter = painterResource(R.drawable.ic_crown),
                contentDescription = null,
                tint = Throne.Gold,
                modifier = Modifier.size(16.dp),
            )
            Spacer(Modifier.width(8.dp))
            Text(
                text = lastVisitText(state.latestLog, state.today),
                style = MaterialTheme.typography.bodyMedium,
                color = Throne.TextMuted,
            )
        }
    }
}

@Composable
private fun lastVisitText(latest: BowelLog?, today: LocalDate): String {
    if (latest == null) return stringResource(R.string.last_visit_none)
    val time = latest.localTime().format(rememberTimeFormatter())
    val days = ChronoUnit.DAYS.between(latest.localDate, today).toInt()
    return when {
        days <= 0 -> stringResource(R.string.last_visit_today, time)
        days == 1 -> stringResource(R.string.last_visit_yesterday, time)
        else -> pluralStringResource(R.plurals.last_visit_days_ago, days, days)
    }
}

@Composable
private fun MonthCard(
    state: CalendarUiState,
    onSelectDay: (LocalDate) -> Unit,
    onPreviousMonth: () -> Unit,
    onNextMonth: () -> Unit,
    onToday: () -> Unit,
) {
    val currentMonth = YearMonth.from(state.today)
    val canGoNext = state.visibleMonth < currentMonth
    val showToday = state.visibleMonth != currentMonth || state.selectedDate != state.today
    val monthFormatter = rememberSkeletonFormatter("MMMMy")
    val swipeThreshold = with(LocalDensity.current) { 56.dp.toPx() }
    val latestPrevious by rememberUpdatedState(onPreviousMonth)
    val latestNext by rememberUpdatedState(onNextMonth)
    val latestCanGoNext by rememberUpdatedState(canGoNext)

    ThroneCard(
        modifier = Modifier.fillMaxWidth(),
        contentPadding = PaddingValues(top = 8.dp, bottom = 10.dp),
    ) {
        Row(
            Modifier
                .fillMaxWidth()
                .padding(start = 16.dp, end = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            AnimatedContent(
                targetState = state.visibleMonth,
                transitionSpec = { fadeIn(tween(220)) togetherWith fadeOut(tween(120)) },
                modifier = Modifier.weight(1f),
                label = "monthTitle",
            ) { month ->
                Text(
                    text = month.atDay(1).format(monthFormatter),
                    style = MaterialTheme.typography.headlineSmall,
                    color = Throne.Porcelain,
                )
            }
            AnimatedVisibility(
                visible = showToday,
                enter = fadeIn() + scaleIn(initialScale = 0.8f),
                exit = fadeOut() + scaleOut(targetScale = 0.8f),
            ) {
                TodayPill(onClick = onToday)
            }
            IconAction(
                R.drawable.ic_chevron_left,
                stringResource(R.string.calendar_previous_month),
                onPreviousMonth,
                tint = Throne.GoldLight,
            )
            IconAction(
                R.drawable.ic_chevron_right,
                stringResource(R.string.calendar_next_month),
                onNextMonth,
                tint = Throne.GoldLight,
                enabled = canGoNext,
            )
        }
        Spacer(Modifier.height(6.dp))
        WeekdayHeader(state.weekStart)
        AnimatedContent(
            targetState = state.visibleMonth,
            transitionSpec = {
                val forward = targetState > initialState
                val enter = slideInHorizontally(tween(320)) { width -> if (forward) width / 3 else -width / 3 } +
                    fadeIn(tween(260))
                val exit = slideOutHorizontally(tween(320)) { width -> if (forward) -width / 3 else width / 3 } +
                    fadeOut(tween(160))
                (enter togetherWith exit).using(SizeTransform(clip = false))
            },
            modifier = Modifier.pointerInput(Unit) {
                var total = 0f
                detectHorizontalDragGestures(
                    onDragStart = { total = 0f },
                    onDragEnd = {
                        when {
                            total > swipeThreshold -> latestPrevious()
                            total < -swipeThreshold && latestCanGoNext -> latestNext()
                        }
                    },
                    onHorizontalDrag = { _, delta -> total += delta },
                )
            },
            label = "monthGrid",
        ) { month ->
            MonthGrid(month = month, state = state, onSelectDay = onSelectDay)
        }
    }
}

@Composable
private fun TodayPill(onClick: () -> Unit) {
    Box(
        Modifier
            .padding(end = 4.dp)
            .height(48.dp)
            .clip(CircleShape)
            .clickable(role = Role.Button, onClick = onClick)
            .padding(horizontal = 4.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = stringResource(R.string.calendar_today),
            style = MaterialTheme.typography.labelMedium,
            color = Throne.Gold,
            modifier = Modifier
                .border(1.dp, Throne.GoldDim, CircleShape)
                .padding(horizontal = 12.dp, vertical = 6.dp),
        )
    }
}

@Composable
private fun WeekdayHeader(weekStart: DayOfWeek) {
    val locale = currentLocale()
    Row(
        Modifier
            .fillMaxWidth()
            .clearAndSetSemantics { },
    ) {
        for (offset in 0 until 7) {
            val day = weekStart.plus(offset.toLong())
            Text(
                text = day.getDisplayName(TextStyle.NARROW, locale),
                style = MaterialTheme.typography.labelMedium,
                color = Throne.TextFaint,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .weight(1f)
                    .padding(vertical = 6.dp),
            )
        }
    }
}

@Composable
private fun MonthGrid(
    month: YearMonth,
    state: CalendarUiState,
    onSelectDay: (LocalDate) -> Unit,
) {
    val first = month.atDay(1)
    val leading = (first.dayOfWeek.value - state.weekStart.value + 7) % 7
    val length = month.lengthOfMonth()
    val dayFormatter = rememberSkeletonFormatter("EEEEMMMMd")
    val rows = (leading + length + 6) / 7
    Column(Modifier.fillMaxWidth()) {
        for (row in 0 until rows) {
            Row(Modifier.fillMaxWidth()) {
                for (column in 0 until 7) {
                    val dayNumber = row * 7 + column - leading + 1
                    if (dayNumber in 1..length) {
                        val date = month.atDay(dayNumber)
                        DayCell(
                            date = date,
                            count = state.countsByDate[date] ?: 0,
                            isToday = date == state.today,
                            isSelected = date == state.selectedDate,
                            isFuture = date.isAfter(state.today),
                            label = date.format(dayFormatter),
                            onClick = { onSelectDay(date) },
                            modifier = Modifier.weight(1f),
                        )
                    } else {
                        Spacer(
                            Modifier
                                .weight(1f)
                                .height(CELL_HEIGHT),
                        )
                    }
                }
            }
        }
    }
}

private val CELL_HEIGHT = 50.dp

@Composable
private fun DayCell(
    date: LocalDate,
    count: Int,
    isToday: Boolean,
    isSelected: Boolean,
    isFuture: Boolean,
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val description = when {
        count > 0 -> pluralStringResource(R.plurals.calendar_day_logs, count, label, count)
        isToday -> stringResource(R.string.calendar_day_today, label)
        else -> label
    }
    val circleBackground = when {
        isSelected -> GoldGradient
        count > 0 -> Brush.verticalGradient(listOf(Throne.CardHighest, Throne.CardRaised))
        else -> Brush.verticalGradient(listOf(Color.Transparent, Color.Transparent))
    }
    val textColor = when {
        isSelected -> Throne.OnGold
        isFuture -> Throne.TextFaint.copy(alpha = 0.45f)
        isToday -> Throne.GoldLight
        else -> Throne.Porcelain
    }
    Box(
        modifier = modifier
            .height(CELL_HEIGHT)
            .clickable(enabled = !isFuture, role = Role.Button, onClick = onClick)
            .semantics(mergeDescendants = true) {
                contentDescription = description
                selected = isSelected
            },
        contentAlignment = Alignment.Center,
    ) {
        val ring = if (isToday && !isSelected) {
            Modifier.border(1.5.dp, Throne.Gold, CircleShape)
        } else {
            Modifier
        }
        Box(
            Modifier
                .size(40.dp)
                .clip(CircleShape)
                .background(circleBackground)
                .then(ring),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = date.dayOfMonth.toString(),
                style = MaterialTheme.typography.bodyMedium.copy(
                    fontWeight = if (isToday || isSelected || count > 0) FontWeight.Bold else FontWeight.Medium,
                ),
                color = textColor,
            )
        }
        if (count == 1) {
            Box(
                Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 1.dp)
                    .size(5.dp)
                    .clip(CircleShape)
                    .background(Throne.Gold),
            )
        } else if (count > 1) {
            Box(
                Modifier
                    .align(Alignment.TopEnd)
                    .padding(top = 1.dp, end = 1.dp)
                    .size(18.dp)
                    .clip(CircleShape)
                    .background(Throne.GoldLight)
                    .border(1.5.dp, Throne.Card, CircleShape),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = if (count > 9) "9+" else count.toString(),
                    color = Throne.OnGold,
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.labelSmall.copy(letterSpacing = 0.sp),
                )
            }
        }
    }
}

@Composable
private fun MonthGlance(state: CalendarUiState) {
    val monthName = state.visibleMonth.month.getDisplayName(TextStyle.FULL_STANDALONE, currentLocale())
    Column {
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            StatTile(
                label = stringResource(R.string.calendar_month_logs, monthName),
                value = state.monthTally.logCount.toString(),
                modifier = Modifier.weight(1f),
            )
            StatTile(
                label = stringResource(R.string.calendar_month_days, monthName),
                value = state.monthTally.loggedDays.toString(),
                modifier = Modifier.weight(1f),
            )
        }
        Spacer(Modifier.height(12.dp))
        Text(
            text = stringResource(R.string.calendar_hint),
            style = MaterialTheme.typography.bodySmall,
            color = Throne.TextFaint,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

@Composable
private fun FirstRunCard() {
    ThroneCard(modifier = Modifier.fillMaxWidth(), contentPadding = PaddingValues(20.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            BrandMark(width = 44.dp, glow = true)
            Spacer(Modifier.width(16.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    text = stringResource(R.string.calendar_empty_title),
                    style = MaterialTheme.typography.titleLarge,
                    color = Throne.Porcelain,
                )
                Spacer(Modifier.height(6.dp))
                Text(
                    text = stringResource(R.string.calendar_empty_body),
                    style = MaterialTheme.typography.bodyMedium,
                    color = Throne.TextMuted,
                )
            }
        }
    }
}

@Composable
private fun LogButton(state: CalendarUiState, onLog: () -> Unit) {
    val formatter = rememberSkeletonFormatter("MMMd")
    val label = if (state.selectedDate == state.today) {
        stringResource(R.string.calendar_log)
    } else {
        stringResource(R.string.calendar_log_for, state.selectedDate.format(formatter))
    }
    GoldButton(
        text = label,
        onClick = onLog,
        icon = R.drawable.ic_add,
        height = 56.dp,
        enabled = !state.selectedDate.isAfter(state.today),
        modifier = Modifier.shadow(
            elevation = 14.dp,
            shape = CircleShape,
            ambientColor = Throne.Gold,
            spotColor = Throne.Gold,
        ),
    )
}
