package com.d1onix.dishlab.domain.analytics

import kotlin.test.Test
import kotlin.test.assertEquals

class AnalyticsTrackerTest {
    @Test
    fun `MVP events use the Firebase event names`() {
        assertEquals(
            listOf("session_start", "scan_attempted", "scan_resolved", "scan_failed", "recipe_opened"),
            AnalyticsEvent.entries.map(AnalyticsEvent::firebaseName),
        )
    }

    @Test
    fun `no-op tracker accepts every event`() {
        AnalyticsEvent.entries.forEach(NoOpAnalyticsTracker::track)
    }
}
