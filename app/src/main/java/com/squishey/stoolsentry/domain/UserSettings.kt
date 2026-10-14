package com.squishey.stoolsentry.domain

import java.time.DayOfWeek

data class UserSettings(
    val weekStart: DayOfWeek,
    val disclaimerAccepted: Boolean,
    val appLockEnabled: Boolean,
)
