package com.example.problemfinder.feature.opportunities.impl.browse

import com.example.problemfinder.feature.opportunities.impl.util.toOpportunityDateLabel
import java.time.ZoneId
import java.util.Locale
import kotlin.time.Instant
import org.junit.Assert.assertEquals
import org.junit.Test

class OpportunityDateFormatterTest {
    @Test
    fun formatsARealInstantAsALocalizedDate() {
        assertEquals("Oct 10, 2026", Instant.parse("2026-10-10T12:00:00.123456789Z").toOpportunityDateLabel(Locale.US, ZoneId.of("UTC")))
    }

    @Test
    fun westernTimeZoneCanShowThePreviousDay() {
        assertEquals("Oct 9, 2026", Instant.parse("2026-10-10T01:00:00Z").toOpportunityDateLabel(Locale.US, ZoneId.of("America/Los_Angeles")))
    }

    @Test
    fun easternTimeZoneCanShowTheFollowingDay() {
        assertEquals("Oct 11, 2026", Instant.parse("2026-10-10T23:00:00Z").toOpportunityDateLabel(Locale.US, ZoneId.of("Asia/Tokyo")))
    }

    @Test
    fun usesTheSelectedLocaleRatherThanHardcodedEnglish() {
        assertEquals("10 oct. 2026", Instant.parse("2026-10-10T12:00:00Z").toOpportunityDateLabel(Locale.FRANCE, ZoneId.of("UTC")))
    }
}
