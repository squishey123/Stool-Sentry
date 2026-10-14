package com.squishey.stoolsentry.domain

import java.time.LocalDate

object CalendarIndex {
    fun countsByDate(logs: List<BowelLog>): Map<LocalDate, Int> =
        logs.groupingBy { it.localDate }.eachCount()
}
