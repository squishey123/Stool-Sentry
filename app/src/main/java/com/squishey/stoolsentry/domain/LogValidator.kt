package com.squishey.stoolsentry.domain

object LogValidator {
    const val MAX_NOTES = 2000

    fun isFuture(occurredAtMillis: Long, nowMillis: Long): Boolean = occurredAtMillis > nowMillis
}
