package com.d1onyx.core.datastore

import android.content.Context

/**
 * The path of the app's preferences file in internal storage.
 *
 * ```
 * val dataStore = createPreferencesDataStore { context.preferencesDataStorePath() }
 * ```
 */
public fun Context.preferencesDataStorePath(
    fileName: String = DEFAULT_DATA_STORE_FILE_NAME,
): String = filesDir.resolve(fileName).absolutePath
