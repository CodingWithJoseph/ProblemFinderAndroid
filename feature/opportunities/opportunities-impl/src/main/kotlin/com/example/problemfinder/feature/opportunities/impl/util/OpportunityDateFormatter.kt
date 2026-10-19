package com.example.problemfinder.feature.opportunities.impl.util

import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.util.Locale
import kotlin.time.Instant

/** Formats the instant in the supplied local zone; its displayed calendar date may differ from UTC. */
internal fun Instant.toOpportunityDateLabel(locale: Locale, zone: ZoneId): String {
    val javaInstant = java.time.Instant.ofEpochSecond(
        epochSeconds,
        nanosecondsOfSecond.toLong(),
    )
    return DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM)
        .withLocale(locale)
        .format(javaInstant.atZone(zone))
}

/** Uses the localized medium date up to its first comma; locales without commas keep the full label. */
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
