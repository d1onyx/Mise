package com.dishlab.domain.model

import java.time.Instant
import java.util.UUID

data class UserAccount(
    val id: UUID,
    val firebaseUid: String,
    val profile: UserProfile? = null,
    val allergens: Set<String> = emptySet(),
    val diets: Set<String> = emptySet(),
    val equipment: Set<String> = emptySet(),
    val goals: Set<String> = emptySet(),
    val settings: UserSettings = UserSettings(),
    val consents: List<UserConsent> = emptyList(),
    val deleted: Boolean = false,
    val createdAt: Instant = Instant.now(),
    val updatedAt: Instant = createdAt,
)

data class UserProfile(
    val displayName: String,
    val locale: String = "uk-UA",
    val timezone: String? = null,
)

data class UserSettings(
    val language: String = "uk-UA",
    val units: String = "metric",
    val notificationsEnabled: Boolean = true,
)

data class UserConsent(
    val type: String,
    val granted: Boolean,
    val decidedAt: Instant = Instant.now(),
)
