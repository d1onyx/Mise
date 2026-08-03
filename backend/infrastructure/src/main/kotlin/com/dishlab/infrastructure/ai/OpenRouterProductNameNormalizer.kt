package com.dishlab.infrastructure.ai

import com.dishlab.application.service.NormalizedProductName
import com.dishlab.application.service.ProductNameNormalizer
import com.dishlab.application.service.ProductNormalizationInput
import com.dishlab.infrastructure.catalog.IngredientNameCatalog
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.engine.cio.CIO
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.contentType
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import java.util.concurrent.ConcurrentHashMap

class OpenRouterProductNameNormalizer(
    private val apiKey: String,
    private val catalog: IngredientNameCatalog,
    private val model: String = "meta-llama/llama-3.2-3b-instruct",
    private val preset: String = "@preset/basic-products-v2",
) : ProductNameNormalizer {
    private val json = Json { ignoreUnknownKeys = true }
    private val cache = ConcurrentHashMap<String, NormalizedProductName>()
    private val client = HttpClient(CIO) {
        install(ContentNegotiation) {
            json(json)
        }
    }

    override suspend fun normalize(products: List<ProductNormalizationInput>): List<NormalizedProductName> {
        val results = arrayOfNulls<NormalizedProductName>(products.size)
        val missing = products.mapIndexedNotNull { index, product ->
            val key = product.cacheKey()
            cache[key]?.let { results[index] = it }
            if (results[index] == null) IndexedProduct(index, product, key) else null
        }
        if (missing.isNotEmpty()) {
            val normalized = requestNormalization(missing.map(IndexedProduct::product))
            missing.forEachIndexed { resultIndex, indexed ->
                val result = normalized.getOrNull(resultIndex) ?: fallback(indexed.product)
                cache[indexed.cacheKey] = result
                results[indexed.index] = result
            }
        }
        return results.mapIndexed { index, result -> result ?: fallback(products[index]) }
    }

    private suspend fun requestNormalization(products: List<ProductNormalizationInput>): List<NormalizedProductName> {
        val candidates = products.associate { product ->
            product.name to catalog.shortlist(product.name, product.categories)
        }
        val userPayload = NormalizationRequest(
            products = products.map { ProductInput(it.name, it.categories, candidates[it.name].orEmpty()) },
        )
        return runCatching {
            val response: ChatResponse = client.post(OPENROUTER_URL) {
                contentType(ContentType.Application.Json)
                header("Authorization", "Bearer $apiKey")
                header("HTTP-Referer", "https://dishlab.app")
                header("X-Title", "DishLab")
                setBody(
                    ChatRequest(
                        preset = preset,
                        model = model,
                        messages = listOf(
                            ChatMessage("system", SYSTEM_PROMPT),
                            ChatMessage("user", json.encodeToString(NormalizationRequest.serializer(), userPayload)),
                        ),
                    ),
                )
            }.body()
            val content = response.choices.firstOrNull()?.message?.content.orEmpty().extractJson()
            val parsed = json.decodeFromString<NormalizationResponse>(content)
            products.mapIndexed { index, product ->
                val modelNames = parsed.products.getOrNull(index)?.normalizedNames.orEmpty()
                val resolvedNames = modelNames
                    .flatMap { modelName -> catalog.resolve(modelName)?.let(catalog::relatedNames).orEmpty() }
                    .distinct()
                NormalizedProductName(
                    product.name,
                    resolvedNames.ifEmpty { fallback(product).normalizedNames },
                )
            }
        }.getOrElse {
            products.map(::fallback)
        }
    }

    private fun fallback(product: ProductNormalizationInput): NormalizedProductName {
        val resolved = catalog.resolve(product.categories.lastOrNull().orEmpty())
            ?: catalog.resolve(product.name.substringBefore("—"))
        return NormalizedProductName(
            product.name,
            resolved?.let(catalog::relatedNames).orEmpty(),
        )
    }

    private fun ProductNormalizationInput.cacheKey(): String =
        (listOf(name) + categories).joinToString("|").lowercase()

    private fun String.extractJson(): String {
        val trimmed = trim()
        if (!trimmed.startsWith("```")) return trimmed
        return trimmed.substringAfter('\n').substringBeforeLast("```").trim()
    }

    private data class IndexedProduct(
        val index: Int,
        val product: ProductNormalizationInput,
        val cacheKey: String,
    )

    @Serializable
    private data class ChatMessage(val role: String, val content: String)

    @Serializable
    private data class ChatRequest(
        val preset: String,
        val model: String,
        val messages: List<ChatMessage>,
        val temperature: Float = 0.0f,
        @SerialName("max_tokens") val maxTokens: Int = 800,
        @SerialName("response_format") val responseFormat: ResponseFormat = ResponseFormat(),
    )

    @Serializable
    private data class ResponseFormat(val type: String = "json_object")

    @Serializable
    private data class ChatResponse(val choices: List<Choice> = emptyList())

    @Serializable
    private data class Choice(val message: ChatMessage)

    @Serializable
    private data class NormalizationRequest(val products: List<ProductInput>)

    @Serializable
    private data class ProductInput(
        val name: String,
        val categories: List<String>,
        @SerialName("allowed_names") val allowedNames: List<String>,
    )

    @Serializable
    private data class NormalizationResponse(val products: List<NormalizedProductOutput> = emptyList())

    @Serializable
    private data class NormalizedProductOutput(
        @SerialName("original_name") val originalName: String = "",
        @SerialName("normalized_names") val normalizedNames: List<String> = emptyList(),
    )

    private companion object {
        const val OPENROUTER_URL = "https://openrouter.ai/api/v1/chat/completions"
        val SYSTEM_PROMPT = """
            You are a product name normalizer.

            You receive JSON with products. Each product has:
            - "name": original product name in any language; a brand may follow "—"
            - "categories": broad-to-specific categories; the last is most specific
            - "allowed_names": canonical English recipe ingredient names

            Return only JSON:
            {"products":[{"original_name":"...","normalized_names":["...","..."]}]}

            Rules:
            1. Use categories[-1] as the primary signal and name to resolve ambiguity.
            2. Strip brands, packaging, quantity, and marketing words.
            3. Return every relevant compatible ingredient name from allowed_names:
               - the most specific exact ingredient
               - its broader generic ingredient names
               - compatible intermediate variants
               - if product have any number, for example butter 67%, you don`t write 67%, write only butter
            4. Do not return incompatible sibling variants. Example: table salt implies salt,
               but does not imply sea salt, kosher salt, or smoked salt.
            5. Every normalized_names item must be lowercase and copied exactly from allowed_names.
            6. Order normalized_names from most specific to most general and remove duplicates.
            7. Return at most 12 normalized_names per product.
            8. If no allowed name describes a food ingredient, use an empty normalized_names array.
            9. Preserve product order and return exactly one output per input.
            10. No markdown and no explanation.
        """.trimIndent()
    }
}
