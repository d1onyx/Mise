package com.d1onyx.core.essentials.permissions

import kotlinx.coroutines.flow.Flow

/**
 * Runtime permissions the app can request.
 *
 * Deliberately free of `android.permission.*` strings — those are meaningless
 * on iOS. Each platform maps these entries to its own permission model in its
 * own source set.
 */
public enum class Permission {

    /**
     * Permission to post notifications.
     * Android: `POST_NOTIFICATIONS` (API 33+). iOS: `UNUserNotificationCenter` authorization.
     */
    PostNotifications,

    /**
     * Permission to read the photo library / external images.
     */
    ReadPhotos,

    /**
     * Permission to use the camera.
     */
    Camera,
}

/**
 * Represents the current permission state.
 */
public enum class PermissionStatus {

    /**
     * The app has access to the permission (allowed by the user).
     */
    Granted,

    /**
     * The user denied access to the permission.
     */
    Denied,

    /**
     * The user denied access permanently, so the system prompt can no longer be shown.
     */
    AlwaysDenied,
}

/**
 * An entry point for requesting and monitoring permissions.
 */
public interface PermissionRequester {

    /**
     * Observe the specific permission state.
     */
    public fun observePermission(permission: Permission): Flow<PermissionStatus>

    /**
     * Request the [permission] and await the user's choice.
     */
    public suspend fun requestPermission(permission: Permission): PermissionStatus
}
