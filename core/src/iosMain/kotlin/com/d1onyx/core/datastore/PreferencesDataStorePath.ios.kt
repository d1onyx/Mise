@file:OptIn(kotlinx.cinterop.ExperimentalForeignApi::class)

package com.d1onyx.core.datastore

import platform.Foundation.NSDocumentDirectory
import platform.Foundation.NSFileManager
import platform.Foundation.NSURL
import platform.Foundation.NSUserDomainMask

/**
 * The path of the app's preferences file in the documents directory.
 *
 * ```
 * val dataStore = createPreferencesDataStore { preferencesDataStorePath() }
 * ```
 */
public fun preferencesDataStorePath(
    fileName: String = DEFAULT_DATA_STORE_FILE_NAME,
): String {
    val documentDirectory: NSURL? = NSFileManager.defaultManager.URLForDirectory(
        directory = NSDocumentDirectory,
        inDomain = NSUserDomainMask,
        appropriateForURL = null,
        create = false,
        error = null,
    )
    val basePath = requireNotNull(documentDirectory?.path) {
        "Unable to resolve the iOS documents directory for the preferences store"
    }
    return "$basePath/$fileName"
}
