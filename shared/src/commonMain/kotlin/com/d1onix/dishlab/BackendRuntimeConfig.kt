package com.d1onix.dishlab

/** Values supplied by each platform composition root for the Ktor API client. */
data class BackendRuntimeConfig(
    val baseUrl: String,
    val isDebug: Boolean,
    /** Present only for a local Ktor server started with `DEV_AUTH=true`. */
    val developmentToken: String? = null,
)
