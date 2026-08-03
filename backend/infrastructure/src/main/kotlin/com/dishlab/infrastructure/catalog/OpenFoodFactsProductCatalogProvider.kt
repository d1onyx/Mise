package com.dishlab.infrastructure.catalog

import com.dishlab.application.service.ProductCatalogProvider
import com.dishlab.domain.model.CatalogProduct
import com.dishlab.domain.model.CatalogProductPage
import com.dishlab.domain.model.toEnglishIngredientTaxonomyTag
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.doubleOrNull
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import java.net.URI
import java.net.URLEncoder
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.nio.charset.StandardCharsets
import java.util.concurrent.ConcurrentHashMap

class OpenFoodFactsProductCatalogProvider(
    private val userAgent: String = "DishLab/1.0 (server)",
    private val client: HttpClient = HttpClient.newBuilder().build(),
) : ProductCatalogProvider {
    private val json = Json { ignoreUnknownKeys = true }
    private val canonicalTagsCache = ConcurrentHashMap<String, List<String>>()

    override fun findByBarcode(barcode: String, language: String?): CatalogProduct? {
        val fields = PRODUCT_FIELDS.joinToString(",")
        val url = "https://world.openfoodfacts.org/api/v3/product/$barcode?fields=${encode(fields)}"
        val root = getJson(url) ?: return null
        val product = root["product"] as? JsonObject ?: return null
        val catalogProduct = product.toProduct(barcode) ?: return null
        return catalogProduct.copy(
            canonicalTags = canonicalizeIngredientTags(
                query = catalogProduct.name,
                language = language.orEmpty().ifBlank { "en" },
                fallbackToSlug = false,
            ),
        )
    }

    override fun search(
        query: String,
        country: String?,
        language: String?,
        page: Int,
        pageSize: Int,
    ): CatalogProductPage {
        val params = linkedMapOf(
            "search_terms" to query,
            "action" to "process",
            "json" to "1",
            "page" to page.toString(),
            "page_size" to pageSize.toString(),
            "fields" to SEARCH_FIELDS.joinToString(","),
        )
        country?.let { params["countries_tags_en"] = it }
        language?.let { params["lc"] = it }
        val url = "https://world.openfoodfacts.org/cgi/search.pl?" +
            params.entries.joinToString("&") { (key, value) -> "${encode(key)}=${encode(value)}" }
        val root = getJson(url) ?: return CatalogProductPage(emptyList(), page, false)
        val count = root["count"]?.jsonPrimitive?.intOrNull ?: 0
        val items = (root["products"] as? JsonArray).orEmpty()
            .mapNotNull { element ->
                val product = element as? JsonObject ?: return@mapNotNull null
                val barcode = product.string("_id").ifBlank { product.string("code") }
                product.toProduct(barcode)
            }
            .filter { it.barcode.isNotBlank() && it.name.isNotBlank() }
            .distinctBy(CatalogProduct::barcode)
        return CatalogProductPage(items, page, page * pageSize < count && items.isNotEmpty())
    }

    override fun searchCategories(query: String, language: String): List<String> {
        return canonicalizeIngredientTags(query, language)
            .map { it.removePrefix("en:").replace('-', ' ').replaceFirstChar(Char::uppercaseChar) }
    }

    private fun canonicalizeIngredientTags(
        query: String,
        language: String,
        fallbackToSlug: Boolean = true,
    ): List<String> {
        val cacheKey = "${language.lowercase()}:${query.trim().lowercase()}"
        val canonical = canonicalTagsCache.getOrPut(cacheKey) {
            runCatching { fetchCanonicalIngredientTags(query, language) }
                .getOrDefault(emptyList())
        }
        // For barcode lookups we must NOT fabricate a slug tag (e.g. en:maslo-61) from the
        // product name — such a tag never matches recipes. Leaving the list empty lets the
        // client fall back to the AI tag validator to derive a real taxonomy tag.
        return canonical.ifEmpty {
            if (fallbackToSlug) listOf(query.toEnglishIngredientTaxonomyTag()).filter(String::isNotBlank)
            else emptyList()
        }
    }

    private fun fetchCanonicalIngredientTags(query: String, language: String): List<String> {
        val url = "https://world.openfoodfacts.org/api/v3/taxonomy_canonicalize_tags?" +
            listOf(
                "tagtype" to "ingredients",
                "local_tags_list" to query,
                "lc" to language.ifBlank { "en" },
            ).joinToString("&") { (key, value) -> "${encode(key)}=${encode(value)}" }
        val root = getJson(url) ?: return emptyList()
        return (root["canonical_tags"] as? JsonArray).orEmpty()
            .mapNotNull { it as? JsonObject }
            .mapNotNull { it["tag"]?.jsonPrimitive?.content?.takeIf { tag -> tag.startsWith("en:") } }
            .map { tag -> tag.removePrefix("en:").toEnglishIngredientTaxonomyTag() }
            .filter(String::isNotBlank)
            .distinct()
    }

    private fun getJson(url: String): JsonObject? {
        val request = HttpRequest.newBuilder(URI.create(url))
            .header("Accept", "application/json")
            .header("User-Agent", userAgent)
            .GET()
            .build()
        val response = client.send(request, HttpResponse.BodyHandlers.ofString())
        if (response.statusCode() !in 200..299) return null
        return json.parseToJsonElement(response.body()) as? JsonObject
    }

    private fun JsonObject.toProduct(barcode: String): CatalogProduct? {
        val name = string("product_name").trim()
        if (barcode.isBlank() || name.isBlank()) return null
        val categories = array("categories_tags")
            .filter { it.startsWith("en:") }
            .map { it.removePrefix("en:").replace('-', ' ').replaceFirstChar(Char::uppercaseChar) }
        val category = categories.lastOrNull().orEmpty()
        val nutriments = this["nutriments"] as? JsonObject
        return CatalogProduct(
            barcode = barcode,
            name = name,
            brand = string("brands").trim(),
            category = category,
            categories = categories,
            unit = string("product_quantity_unit").lowercase().ifBlank { parseUnit(string("quantity")) },
            calories = nutriments?.number("energy-kcal_100g"),
            protein = nutriments?.number("proteins_100g"),
            fat = nutriments?.number("fat_100g"),
            carbs = nutriments?.number("carbohydrates_100g"),
            imageUrl = string("image_front_small_url"),
            packageQuantity = number("product_quantity")?.toInt() ?: 0,
            nutritionGrade = string("nutrition_grades").lowercase().takeIf { it in NUTRITION_GRADES }.orEmpty(),
        )
    }

    private fun JsonObject.string(key: String): String =
        this[key]?.jsonPrimitive?.content.orEmpty()

    private fun JsonObject.number(key: String): Double? =
        this[key]?.jsonPrimitive?.doubleOrNull

    private fun JsonObject.array(key: String): List<String> =
        (this[key] as? JsonArray).orEmpty().mapNotNull { it.jsonPrimitive.content.takeIf(String::isNotBlank) }

    private fun parseUnit(quantity: String): String =
        Regex("""[\d.,]+\s*([A-Za-zА-Яа-я]+)""").find(quantity)
            ?.groupValues
            ?.getOrNull(1)
            ?.lowercase()
            ?: "шт"

    private fun encode(value: String): String = URLEncoder.encode(value, StandardCharsets.UTF_8)
    private companion object {
        val PRODUCT_FIELDS = listOf(
            "code", "product_name", "brands", "product_quantity", "product_quantity_unit",
            "quantity", "categories_tags", "image_front_small_url", "nutrition_grades", "nutriments",
        )
        val SEARCH_FIELDS = listOf(
            "_id", "code", "product_name", "brands", "image_front_small_url",
            "categories_tags", "product_quantity", "product_quantity_unit", "quantity", "nutrition_grades", "nutriments",
        )
        val NUTRITION_GRADES = setOf("a", "b", "c", "d", "e")
    }
}
