package com.d1onyx.core.essentials.intents

/**
 * Common system screens/actions.
 *
 * The name is kept from the Android original for continuity, although
 * `Intent` is an Android concept — on iOS these map to `UIApplication.openURL`
 * with the appropriate settings URL.
 */
public interface IntentLauncher {

    /**
     * Open system notification settings for this app.
     */
    public fun openNotificationsSettings()

    /**
     * Open system settings of this app.
     */
    public fun openAppSettings()
}
