package com.dishlab.application.service

import com.dishlab.domain.error.ValidationError
import com.dishlab.domain.model.CatalogProduct
import com.dishlab.domain.model.CatalogProductPage
import com.dishlab.domain.model.CatalogIngredient
import com.dishlab.domain.model.toEnglishIngredientTaxonomyTag
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

interface ProductCatalogProvider {
    fun findByBarcode(barcode: String, language: String?): CatalogProduct?
    fun search(query: String, country: String?, language: String?, page: Int, pageSize: Int): CatalogProductPage
    fun searchCategories(query: String, language: String): List<String>
}

data class ProductNormalizationInput(
    val name: String,
    val categories: List<String> = emptyList(),
)

data class NormalizedProductName(
    val originalName: String,
    val normalizedNames: List<String>,
) {
    val canonicalTags: List<String>
        get() = normalizedNames
            .map(String::toEnglishIngredientTaxonomyTag)
            .filter(String::isNotBlank)
            .distinct()
}

interface ProductNameNormalizer {
    suspend fun normalize(products: List<ProductNormalizationInput>): List<NormalizedProductName>
}

class FallbackProductNameNormalizer : ProductNameNormalizer {
    override suspend fun normalize(products: List<ProductNormalizationInput>): List<NormalizedProductName> =
        products.map { product ->
            NormalizedProductName(
                originalName = product.name,
                normalizedNames = listOfNotNull(
                    product.categories.lastOrNull()
                    ?.trim()
                    ?.lowercase()
                    ?.takeIf(String::isNotBlank)
                    ?: product.name.substringBefore("—").trim().lowercase().takeIf(String::isNotBlank),
                ),
            )
        }
}

data class CatalogDump(
    val version: String,
    val items: List<IngredientTagEntry>,
)

