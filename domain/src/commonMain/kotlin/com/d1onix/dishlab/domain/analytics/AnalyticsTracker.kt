package com.d1onix.dishlab.domain.analytics

/** Records product metrics without coupling feature code to an analytics SDK. */
interface AnalyticsTracker {
    fun track(event: AnalyticsEvent)
}

/** Events defined for the MVP product funnel. */
enum class AnalyticsEvent(val firebaseName: String) {
    SessionStarted("session_start"),
    ScanAttempted("scan_attempted"),
    ScanResolved("scan_resolved"),
    ScanFailed("scan_failed"),
    RecipeOpened("recipe_opened"),
}

/** Safe default for previews, tests, and platforms without Firebase Analytics. */
object NoOpAnalyticsTracker : AnalyticsTracker {
    override fun track(event: AnalyticsEvent) = Unit
}
