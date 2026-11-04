package com.example.problemfinder.core.common.util

import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeParseException
import kotlin.time.Instant


@Suppress("CheckResult")
fun String.isValidDate(): Boolean {
    return try {
        LocalDate.parse(this, DateTimeFormatter.ISO_LOCAL_DATE);
        true
    } catch (error: DateTimeParseException) {
        print(error.message)
        false
    }
}

@Suppress("CheckResult")
fun String.toDateInstant(): Instant = Instant.parse(this)
