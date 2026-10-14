package com.squishey.stoolsentry.ui.summary

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.squishey.stoolsentry.data.LogRepository
import com.squishey.stoolsentry.domain.AppClock
import com.squishey.stoolsentry.domain.StatsCalculator
import com.squishey.stoolsentry.domain.SummaryPeriod
import com.squishey.stoolsentry.domain.SummarySnapshot
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn

data class SummaryUiState(
    val period: SummaryPeriod,
    val snapshot: SummarySnapshot,
)

class SummaryViewModel(
    repository: LogRepository,
    private val clock: AppClock,
) : ViewModel() {
    private val period = MutableStateFlow(SummaryPeriod.DAYS_30)
    private val today = MutableStateFlow(clock.today())

    val uiState: StateFlow<SummaryUiState> = combine(
        repository.observeAll(),
        period,
        today,
    ) { logs, selectedPeriod, currentToday ->
        SummaryUiState(
            period = selectedPeriod,
            snapshot = StatsCalculator.summarize(logs, selectedPeriod, currentToday),
        )
    }.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5_000),
        SummaryUiState(
            period = period.value,
            snapshot = StatsCalculator.summarize(emptyList(), period.value, today.value),
        ),
    )

    fun selectPeriod(period: SummaryPeriod) {
        this.period.value = period
    }

    fun refreshToday() {
        today.value = clock.today()
    }
}
