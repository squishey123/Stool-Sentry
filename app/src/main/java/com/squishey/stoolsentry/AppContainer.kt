package com.squishey.stoolsentry

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.createSavedStateHandle
import androidx.lifecycle.viewmodel.CreationExtras
import com.squishey.stoolsentry.data.AppDatabase
import com.squishey.stoolsentry.data.LogRepository
import com.squishey.stoolsentry.data.SettingsStore
import com.squishey.stoolsentry.domain.SystemAppClock
import com.squishey.stoolsentry.ui.calendar.CalendarViewModel
import com.squishey.stoolsentry.ui.day.DayViewModel
import com.squishey.stoolsentry.ui.log.LogWizardViewModel
import com.squishey.stoolsentry.ui.settings.SettingsViewModel
import com.squishey.stoolsentry.ui.summary.SummaryViewModel

class AppContainer(context: Context) {
    val clock = SystemAppClock()
    private val database = AppDatabase.create(context)
    val repository = LogRepository(database.bowelLogDao(), clock)
    val settings = SettingsStore(context)
    val viewModelFactory = AppViewModelFactory(this)
}

class AppViewModelFactory(
    private val container: AppContainer,
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>, extras: CreationExtras): T {
        val handle = extras.createSavedStateHandle()
        val viewModel = when {
            modelClass.isAssignableFrom(CalendarViewModel::class.java) ->
                CalendarViewModel(container.repository, container.settings, container.clock)
            modelClass.isAssignableFrom(LogWizardViewModel::class.java) ->
                LogWizardViewModel(container.repository, container.clock, handle)
            modelClass.isAssignableFrom(DayViewModel::class.java) ->
                DayViewModel(container.repository, container.clock, handle)
            modelClass.isAssignableFrom(SummaryViewModel::class.java) ->
                SummaryViewModel(container.repository, container.clock)
            modelClass.isAssignableFrom(SettingsViewModel::class.java) ->
                SettingsViewModel(container.settings, container.repository)
            else -> throw IllegalArgumentException("Unknown ViewModel: ${modelClass.name}")
        }
        @Suppress("UNCHECKED_CAST")
        return viewModel as T
    }
}
