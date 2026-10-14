package com.squishey.stoolsentry.domain

import java.time.LocalDate
import java.time.LocalTime
import java.time.YearMonth
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class CalendarIndexTest {
    @Test
    fun countsLogsOnTheSameLocalDate() {
        val day = LocalDate.of(2026, 10, 14)
        val logs = listOf(
            sampleLog(day, LocalTime.of(8, 0)),
            sampleLog(day, LocalTime.of(18, 0)),
            sampleLog(day.minusDays(1)),
        )
        val counts = CalendarIndex.countsByDate(logs)
        assertEquals(2, counts[day])
        assertEquals(1, counts[day.minusDays(1)])
    }

    @Test
    fun latestPicksTheMostRecentMomentRegardlessOfListOrder() {
        val day = LocalDate.of(2026, 10, 14)
        val evening = sampleLog(day, LocalTime.of(21, 30), id = 2)
        val logs = listOf(
            sampleLog(day.minusDays(3), id = 1),
            evening,
            sampleLog(day, LocalTime.of(7, 15), id = 3),
        )
        assertEquals(evening, CalendarIndex.latest(logs))
    }

    @Test
    fun latestIsNullWithoutLogs() {
        assertNull(CalendarIndex.latest(emptyList()))
    }

    @Test
    fun monthTallyCountsOnlyTheRequestedMonth() {
        val logs = listOf(
            sampleLog(LocalDate.of(2026, 9, 30)),
            sampleLog(LocalDate.of(2026, 10, 1), LocalTime.of(8, 0)),
            sampleLog(LocalDate.of(2026, 10, 1), LocalTime.of(19, 0)),
            sampleLog(LocalDate.of(2026, 10, 14)),
            sampleLog(LocalDate.of(2026, 11, 1)),
        )
        val tally = CalendarIndex.monthTally(CalendarIndex.countsByDate(logs), YearMonth.of(2026, 10))
        assertEquals(MonthTally(logCount = 3, loggedDays = 2), tally)
    }

    @Test
    fun monthTallyIsZeroForAnEmptyMonth() {
        val tally = CalendarIndex.monthTally(emptyMap(), YearMonth.of(2026, 10))
        assertEquals(MonthTally(logCount = 0, loggedDays = 0), tally)
    }
}
