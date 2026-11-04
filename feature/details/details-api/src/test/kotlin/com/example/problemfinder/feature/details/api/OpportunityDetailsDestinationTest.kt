package com.example.problemfinder.feature.details.api

import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test

class OpportunityDetailsDestinationTest {
    @Test fun `route keeps the selected opportunity ID`() {
        assertEquals("opportunity_details/42", OpportunityDetailsDestination.route(42))
        assertEquals("opportunity_details/{clusterId}", OpportunityDetailsDestination.ROUTE)
    }
    @Test fun `invalid ID cannot build a detail route`() {
        for (id in listOf(0, -1)) assertThrows(IllegalArgumentException::class.java) { OpportunityDetailsDestination.route(id) }
    }
}
