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
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class DayViewModel(
    private val repository: LogRepository,
    clock: AppClock,
    savedStateHandle: SavedStateHandle,
) : ViewModel() {
    val date: LocalDate = savedStateHandle.get<String>(RoutePatterns.ARG_DATE)
        ?.let { runCatching { LocalDate.parse(it) }.getOrNull() }
        ?: clock.today()

    val logs: StateFlow<List<BowelLog>> = repository.observeByDate(date)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    fun delete(id: Long) {
        viewModelScope.launch {
            repository.delete(id)
        }
    }
}
