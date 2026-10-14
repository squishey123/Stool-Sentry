package com.squishey.stoolsentry.domain

import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

interface AppClock {
    fun instant(): Instant

    fun zone(): ZoneId

    fun today(): LocalDate = instant().atZone(zone()).toLocalDate()
}

class SystemAppClock : AppClock {
    override fun instant(): Instant = Instant.now()

    override fun zone(): ZoneId = ZoneId.systemDefault()
}
