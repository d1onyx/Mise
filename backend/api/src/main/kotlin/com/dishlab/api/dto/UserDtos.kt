package com.dishlab.api.dto

import com.dishlab.domain.model.UserAccount
import com.dishlab.domain.model.UserConsent
import com.dishlab.domain.model.UserProfile
import com.dishlab.domain.model.UserSettings
import kotlinx.serialization.Serializable

@Serializable
data class UserResponse(
    val id: String,
    val firebaseUid: String,
    val profile: UserProfileResponse?,
    val diets: Set<String>,
    val allergens: Set<String>,
    val equipment: Set<String>,
    val goals: Set<String>,
    val settings: UserSettingsResponse,
    val consents: List<UserConsentResponse>,
    val deleted: Boolean,
)

@Serializable
data class UserProfileResponse(
    val displayName: String,
    val locale: String,
    val timezone: String? = null,
)

@Serializable
data class UserSettingsResponse(
    val language: String,
    val units: String,
    val notificationsEnabled: Boolean,
)

@Serializable
data class UserConsentResponse(
    val type: String,
    val granted: Boolean,
)

@Serializable
data class UpdateProfileRequest(
    val displayName: String,
    val locale: String = "uk-UA",
    val timezone: String? = null,
)

@Serializable
data class UpdateAllergensRequest(
    val allergens: Set<String>,
)
@Serializable
data class UpdateDietsRequest(
    val diets: Set<String>,
)

@Serializable
data class UpdateEquipmentRequest(
    val equipment: Set<String>,
)

@Serializable
data class UpdateGoalsRequest(
    val goals: Set<String>,
)

@Serializable
data class UpdateSettingsRequest(
    val language: String = "uk-UA",
    val units: String = "metric",
    val notificationsEnabled: Boolean = true,
)

@Serializable
data class SetConsentRequest(
    val type: String,
    val granted: Boolean,
)

fun UserAccount.toResponse(): UserResponse = UserResponse(
    id = id.toString(),
    firebaseUid = firebaseUid,
    profile = profile?.toResponse(),
    allergens = allergens,
    diets = diets,
    equipment = equipment,
    goals = goals,
    settings = settings.toResponse(),
    consents = consents.map { it.toResponse() },
    deleted = deleted,
)

fun UserProfile.toResponse(): UserProfileResponse = UserProfileResponse(
    displayName = displayName,
    locale = locale,
    timezone = timezone,
)

fun UserSettings.toResponse(): UserSettingsResponse = UserSettingsResponse(
    language = language,
    units = units,
    notificationsEnabled = notificationsEnabled,
)

fun UserConsent.toResponse(): UserConsentResponse = UserConsentResponse(
    type = type,
    granted = granted,
)

fun UpdateProfileRequest.toDomain(): UserProfile = UserProfile(
    displayName = displayName,
    locale = locale,
    timezone = timezone,
)

fun UpdateSettingsRequest.toDomain(): UserSettings = UserSettings(
    language = language,
    units = units,
    notificationsEnabled = notificationsEnabled,
)

fun SetConsentRequest.toDomain(): UserConsent = UserConsent(
    type = type,
    granted = granted,
)
