package com.example.problemfinder.feature.browse.impl.browse

import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.util.Locale
import kotlin.time.Instant

internal fun Instant.toBrowseDateLabel(locale: Locale, zone: ZoneId): String {
    val javaInstant = java.time.Instant.ofEpochSecond(
        epochSeconds,
        nanosecondsOfSecond.toLong(),
    )
    return DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM)
        .withLocale(locale)
        .format(javaInstant.atZone(zone))
}
