package com.squishey.stoolsentry.domain

import java.time.LocalDate
import java.time.YearMonth

data class MonthTally(
    val logCount: Int,
    val loggedDays: Int,
)

object CalendarIndex {
    fun countsByDate(logs: List<BowelLog>): Map<LocalDate, Int> =
        logs.groupingBy { it.localDate }.eachCount()

    fun latest(logs: List<BowelLog>): BowelLog? = logs.maxByOrNull { it.occurredAtMillis }

    fun monthTally(countsByDate: Map<LocalDate, Int>, month: YearMonth): MonthTally {
        val inMonth = countsByDate.filterKeys { YearMonth.from(it) == month }
        return MonthTally(
            logCount = inMonth.values.sum(),
            loggedDays = inMonth.count { it.value > 0 },
        )
    }
}
