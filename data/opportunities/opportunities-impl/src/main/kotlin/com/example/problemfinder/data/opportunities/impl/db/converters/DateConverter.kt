package com.example.problemfinder.data.opportunities.impl.db.converters

import androidx.room3.ColumnTypeConverter
import kotlin.time.Instant

object DateConverter {
    @ColumnTypeConverter
    fun fromTimestampInstant(value: String?): Instant? {
        return value?.let { Instant.parse(value) }
    }

    @ColumnTypeConverter
    fun toTimestampInstant(instant: Instant?): String? {
        return instant?.toString()
    }
}
