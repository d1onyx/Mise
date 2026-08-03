package com.dishlab.domain.policy

import com.dishlab.domain.error.ValidationError
import com.dishlab.domain.model.UserProfile

object UserProfileValidator {
    fun validate(profile: UserProfile) {
        val displayNameLength = profile.displayName.trim().length
        if (displayNameLength !in 2..80) {
            throw ValidationError(
                details = mapOf(
                    "field" to "displayName",
                    "reason" to "length_must_be_2_80",
                ),
            )
        }

        if (profile.locale.isBlank()) {
            throw ValidationError(
                details = mapOf(
                    "field" to "locale",
                    "reason" to "required",
                ),
            )
        }
    }
}
