package com.d1onyx.core.essentials.entities

/**
 * Universal source of all images, consumed by the shared `ImageView` component.
 *
 * Unlike the Android-only original, [Local] carries a platform-neutral [key]
 * instead of an `Int` resource id: `R` does not exist on iOS, and Compose
 * Multiplatform addresses its own resources through generated `Res` accessors.
 * The UI layer maps the key to whatever its platform actually needs.
 */
public sealed interface ImageSource {

    /**
     * Empty image (e.g. image is not set or does not exist).
     */
    public data object Empty : ImageSource

    /**
     * Remote image source that can be downloaded via the specified [url].
     */
    public data class Remote(
        val url: String,
    ) : ImageSource

    /**
     * Image bundled with the app, addressed by a platform-neutral [key].
     */
    public data class Local(
        val key: String,
    ) : ImageSource
}
