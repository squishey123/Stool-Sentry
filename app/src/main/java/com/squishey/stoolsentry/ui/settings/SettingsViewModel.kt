package com.squishey.stoolsentry.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.squishey.stoolsentry.data.LogRepository
import com.squishey.stoolsentry.data.SettingsStore
import com.squishey.stoolsentry.domain.ImportResult
import com.squishey.stoolsentry.domain.UserSettings
import java.time.DayOfWeek
import java.time.temporal.WeekFields
import java.util.Locale
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class SettingsViewModel(
    private val settingsStore: SettingsStore,
    private val repository: LogRepository,
) : ViewModel() {
    val settings: StateFlow<UserSettings> = settingsStore.settings.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5_000),
        UserSettings(
            weekStart = WeekFields.of(Locale.getDefault()).firstDayOfWeek,
            disclaimerAccepted = false,
            appLockEnabled = false,
        ),
    )

    fun setWeekStart(day: DayOfWeek) {
        viewModelScope.launch { settingsStore.setWeekStart(day) }
    }

    fun acceptDisclaimer() {
        viewModelScope.launch { settingsStore.setDisclaimerAccepted(true) }
    }

    fun setAppLockEnabled(enabled: Boolean) {
        viewModelScope.launch { settingsStore.setAppLockEnabled(enabled) }
    }

    suspend fun exportCsv(): String = repository.exportCsv()

    suspend fun importCsv(csvText: String): ImportResult = repository.import(csvText)
}
