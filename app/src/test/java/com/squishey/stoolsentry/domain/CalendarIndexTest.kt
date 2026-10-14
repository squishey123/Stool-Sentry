package com.squishey.stoolsentry.domain

import java.time.LocalDate
import java.time.LocalTime
import org.junit.Assert.assertEquals
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
}
