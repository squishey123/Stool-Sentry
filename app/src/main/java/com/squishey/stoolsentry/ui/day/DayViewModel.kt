package com.squishey.stoolsentry.ui.day

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.squishey.stoolsentry.data.LogRepository
import com.squishey.stoolsentry.domain.AppClock
import com.squishey.stoolsentry.domain.BowelLog
import com.squishey.stoolsentry.ui.nav.RoutePatterns
import java.time.LocalDate
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class DayViewModel(
    private val repository: LogRepository,
    private val clock: AppClock,
    savedStateHandle: SavedStateHandle,
) : ViewModel() {
    val date: LocalDate = savedStateHandle.get<String>(RoutePatterns.ARG_DATE)
        ?.let { runCatching { LocalDate.parse(it) }.getOrNull() }
        ?: clock.today()

    /** Null until the first database read finishes. */
    val loadedLogs: StateFlow<List<BowelLog>?> = repository.observeByDate(date)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    val logs: StateFlow<List<BowelLog>> = loadedLogs
        .map { it.orEmpty() }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    fun isFuture(): Boolean = date.isAfter(clock.today())

    fun delete(id: Long) {
        viewModelScope.launch {
            repository.delete(id)
        }
    }
}
