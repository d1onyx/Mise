package com.d1onyx.core.essentials.entities

import kotlin.jvm.JvmInline

/**
 * HTTP Code of any backend response (e.g. 200, 404, etc.)
 */
@JvmInline
public value class HttpCode(public val value: Int) {
    override fun toString(): String = value.toString()
}

/**
 * Server error code of any backend response.
 */
@JvmInline
public value class ServerCode(public val value: String) {
    override fun toString(): String = value
}
