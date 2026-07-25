package com.d1onyx.core.network.error

import kotlinx.serialization.Serializable

/**
 * The shape of an error response body.
 */
@Serializable
public data class ErrorDto(
    val errcode: String? = null,
    val error: String? = null,
)
