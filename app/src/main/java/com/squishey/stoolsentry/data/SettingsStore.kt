package com.squishey.stoolsentry.data

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.squishey.stoolsentry.domain.UserSettings
import java.time.DayOfWeek
import java.time.temporal.WeekFields
import java.util.Locale
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.settingsDataStore by preferencesDataStore(name = "settings")

class SettingsStore(context: Context) {
    private val appContext = context.applicationContext

    val settings: Flow<UserSettings> = appContext.settingsDataStore.data.map { preferences ->
        val storedWeekStart = preferences[Keys.WEEK_START]
            ?.let { name -> runCatching { DayOfWeek.valueOf(name) }.getOrNull() }
        UserSettings(
            weekStart = storedWeekStart ?: WeekFields.of(Locale.getDefault()).firstDayOfWeek,
            disclaimerAccepted = preferences[Keys.DISCLAIMER_ACCEPTED] ?: false,
            appLockEnabled = preferences[Keys.APP_LOCK_ENABLED] ?: false,
        )
    }

    suspend fun setWeekStart(day: DayOfWeek) {
        appContext.settingsDataStore.edit { preferences ->
            preferences[Keys.WEEK_START] = day.name
        }
    }

    suspend fun setDisclaimerAccepted(accepted: Boolean) {
        appContext.settingsDataStore.edit { preferences ->
            preferences[Keys.DISCLAIMER_ACCEPTED] = accepted
        }
    }

    suspend fun setAppLockEnabled(enabled: Boolean) {
        appContext.settingsDataStore.edit { preferences ->
            preferences[Keys.APP_LOCK_ENABLED] = enabled
        }
    }

    private object Keys {
        val WEEK_START = stringPreferencesKey("week_start")
        val DISCLAIMER_ACCEPTED = booleanPreferencesKey("disclaimer_accepted")
        val APP_LOCK_ENABLED = booleanPreferencesKey("app_lock_enabled")
    }
}
