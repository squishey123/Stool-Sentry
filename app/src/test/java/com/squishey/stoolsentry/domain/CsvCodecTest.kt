package com.squishey.stoolsentry.domain

import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneOffset
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class CsvCodecTest {
    private val now = Instant.parse("2026-10-14T12:00:00Z")

    @Test
    fun roundTripKeepsQuotedNotes() {
        val log = sampleLog(
            date = LocalDate.of(2026, 10, 1),
            time = LocalTime.of(8, 15),
            notes = "said \"hi\", then left",
            symptoms = Symptoms(straining = true),
        )
        val decoded = CsvCodec.decode(CsvCodec.encode(listOf(log)), now)
        assertEquals(0, decoded.skipped)
        assertEquals(1, decoded.logs.size)
        assertEquals(log.notes, decoded.logs.single().notes)
        assertEquals(log.localDate, decoded.logs.single().localDate)
        assertEquals(LocalTime.of(8, 15), decoded.logs.single().localTime())
        assertTrue(decoded.logs.single().symptoms.straining)
        assertEquals(BristolType.TYPE_4, decoded.logs.single().bristolType)
    }

    @Test
    fun rowsMissingARequiredFieldAreSkipped() {
        val csv = """
            local_date,local_time,zone,type,size,color,urgent,blood,painful,straining,incomplete,mucus,notes
            2026-10-01,08:15:00,UTC,4,MEDIUM,BROWN,false,false,false,false,false,false,ok
            2026-10-02,09:00:00,UTC,,MEDIUM,BROWN,false,false,false,false,false,false,
            2026-10-03,09:00:00,UTC,4,,BROWN,false,false,false,false,false,false,
            2026-10-04,09:00:00,UTC,4,MEDIUM,,false,false,false,false,false,false,
        """.trimIndent()
        val decoded = CsvCodec.decode(csv, now)
        assertEquals(1, decoded.logs.size)
        assertEquals(3, decoded.skipped)
        assertEquals(LocalDate.of(2026, 10, 1), decoded.logs.single().localDate)
    }

    @Test
    fun futureRowsAreSkipped() {
        val csv = """
            local_date,local_time,zone,type,size,color,urgent,blood,painful,straining,incomplete,mucus,notes
            2026-10-15,09:00:00,UTC,4,MEDIUM,BROWN,false,false,false,false,false,false,
        """.trimIndent()
        val decoded = CsvCodec.decode(csv, now)
        assertTrue(decoded.logs.isEmpty())
        assertEquals(1, decoded.skipped)
    }

    @Test
    fun emptyFileImportsNothing() {
        val decoded = CsvCodec.decode("", now)
        assertEquals(0, decoded.logs.size)
        assertEquals(0, decoded.skipped)
    }
}
