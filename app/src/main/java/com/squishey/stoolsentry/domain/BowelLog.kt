package com.squishey.stoolsentry.domain

import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId

data class BowelLog(
    val id: Long = 0,
    val occurredAtMillis: Long,
    val zoneId: String,
    val localDate: LocalDate,
    val bristolType: BristolType,
    val size: StoolSize,
    val color: StoolColor,
    val symptoms: Symptoms,
    val notes: String,
    val createdAtMillis: Long,
    val updatedAtMillis: Long,
) {
    fun localTime(): LocalTime =
        Instant.ofEpochMilli(occurredAtMillis).atZone(ZoneId.of(zoneId)).toLocalTime()

    fun timeOfDay(): TimeOfDay = TimeOfDay.fromHour(localTime().hour)
}
