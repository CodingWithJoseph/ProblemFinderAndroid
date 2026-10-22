package com.example.problemfinder.data.opportunities.impl.db.converters

import androidx.room3.ColumnTypeConverter
import com.example.problemfinder.data.opportunities.impl.model.EvidenceSection

object SectionConverter {
    @ColumnTypeConverter
    fun fromSection(section: EvidenceSection): String = section.storedValue

    @ColumnTypeConverter
    fun toSection(value: String): EvidenceSection = when(value) {
        EvidenceSection.POSTS.storedValue -> EvidenceSection.POSTS
        EvidenceSection.TOP.storedValue -> EvidenceSection.TOP
        else -> throw IllegalArgumentException()
    }
}
