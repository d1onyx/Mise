package com.d1onyx.core.essentials.logger

/**
 * The native logging facility of the current platform:
 * LogCat on Android, `NSLog` on iOS.
 *
 * Install it on app start:
 *
 * ```
 * Logger.install(DefaultLogger(platformLogSink()))
 * ```
 *
 * For release builds, wrap it so noise never reaches production:
 *
 * ```
 * Logger.install(
 *     DefaultLogger(
 *         if (isDebug) platformLogSink()
 *         else MinLevelLogSink(crashReporterSink, LogLevel.Warn)
 *     )
 * )
 * ```
 */
public expect fun platformLogSink(): LogSink
