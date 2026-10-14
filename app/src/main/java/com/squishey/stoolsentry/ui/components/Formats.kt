package com.squishey.stoolsentry.ui.components

import android.text.format.DateFormat
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import java.time.format.DateTimeFormatter
import java.util.Locale

@Composable
fun currentLocale(): Locale = LocalConfiguration.current.locales.get(0) ?: Locale.US

/** Formatter for a CLDR skeleton such as "EEEMMMd", localized for the device. */
@Composable
fun rememberSkeletonFormatter(skeleton: String): DateTimeFormatter {
    val locale = currentLocale()
    return remember(locale, skeleton) {
        DateTimeFormatter.ofPattern(DateFormat.getBestDateTimePattern(locale, skeleton), locale)
    }
}

/** Time formatter that follows the device's 12 or 24 hour setting. */
@Composable
fun rememberTimeFormatter(): DateTimeFormatter {
    val is24Hour = DateFormat.is24HourFormat(LocalContext.current)
    return rememberSkeletonFormatter(if (is24Hour) "Hm" else "hm")
}
