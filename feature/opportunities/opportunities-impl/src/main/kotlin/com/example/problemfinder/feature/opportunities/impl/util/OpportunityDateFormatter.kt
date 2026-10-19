package com.example.problemfinder.feature.opportunities.impl.util

import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.util.Locale
import kotlin.time.Instant

internal fun Instant.toOpportunityDateLabel(locale: Locale, zone: ZoneId): String {
    val javaInstant = java.time.Instant.ofEpochSecond(
        epochSeconds,
        nanosecondsOfSecond.toLong(),
    )
    return DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM)
        .withLocale(locale)
        .format(javaInstant.atZone(zone))
}

internal fun Instant.toOpportunityDateLabelCompact(locale: Locale, zone: ZoneId): String {
    val javaInstant = java.time.Instant.ofEpochSecond(
        epochSeconds,
        nanosecondsOfSecond.toLong(),
    )
    return DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM)
        .withLocale(locale)
        .format(javaInstant.atZone(zone))
        .substringBefore(',')
}
