package com.dishlab.api.middleware

import io.ktor.server.application.ApplicationCall
import io.ktor.util.AttributeKey
import java.util.UUID

const val RequestIdHeader = "X-Request-Id"
const val RequestIdAttributeName = "dishlab.requestId"
val RequestIdKey = AttributeKey<String>(RequestIdAttributeName)

fun ApplicationCall.requestId(): String = attributes.getOrNull(RequestIdKey) ?: "unknown"

fun newRequestId(): String = UUID.randomUUID().toString()
