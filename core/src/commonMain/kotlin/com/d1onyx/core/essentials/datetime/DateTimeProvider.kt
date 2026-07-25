package com.d1onyx.core.essentials.datetime

import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import kotlin.concurrent.Volatile
import kotlin.time.Clock
import kotlin.time.Instant

/**
 * A repository for working with date and time.
 *
 * Returns [kotlin.time.Instant] from the standard library instead of the original
 * `java.time.ZonedDateTime`, which does not exist on Kotlin/Native.
 * Pair it with [timeZone] when a wall-clock representation is needed.
 */
public interface DateTimeProvider {

    /**
     * Get the current moment in time.
     */
    public fun now(): Instant

    /**
     * The time zone used for presenting timestamps to the user.
     */
    public fun timeZone(): TimeZone

    /**
     * Get the current timestamp in milliseconds since the Unix epoch.
     */
    public fun currentTimeMillis(): Long

    /**
     * The globally installed [DateTimeProvider].
     *
     * Defaults to [SystemDateTimeProvider], so date/time works out of the box;
     * tests replace it with a fixed-clock implementation via [install].
     */
    public companion object : DateTimeProvider {

        @Volatile
        private var instance: DateTimeProvider = SystemDateTimeProvider

        override fun now(): Instant = instance.now()
        override fun timeZone(): TimeZone = instance.timeZone()
        override fun currentTimeMillis(): Long = instance.currentTimeMillis()

        public fun install(provider: DateTimeProvider) {
            instance = provider
        }

        public fun reset() {
            instance = SystemDateTimeProvider
        }
    }
}

/**
 * The default provider, backed by the system clock and the device time zone.
 */
public object SystemDateTimeProvider : DateTimeProvider {
    override fun now(): Instant = Clock.System.now()
    override fun timeZone(): TimeZone = TimeZone.currentSystemDefault()
    override fun currentTimeMillis(): Long = Clock.System.now().toEpochMilliseconds()
}

/**
 * A provider frozen at [fixedInstant] — for deterministic tests.
 */
public class FixedDateTimeProvider(
    private val fixedInstant: Instant,
    private val zone: TimeZone = TimeZone.UTC,
) : DateTimeProvider {
    override fun now(): Instant = fixedInstant
    override fun timeZone(): TimeZone = zone
    override fun currentTimeMillis(): Long = fixedInstant.toEpochMilliseconds()
}

/**
 * Convert an [Instant] to wall-clock time in the currently installed zone.
 */
public fun Instant.toLocalDateTimeInAppZone(): LocalDateTime =
    toLocalDateTime(DateTimeProvider.timeZone())
