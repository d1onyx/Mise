package com.d1onix.dishlab.data.catalog.off

import com.d1onyx.core.essentials.di.AppScope
import com.d1onyx.core.essentials.exceptions.BackendException
import com.d1onyx.core.essentials.logger.Logger
import com.d1onyx.core.network.NetworkConfig
import com.d1onyx.core.network.auth.AuthTokenProvider
import com.d1onyx.core.network.createHttpClient
import dev.zacsweers.metro.Inject
import dev.zacsweers.metro.SingleIn
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.parameter
import io.ktor.http.HttpHeaders
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject

/**
 * Calls Open Food Facts from the Android/iOS device. This client deliberately
 * has no DishLab bearer token and therefore cannot leak it to a third party.
 */
@SingleIn(AppScope::class)
@Inject
open class OpenFoodFactsProductDataSource(
    config: NetworkConfig,
    logger: Logger,
    private val json: Json,
) {
    private val client = createHttpClient(
        config = config.copy(baseUrl = OPEN_FOOD_FACTS_BASE_URL),
        logger = logger,
        tokenProvider = AuthTokenProvider.None,
        json = json,
    )

    /**
     * No `fields` parameter: OFF only returns what is asked for, so restricting
     * it to a named subset silently drops every field this DTO does not (yet)
     * know about. Omitting it returns the complete product object, which is
     * also what lets [rawSourceJson][ClientProductSnapshotDto.rawSourceJson]
     * carry everything OFF has for this barcode.
     */
    open suspend fun findByBarcode(barcode: String): ClientProductSnapshotDto? {
        val normalizedBarcode = barcode.filter(Char::isDigit)
        return try {
            val root = client.get("api/v3.6/product/$normalizedBarcode.json") {
                header(HttpHeaders.UserAgent, OPEN_FOOD_FACTS_USER_AGENT)
                parameter("product_type", "food")
            }.body<JsonObject>()
            val response = json.decodeFromJsonElement(OpenFoodFactsProductResponseDto.serializer(), root)
            response.toSnapshot(normalizedBarcode, rawProductJson = root["product"])
        } catch (exception: BackendException) {
            if (exception.httpCode.value == HTTP_NOT_FOUND) null else throw exception
        }
    }

    private companion object {
        const val OPEN_FOOD_FACTS_BASE_URL = "https://world.openfoodfacts.org/"
        const val OPEN_FOOD_FACTS_USER_AGENT = "DishLab/1.0 (https://github.com/d1onyx)"
        const val HTTP_NOT_FOUND = 404
    }
}
