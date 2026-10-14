package com.squishey.stoolsentry.domain

data class Symptoms(
    val urgent: Boolean = false,
    val blood: Boolean = false,
    val painful: Boolean = false,
    val straining: Boolean = false,
    val incomplete: Boolean = false,
    val mucus: Boolean = false,
)