class ProductCatalogService(
    private val provider: ProductCatalogProvider,
    private val products: ProductCatalogRepository = InMemoryProductCatalogRepository(),
    private val canonicalizer: ProductCanonicalizationService = ProductCanonicalizationService(
        FallbackProductNameNormalizer(),
        InMemoryFoodTaxonomyRepository(),
    ),
    private val tagCatalog: IngredientTagCatalog? = null,
    private val tagValidator: TagValidationProvider? = null,
    private val tagCategorizationProvider: TagCategorizationProvider? = null,
    private val backgroundScope: CoroutineScope = CoroutineScope(Dispatchers.IO + SupervisorJob()),
) {
    suspend fun findByBarcode(firebaseUid: String, barcode: String, language: String?): CatalogProduct? {
        validateBarcode(barcode)
        val normalized = barcode.filter(Char::isDigit)
        products.findByBarcode(normalized)?.let { return it }
        val product = provider.findByBarcode(normalized, language) ?: return null
        return products.save(canonicalizer.canonicalize(product.copy(barcode = normalized)))
    }

    /**
     * Canonicalizes an OFF snapshot fetched by a user's device. No Open Food
     * Facts request is made here, so public API traffic is not concentrated on
     * the DishLab server IP.
     */
    suspend fun resolveClientProduct(firebaseUid: String, product: CatalogProduct): CatalogProduct {
        validateClientProduct(product)
        val normalizedBarcode = product.barcode.filter(Char::isDigit)
        val resolved = canonicalizer.canonicalize(product.copy(barcode = normalizedBarcode))
        return products.save(resolved)
    }

    fun search(
        firebaseUid: String,
        query: String,
        country: String?,
        language: String?,
        page: Int,
        pageSize: Int,
    ): CatalogProductPage {
        val normalized = query.trim()
        if (normalized.length < 2) throw ValidationError(details = mapOf("q" to "Query must contain at least 2 characters"))
        return provider.search(
            query = normalized,
            country = country?.trim()?.takeIf(String::isNotBlank),
            language = language?.trim()?.takeIf(String::isNotBlank),
            page = page.coerceAtLeast(1),
            pageSize = pageSize.coerceIn(1, 100),
        )
    }

    fun searchCategories(firebaseUid: String, query: String, language: String): List<String> {
        if (query.isBlank()) return emptyList()
        return tagCatalog?.search(query.trim())
            ?: provider.searchCategories(query.trim(), language.trim().ifBlank { "en" })
    }

    /**
     * Full ingredient-tag catalog for offline client sync. Returns an empty
     * payload (no [version] change) when the client already has [since].
     */
    fun allCategories(firebaseUid: String, since: String?): CatalogDump {
        val catalog = tagCatalog ?: return CatalogDump(version = "", items = emptyList())
        val version = catalog.version()
        if (since != null && since == version) return CatalogDump(version = version, items = emptyList())
        return CatalogDump(version = version, items = catalog.all())
    }

    suspend fun validateAndAddTag(firebaseUid: String, tag: String, productName: String): TagValidationResult {
        val trimmed = tag.trim()
        if (trimmed.isBlank()) throw ValidationError(details = mapOf("tag" to "Tag is required"))
        val validator = tagValidator ?: return TagValidationResult(valid = true, tag = trimmed)
        val result = validator.validate(trimmed, productName.trim())
        if (!result.valid) return result

        val catalog = tagCatalog ?: return result

        // If normalized tag already exists in catalog — return canonical form
        val existing = catalog.resolve(result.tag)
        if (existing != null) {
            return TagValidationResult(valid = true, tag = existing)
        }

        // Reject non-Latin tags — all catalog tags must be in English (Latin script)
        val tagHasNonLatin = result.tag.any {
            it.code in 0x0400..0x04FF || it.code in 0x0600..0x06FF || it.code in 0x4E00..0x9FFF
        }
        if (tagHasNonLatin) {
            return TagValidationResult(
                valid = false,
                tag = result.tag,
                reason = "Тег має бути англійською мовою (наприклад, 'apple' замість 'яблуко').",
            )
        }

        // Detect transliterations: original input has Cyrillic but AI returned ASCII
        // that doesn't match anything in the English food catalog → AI transliterated instead of translated
        val originalHasCyrillic = trimmed.any { it.code in 0x0400..0x04FF }
        if (originalHasCyrillic && catalog.search(result.tag, limit = 1).isEmpty()) {
            return TagValidationResult(
                valid = false,
                tag = result.tag,
                reason = "Не вдалося перекласти '${trimmed}' на англійську. Введіть тег англійською мовою (наприклад, 'cake' замість 'тортик').",
            )
        }

        // New tag: return result immediately, categorize and persist in background
        val normalizedTag = result.tag
        backgroundScope.launch {
            val category = tagCategorizationProvider?.categorize(normalizedTag, catalog.categories()) ?: "other"
            catalog.addNew(normalizedTag, category)
        }

        return result
    }

    suspend fun save(firebaseUid: String, product: CatalogProduct): CatalogProduct {
        validateBarcode(product.barcode)
        if (product.name.isBlank()) throw ValidationError(details = mapOf("name" to "Product name is required"))
        return products.save(canonicalizer.canonicalize(product.copy(barcode = product.barcode.filter(Char::isDigit))))
    }

    suspend fun normalizeProducts(products: List<ProductNormalizationInput>): List<NormalizedProductName> {
        if (products.isEmpty()) return emptyList()
        if (products.size > 50) {
            throw ValidationError(details = mapOf("products" to "At most 50 products can be normalized at once"))
        }
        products.forEachIndexed { index, product ->
            if (product.name.trim().length !in 2..200) {
                throw ValidationError(details = mapOf("products[$index].name" to "Name must be between 2 and 200 characters"))
            }
        }
        return canonicalizer.normalize(
            products.map { it.copy(name = it.name.trim(), categories = it.categories.map(String::trim)) },
        )
    }

    private fun validateBarcode(value: String) {
        val barcode = value.filter(Char::isDigit)
        if (barcode.length !in 4..24) {
            throw ValidationError(details = mapOf("barcode" to "Barcode must contain between 4 and 24 digits"))
        }
    }

    private fun validateClientProduct(product: CatalogProduct) {
        validateBarcode(product.barcode)
        if (product.name.length !in 2..300) {
            throw ValidationError(details = mapOf("name" to "Name must be between 2 and 300 characters"))
        }
        val source = product.source
        if (source == null || source.provider != OPEN_FOOD_FACTS_PROVIDER || !source.clientProvided) {
            throw ValidationError(details = mapOf("source_provider" to "Unsupported product source"))
        }
        if (product.categories.size > MAX_TAGS || product.labels.size > MAX_TAGS ||
            product.allergens.size > MAX_TAGS || product.traces.size > MAX_TAGS || product.additives.size > MAX_TAGS
        ) {
            throw ValidationError(details = mapOf("tags" to "Product contains too many tags"))
        }
        if (product.ingredients.sumOf { it.treeSize() } > MAX_INGREDIENTS) {
            throw ValidationError(details = mapOf("ingredients" to "Product contains too many ingredients"))
        }
        if (product.nutrition?.nutrients.orEmpty().size > MAX_NUTRIENTS) {
            throw ValidationError(details = mapOf("nutrition" to "Product contains too many nutrients"))
        }
        if (product.packaging.size > MAX_PACKAGING_COMPONENTS) {
            throw ValidationError(details = mapOf("packaging" to "Product contains too many packaging components"))
        }
    }

    private fun CatalogIngredient.treeSize(): Int = 1 + ingredients.sumOf { it.treeSize() }

    private companion object {
        const val OPEN_FOOD_FACTS_PROVIDER = "open_food_facts"
        const val MAX_TAGS = 250
        const val MAX_INGREDIENTS = 500
        const val MAX_NUTRIENTS = 400
        const val MAX_PACKAGING_COMPONENTS = 100
    }
}
