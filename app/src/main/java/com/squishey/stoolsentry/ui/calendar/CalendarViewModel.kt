package com.squishey.stoolsentry.ui.calendar

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.squishey.stoolsentry.data.LogRepository
import com.squishey.stoolsentry.data.SettingsStore
import com.squishey.stoolsentry.domain.AppClock
import com.squishey.stoolsentry.domain.BowelLog
import com.squishey.stoolsentry.domain.CalendarIndex
import com.squishey.stoolsentry.domain.MonthTally
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.YearMonth
import java.time.temporal.WeekFields
import java.util.Locale
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn

data class CalendarUiState(
    val today: LocalDate,
    val visibleMonth: YearMonth,
    val selectedDate: LocalDate,
    val countsByDate: Map<LocalDate, Int>,
    val weekStart: DayOfWeek,
    val latestLog: BowelLog? = null,
    val monthTally: MonthTally = MonthTally(logCount = 0, loggedDays = 0),
    val loaded: Boolean = false,
)

class CalendarViewModel(
    repository: LogRepository,
    settingsStore: SettingsStore,
    private val clock: AppClock,
) : ViewModel() {
    private val today = MutableStateFlow(clock.today())
    private val visibleMonth = MutableStateFlow(YearMonth.from(today.value))
    private val selectedDate = MutableStateFlow(today.value)

    val uiState: StateFlow<CalendarUiState> = combine(
        repository.observeAll(),
        today,
        visibleMonth,
        selectedDate,
        settingsStore.settings,
    ) { logs, currentToday, month, selected, settings ->
        val counts = CalendarIndex.countsByDate(logs)
        CalendarUiState(
            today = currentToday,
            visibleMonth = month,
            selectedDate = selected,
            countsByDate = counts,
            weekStart = settings.weekStart,
            latestLog = CalendarIndex.latest(logs),
            monthTally = CalendarIndex.monthTally(counts, month),
            loaded = true,
        )
    }.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5_000),
        CalendarUiState(
            today = today.value,
            visibleMonth = visibleMonth.value,
            selectedDate = selectedDate.value,
            countsByDate = emptyMap(),
            weekStart = WeekFields.of(Locale.getDefault()).firstDayOfWeek,
        ),
    )

    fun select(date: LocalDate) {
        selectedDate.value = date
    }

    /** Selects [date] and shows its month, e.g. after a log is saved. */
    fun focus(date: LocalDate) {
        refreshToday()
        selectedDate.value = date
        visibleMonth.value = YearMonth.from(date)
    }

    fun showPreviousMonth() {
        visibleMonth.value = visibleMonth.value.minusMonths(1)
    }

    fun showNextMonth() {
        visibleMonth.value = visibleMonth.value.plusMonths(1)
    }

    fun jumpToToday() {
        val current = clock.today()
        today.value = current
        selectedDate.value = current
        visibleMonth.value = YearMonth.from(current)
    }

    fun refreshToday() {
        val previousToday = today.value
        val current = clock.today()
        if (current == previousToday) return
        today.value = current
        if (selectedDate.value == previousToday) {
            selectedDate.value = current
        }
        if (YearMonth.from(previousToday) == visibleMonth.value && YearMonth.from(current) != visibleMonth.value) {
            visibleMonth.value = YearMonth.from(current)
        }
    }
}
