package com.squishey.stoolsentry.ui.settings

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.squishey.stoolsentry.data.AutoExporter
import com.squishey.stoolsentry.data.LogRepository
import com.squishey.stoolsentry.data.SettingsStore
import com.squishey.stoolsentry.data.Vault
import com.squishey.stoolsentry.domain.ImportResult
import com.squishey.stoolsentry.domain.UserSettings
import java.time.DayOfWeek
import java.time.temporal.WeekFields
import java.util.Locale
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class SettingsViewModel(
    private val settingsStore: SettingsStore,
    private val repository: LogRepository,
    private val autoExporter: AutoExporter,
    private val vault: Vault,
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

    val encrypted: StateFlow<Boolean> = vault.encrypted

    fun setWeekStart(day: DayOfWeek) {
        viewModelScope.launch { settingsStore.setWeekStart(day) }
    }

    fun acceptDisclaimer() {
        viewModelScope.launch { settingsStore.setDisclaimerAccepted(true) }
    }

    fun setAppLockEnabled(enabled: Boolean) {
        viewModelScope.launch { settingsStore.setAppLockEnabled(enabled) }
    }

    fun setAutoExportEnabled(enabled: Boolean) {
        viewModelScope.launch { settingsStore.setAutoExportEnabled(enabled) }
    }

    /** Turns auto-export on and writes the file immediately. */
    suspend fun enableAutoExport(): Boolean {
        val uri = settingsStore.settings.first().autoExportTreeUri
        if (uri.isNullOrBlank()) return false
        settingsStore.setAutoExportEnabled(true)
        return autoExporter.exportIfEnabled()
    }

    /** Saves the folder, turns auto-export on, and writes the file. */
    suspend fun useAutoExportFolder(uri: Uri): Boolean {
        if (!autoExporter.persistTree(uri)) return false
        val name = autoExporter.folderName(uri)
        val previous = settingsStore.settings.first().autoExportTreeUri
        settingsStore.setAutoExportDestination(uri.toString(), name)
        if (!previous.isNullOrBlank() && previous != uri.toString()) {
            autoExporter.releaseTree(Uri.parse(previous))
        }
        return autoExporter.exportIfEnabled()
    }

    suspend fun exportCsv(): String = repository.exportCsv()

    suspend fun importCsv(csvText: String): ImportResult {
        val result = repository.import(csvText)
        if (result.imported > 0) autoExporter.enqueue()
        return result
    }
}
