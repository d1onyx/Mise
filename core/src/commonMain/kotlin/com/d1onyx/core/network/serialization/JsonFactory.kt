@file:OptIn(ExperimentalSerializationApi::class)

package com.d1onyx.core.network.serialization

import com.d1onyx.core.essentials.entities.EventId
import com.d1onyx.core.essentials.entities.Id
import com.d1onyx.core.essentials.entities.UserId
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonNamingStrategy
import kotlinx.serialization.modules.SerializersModule

/**
 * The JSON configuration used for every request and response.
 *
 * `ignoreUnknownKeys` is on deliberately: a backend adding a field must never
 * break a shipped client.
 *
 * @param isDebug enables pretty printing, which only ever helps when reading logs
 * @param extraModule additional contextual serializers contributed by a feature
 */
public fun createDefaultJson(
    isDebug: Boolean,
    extraModule: SerializersModule = SerializersModule { },
): Json = Json {
    prettyPrint = isDebug
    explicitNulls = false
    encodeDefaults = true
    ignoreUnknownKeys = true
    namingStrategy = JsonNamingStrategy.SnakeCase
    serializersModule = SerializersModule {
        contextual(Id::class, GenericIdSerializer)
        contextual(UserId::class, UserIdSerializer)
        contextual(EventId::class, EventIdSerializer)
        include(extraModule)
    }
}
