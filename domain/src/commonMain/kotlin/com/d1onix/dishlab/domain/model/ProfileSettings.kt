package com.d1onix.dishlab.domain.model

data class ProfileSettings(
    val displayName: String = "Alex Kim",
    val autoConnectNewProducts: Boolean = true,
    val reduceGraphMotion: Boolean = false,
    val showProductScores: Boolean = true,
) {
    val initials: String
        get() = displayName
            .trim()
            .split(Regex("\\s+"))
            .filter { it.isNotEmpty() }
            .take(2)
            .joinToString("") { it.first().uppercase() }
            .ifEmpty { "AK" }
}
