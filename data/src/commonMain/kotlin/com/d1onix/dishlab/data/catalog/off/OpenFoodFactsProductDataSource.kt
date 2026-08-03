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

/**
 * Calls Open Food Facts from the Android/iOS device. This client deliberately
 * has no DishLab bearer token and therefore cannot leak it to a third party.
 */
@SingleIn(AppScope::class)
@Inject
class OpenFoodFactsProductDataSource(
    config: NetworkConfig,
    logger: Logger,
    json: Json,
) {
    private val client = createHttpClient(
        config = config.copy(baseUrl = OPEN_FOOD_FACTS_BASE_URL),
        logger = logger,
        tokenProvider = AuthTokenProvider.None,
        json = json,
    )

    suspend fun findByBarcode(barcode: String): ClientProductSnapshotDto? {
        val normalizedBarcode = barcode.filter(Char::isDigit)
        return try {
            client.get("api/v3.6/product/$normalizedBarcode.json") {
                header(HttpHeaders.UserAgent, OPEN_FOOD_FACTS_USER_AGENT)
                parameter("product_type", "food")
                parameter("fields", PRODUCT_FIELDS.joinToString(","))
            }.body<OpenFoodFactsProductResponseDto>().toSnapshot(normalizedBarcode)
        } catch (exception: BackendException) {
            if (exception.httpCode.value == HTTP_NOT_FOUND) null else throw exception
        }
    }

    private companion object {
        const val OPEN_FOOD_FACTS_BASE_URL = "https://world.openfoodfacts.org/"
        const val OPEN_FOOD_FACTS_USER_AGENT = "DishLab/1.0 (https://github.com/d1onyx)"
        const val HTTP_NOT_FOUND = 404

        val PRODUCT_FIELDS = listOf(
            "code",
            "product_name",
            "generic_name",
            "lang",
            "brands",
            "quantity",
            "product_quantity",
            "product_quantity_unit",
            "serving_size",
            "categories_tags",
            "labels_tags",
            "countries_tags",
            "origins_tags",
            "ingredients_text",
            "ingredients",
            "allergens_tags",
            "traces_tags",
            "additives_tags",
            "nutrition",
            "nutriscore_grade",
            "nutriscore_score",
            "nova_group",
            "environmental_score_grade",
            "environmental_score_score",
            "image_front_url",
            "image_front_small_url",
            "packagings",
            "schema_version",
            "rev",
            "last_modified_t",
        )
    }
}
