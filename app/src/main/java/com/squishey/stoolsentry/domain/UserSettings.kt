package com.squishey.stoolsentry.domain

import java.time.DayOfWeek

data class UserSettings(
    val weekStart: DayOfWeek,
    val disclaimerAccepted: Boolean,
    val appLockEnabled: Boolean,
    val autoExportEnabled: Boolean = false,
    val autoExportTreeUri: String? = null,
    val autoExportFolderName: String? = null,
    val encryptionOfferSeen: Boolean = false,
)
