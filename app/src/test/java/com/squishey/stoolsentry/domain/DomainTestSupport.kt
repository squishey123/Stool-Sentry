package com.squishey.stoolsentry.domain

import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.time.ZoneOffset
import java.time.ZonedDateTime

internal fun sampleLog(
    date: LocalDate,
    time: LocalTime = LocalTime.NOON,
    zone: ZoneId = ZoneOffset.UTC,
    type: BristolType = BristolType.TYPE_4,
    size: StoolSize = StoolSize.MEDIUM,
    color: StoolColor = StoolColor.BROWN,
    symptoms: Symptoms = Symptoms(),
    notes: String = "",
    id: Long = 0,
): BowelLog {
    val occurred = ZonedDateTime.of(date, time, zone).toInstant()
    return BowelLog(
        id = id,
        occurredAtMillis = occurred.toEpochMilli(),
        zoneId = zone.id,
        localDate = date,
        bristolType = type,
        size = size,
        color = color,
        symptoms = symptoms,
        notes = notes,
        createdAtMillis = occurred.toEpochMilli(),
        updatedAtMillis = occurred.toEpochMilli(),
    )
}

internal class FixedClock(
    private val fixedInstant: Instant,
    private val fixedZone: ZoneId,
) : AppClock {
    override fun instant(): Instant = fixedInstant

    override fun zone(): ZoneId = fixedZone
}
