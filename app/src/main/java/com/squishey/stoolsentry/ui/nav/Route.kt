package com.squishey.stoolsentry.ui.nav

import java.time.LocalDate

sealed interface Route {
    data object Calendar : Route

    data class Wizard(
        val logId: Long? = null,
        val presetDate: LocalDate? = null,
    ) : Route

    data class Day(val date: LocalDate) : Route

    data object Summary : Route

    data object Settings : Route
}

object RoutePatterns {
    const val CALENDAR = "calendar"
    const val WIZARD = "wizard?logId={logId}&date={date}"
    const val DAY = "day/{date}"
    const val SUMMARY = "summary"
    const val SETTINGS = "settings"
    const val ARG_LOG_ID = "logId"
    const val ARG_DATE = "date"

    fun wizard(logId: Long? = null, presetDate: LocalDate? = null): String {
        val id = logId ?: -1L
        val date = presetDate?.toString().orEmpty()
        return "wizard?logId=$id&date=$date"
    }

    fun day(date: LocalDate): String = "day/$date"
}
