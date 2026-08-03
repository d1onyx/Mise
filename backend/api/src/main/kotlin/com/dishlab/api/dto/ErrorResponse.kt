package com.dishlab.api.dto

import kotlinx.serialization.Serializable

@Serializable
data class ErrorResponse(
    val error: ApiError,
)

@Serializable
data class ApiError(
    val code: String,
    val message: String,
    val details: Map<String, String> = emptyMap(),
    val traceId: String,
)

@Serializable
data class HealthResponse(
    val status: String,
)

@Serializable
data class AuthDebugResponse(
    val uid: String,
)
