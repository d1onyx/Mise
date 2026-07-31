package com.d1onix.dishlab

import com.d1onix.dishlab.data.storage.StoredUserSessionRepository
import com.d1onix.dishlab.domain.repository.UserSessionRepository
import com.d1onyx.core.essentials.di.AppScope
import dev.zacsweers.metro.ContributesBinding
import dev.zacsweers.metro.Inject
import dev.zacsweers.metro.SingleIn

/** iOS retains the local session until Firebase is configured for the iOS app. */
@ContributesBinding(AppScope::class)
@SingleIn(AppScope::class)
@Inject
class IosStoredUserSessionRepository(
    private val delegate: StoredUserSessionRepository,
) : UserSessionRepository by delegate
