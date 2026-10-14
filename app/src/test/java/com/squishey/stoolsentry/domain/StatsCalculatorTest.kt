package com.squishey.stoolsentry.domain

import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneOffset
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class StatsCalculatorTest {
    private val today = LocalDate.of(2026, 10, 14)
    private val zone = ZoneOffset.UTC

    @Test
    fun emptyHistoryHasNoGapsOrClinicianNote() {
        val snapshot = StatsCalculator.summarize(emptyList(), SummaryPeriod.DAYS_7, today)
        assertTrue(snapshot.isEmpty)
        assertEquals(0, snapshot.logCount)
        assertEquals(0.0, snapshot.averagePerDay, 0.0001)
        assertNull(snapshot.daysSinceLast)
        assertNull(snapshot.longestGapDays)
        assertFalse(snapshot.showClinicianNote)
        assertEquals(0, snapshot.bristol.sumOf { it.count })
    }

    @Test
    fun sevenDayWindowIncludesTodayAndThePreviousSixDays() {
        val included = listOf(
            sampleLog(today.minusDays(6), zone = zone),
            sampleLog(today, zone = zone),
        )
        val excluded = sampleLog(today.minusDays(7), zone = zone)
        val snapshot = StatsCalculator.summarize(included + excluded, SummaryPeriod.DAYS_7, today)
        assertEquals(2, snapshot.logCount)
        assertEquals(7, snapshot.dayCount)
        assertEquals(2.0 / 7.0, snapshot.averagePerDay, 0.0001)
        assertEquals(0, snapshot.daysSinceLast)
    }

    @Test
    fun thirtyAndNinetyDayWindowsStopAtTheirBoundaries() {
        val logs = listOf(
            sampleLog(today.minusDays(29), zone = zone),
            sampleLog(today.minusDays(30), zone = zone),
            sampleLog(today.minusDays(89), zone = zone),
            sampleLog(today.minusDays(90), zone = zone),
        )
        val thirty = StatsCalculator.summarize(logs, SummaryPeriod.DAYS_30, today)
        val ninety = StatsCalculator.summarize(logs, SummaryPeriod.DAYS_90, today)
        assertEquals(1, thirty.logCount)
        assertEquals(3, ninety.logCount)
        assertEquals(90, ninety.dayCount)
    }

    @Test
    fun allTimeAverageIncludesDaysWithNoLogs() {
        val logs = listOf(sampleLog(LocalDate.of(2026, 10, 4), zone = zone))
        val snapshot = StatsCalculator.summarize(logs, SummaryPeriod.ALL, today)
        assertEquals(11, snapshot.dayCount)
        assertEquals(1.0 / 11.0, snapshot.averagePerDay, 0.0001)
        assertEquals(10, snapshot.daysSinceLast)
        assertNull(snapshot.longestGapDays)
    }

    @Test
    fun longestGapIsTheLargestDistanceBetweenLoggedDates() {
        val logs = listOf(
            sampleLog(LocalDate.of(2026, 10, 1), zone = zone),
            sampleLog(LocalDate.of(2026, 10, 1), time = LocalTime.of(18, 0), zone = zone),
            sampleLog(LocalDate.of(2026, 10, 4), zone = zone),
            sampleLog(LocalDate.of(2026, 10, 10), zone = zone),
        )
        val snapshot = StatsCalculator.summarize(logs, SummaryPeriod.DAYS_30, today)
        assertEquals(6, snapshot.longestGapDays)
        assertEquals(4, snapshot.logCount)
    }

    @Test
    fun tiesPreferTheLowerBristolNumberThenAlphabeticalLabels() {
        val logs = listOf(
            sampleLog(today, type = BristolType.TYPE_7, color = StoolColor.RED, zone = zone),
            sampleLog(today.minusDays(1), type = BristolType.TYPE_1, color = StoolColor.BROWN, zone = zone),
        )
        val snapshot = StatsCalculator.summarize(logs, SummaryPeriod.DAYS_7, today)
        assertEquals(listOf("1", "7"), snapshot.bristol.filter { it.count > 0 }.map { it.key })
        assertEquals(listOf("Brown", "Red"), snapshot.colors.filter { it.count > 0 }.map { it.label })
    }

    @Test
    fun symptomRatesUseLogsAndTheClinicianNoteStaysInsideThePeriod() {
        val logs = listOf(
            sampleLog(today, symptoms = Symptoms(urgent = true), zone = zone),
            sampleLog(today.minusDays(1), symptoms = Symptoms(urgent = true, blood = true), zone = zone),
            sampleLog(today.minusDays(2), zone = zone),
            sampleLog(today.minusDays(3), zone = zone),
            sampleLog(today.minusDays(10), color = StoolColor.BLACK, zone = zone),
        )
        val snapshot = StatsCalculator.summarize(logs, SummaryPeriod.DAYS_7, today)
        assertEquals(0.5, snapshot.symptomRates.urgent, 0.0001)
        assertEquals(0.25, snapshot.symptomRates.blood, 0.0001)
        assertTrue(snapshot.showClinicianNote)
        val olderOnly = StatsCalculator.summarize(
            listOf(sampleLog(today.minusDays(10), color = StoolColor.BLACK, zone = zone)),
            SummaryPeriod.DAYS_7,
            today,
        )
        assertFalse(olderOnly.showClinicianNote)
        assertEquals(10, olderOnly.daysSinceLast)
    }

    @Test
    fun timeOfDayUsesTheStoredZoneBoundaries() {
        val times = listOf(
            LocalTime.of(4, 59) to TimeOfDay.NIGHT,
            LocalTime.of(5, 0) to TimeOfDay.MORNING,
            LocalTime.of(10, 59) to TimeOfDay.MORNING,
            LocalTime.of(11, 0) to TimeOfDay.AFTERNOON,
            LocalTime.of(16, 59) to TimeOfDay.AFTERNOON,
            LocalTime.of(17, 0) to TimeOfDay.EVENING,
            LocalTime.of(22, 59) to TimeOfDay.EVENING,
            LocalTime.of(23, 0) to TimeOfDay.NIGHT,
            LocalTime.of(0, 30) to TimeOfDay.NIGHT,
        )
        val logs = times.map { (time, _) -> sampleLog(today, time = time, zone = zone) }
        logs.zip(times).forEach { (log, expected) ->
            assertEquals(expected.second, log.timeOfDay())
        }
        val shares = StatsCalculator.summarize(logs, SummaryPeriod.DAYS_7, today).timeOfDay
        assertEquals(3, shares.first { it.key == TimeOfDay.NIGHT.name }.count)
        assertEquals(2, shares.first { it.key == TimeOfDay.MORNING.name }.count)
        assertEquals(2, shares.first { it.key == TimeOfDay.AFTERNOON.name }.count)
        assertEquals(2, shares.first { it.key == TimeOfDay.EVENING.name }.count)
    }
}
