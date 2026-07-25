package com.d1onyx.core.essentials.dialogs

/**
 * Configuration of an alert dialog displayed by [Dialogs.showAlertDialog].
 */
public interface DialogConfig {

    /** Title of the dialog. */
    public val title: String

    /** Text content — usually a question or important information. */
    public val message: String

    /** Label of the positive action button (e.g. Ok, Confirm). */
    public val positiveButton: String

    /**
     * Optional label of the negative action button. The button is hidden
     * for `null` or blank values.
     */
    public val negativeButton: String?

    public data class Default(
        override val title: String,
        override val message: String,
        override val positiveButton: String,
        override val negativeButton: String? = null,
    ) : DialogConfig
}

/**
 * A common shared interface for launching dialogs and getting results.
 */
public interface Dialogs {

    /**
     * Display an alert dialog configured by [DialogConfig]. The user's choice
     * is returned as a boolean, either confirmed (true) or dismissed (false).
     */
    public suspend fun showAlertDialog(config: DialogConfig): Boolean
}

/**
 * The Toaster can be injected anywhere in the app to display toast messages.
 */
public interface Toaster {

    /**
     * Show a short toast message to the user.
     */
    public fun toast(message: String)
}
