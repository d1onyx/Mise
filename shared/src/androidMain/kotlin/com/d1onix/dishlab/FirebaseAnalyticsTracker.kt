package com.d1onix.dishlab

import android.content.Context
import com.d1onix.dishlab.domain.analytics.AnalyticsEvent
import com.d1onix.dishlab.domain.analytics.AnalyticsTracker
import com.google.firebase.analytics.FirebaseAnalytics

/** Android [AnalyticsTracker] backed by Firebase Analytics. */
class FirebaseAnalyticsTracker(context: Context) : AnalyticsTracker {
    private val firebaseAnalytics = FirebaseAnalytics.getInstance(context)

    override fun track(event: AnalyticsEvent) {
        firebaseAnalytics.logEvent(event.firebaseName, null)
    }
}
