package com.d1onyx.core.essentials.di

/**
 * Aggregation scope marker for the application process lifecycle.
 *
 * Used both to aggregate contributions — `@ContributesTo(AppScope::class)`,
 * `@ContributesBinding(AppScope::class)` — and to scope bindings via
 * `@SingleIn(AppScope::class)`.
 *
 * It is an abstract class rather than a `@Scope` annotation on purpose: Metro
 * expects aggregation scopes to be marker types, and warns when given a
 * concrete scope annotation instead.
 *
 * Bindings scoped to it live as long as the app: the HTTP client, the database,
 * the logger, settings storage.
 */
public abstract class AppScope private constructor()

/**
 * Aggregation scope marker for an authenticated session.
 *
 * Use it through a `@GraphExtension` of the app graph, so that everything tied
 * to the current user is discarded on logout by dropping the extension —
 * rather than by remembering to clear each cache by hand.
 */
public abstract class SessionScope private constructor()
