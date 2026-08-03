package com.dishlab.infrastructure.catalog

import com.dishlab.application.service.IngredientTagCatalog
import com.dishlab.application.service.IngredientTagEntry
import com.dishlab.application.service.NormalizedProductName
import com.dishlab.application.service.ProductNameNormalizer
import com.dishlab.application.service.ProductNormalizationInput
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.put
import java.nio.file.Path
import java.sql.DriverManager
import java.text.Normalizer
import java.util.Locale

data class IngredientNameCandidate(
    val name: String,
    val category: String,
)

class IngredientNameCatalog private constructor(
    private val candidates: MutableList<IngredientNameCandidate>,
    private val filePath: Path? = null,
) : IngredientTagCatalog {
    private val names = candidates.associateBy { it.name }.toMutableMap()

    fun contains(name: String): Boolean = normalize(name) in names

    override fun all(): List<IngredientTagEntry> =
        candidates.map { IngredientTagEntry(name = it.name, category = it.category) }

    override fun version(): String {
        val contentHash = candidates.fold(0) { acc, candidate -> acc * 31 + candidate.name.hashCode() }
        return "${candidates.size}:$contentHash"
    }

    override fun search(query: String, limit: Int): List<String> {
        val normalized = normalize(query)
        if (normalized.isBlank()) return emptyList()
        val tokens = normalized.tokens()
        return candidates.asSequence()
            .map { candidate ->
                val candidateTokens = candidate.name.tokens()
                val score = when {
                    candidate.name.startsWith(normalized) -> 2.0
                    normalized in candidate.name -> 1.5
                    else -> similarity(tokens, candidateTokens)
                }
                candidate.name to score
            }
            .filter { it.second > 0.0 }
            .sortedByDescending { it.second }
            .take(limit)
            .map { it.first }
            .toList()
    }

    override fun addNew(name: String, category: String): Boolean {
        val normalized = normalize(name)
        if (normalized.isBlank() || normalized in names) return false
        val candidate = IngredientNameCandidate(normalized, category)
        candidates.add(candidate)
        names[normalized] = candidate
        persist()
        return true
    }

    private fun persist() {
        val path = filePath ?: return
        runCatching {
            val grouped = candidates.groupBy { it.category }
            val root = buildJsonObject {
                grouped.forEach { (cat, items) ->
                    put(cat, buildJsonObject {
                        items.forEach { put(it.name, buildJsonObject {}) }
                    })
                }
            }
            path.toFile().writeText(Json.encodeToString(JsonObject.serializer(), root))
        }
    }

    override fun categories(): List<String> = candidates.map { it.category }.distinct()

    override fun resolve(name: String): String? {
        val normalized = normalize(name)
        names[normalized]?.let { return it.name }

        val inputTokens = normalized.tokens()
        return candidates.asSequence()
            .map { candidate -> candidate.name to similarity(inputTokens, candidate.name.tokens()) }
            .maxByOrNull { it.second }
            ?.takeIf { it.second >= MIN_RESOLVE_SCORE }
            ?.first
    }

    fun relatedNames(name: String, limit: Int = 12): List<String> {
        val resolved = resolve(name) ?: return emptyList()
        val resolvedTokens = resolved.tokens()
        return candidates.asSequence()
            .filter { candidate ->
                val candidateTokens = candidate.name.tokens()
                candidateTokens.isNotEmpty() && candidateTokens.all(resolvedTokens::contains)
            }
            .sortedWith(
                compareByDescending<IngredientNameCandidate> { it.name == resolved }
                    .thenByDescending { it.name.tokens().size }
                    .thenBy { it.name.length },
            )
            .map(IngredientNameCandidate::name)
            .distinct()
            .take(limit)
            .toList()
    }

    fun shortlist(name: String, categories: List<String>, limit: Int = 120): List<String> {
        val signals = (listOf(name.substringBefore("—")) + categories)
            .joinToString(" ")
            .let(::normalize)
            .tokens()
        if (signals.isEmpty()) return emptyList()

        return candidates.asSequence()
            .map { candidate ->
                val candidateTokens = candidate.name.tokens()
                val categoryTokens = candidate.category.replace('_', ' ').tokens()
                val score = similarity(signals, candidateTokens) +
                    similarity(signals, categoryTokens) * CATEGORY_WEIGHT +
                    if (candidate.name in normalize(name)) EXACT_SUBSTRING_BONUS else 0.0
                candidate.name to score
            }
            .filter { it.second > 0.0 }
            .sortedByDescending { it.second }
            .take(limit)
            .map { it.first }
            .toList()
    }

    companion object {
        private const val MIN_RESOLVE_SCORE = 0.72
        private const val CATEGORY_WEIGHT = 0.35
        private const val EXACT_SUBSTRING_BONUS = 0.8
        private val nonAlphaNumeric = Regex("[^\\p{L}\\p{N}]+")
        private val spaces = Regex("\\s+")

        fun load(path: Path): IngredientNameCatalog {
            val root = Json.parseToJsonElement(path.toFile().readText()).jsonObject
            val candidates = root.entries.flatMap { (category, value) ->
                (value as? JsonObject).orEmpty().keys.map { name ->
                    IngredientNameCandidate(normalize(name), category)
                }
            }.distinctBy(IngredientNameCandidate::name)
            require(candidates.isNotEmpty()) { "Ingredient name catalog is empty: $path" }
            return IngredientNameCatalog(candidates.toMutableList(), path)
        }

        fun fromDb(jdbcUrl: String): IngredientNameCatalog {
            val candidates = mutableListOf<IngredientNameCandidate>()
            DriverManager.getConnection(jdbcUrl).use { conn ->
                conn.createStatement().use { stmt ->
                    stmt.executeQuery("SELECT canonical_name FROM ingredients ORDER BY id").use { rs ->
                        while (rs.next()) {
                            val name = normalize(rs.getString("canonical_name"))
                            if (name.isNotBlank()) candidates.add(IngredientNameCandidate(name, "ingredient"))
                        }
                    }
                }
            }
            require(candidates.isNotEmpty()) { "Ingredient catalog is empty in: $jdbcUrl" }
            return IngredientNameCatalog(candidates, filePath = null)
        }

        private fun normalize(value: String): String =
            Normalizer.normalize(value, Normalizer.Form.NFKD)
                .filterNot { Character.getType(it) == Character.NON_SPACING_MARK.toInt() }
                .lowercase(Locale.ROOT)
                .replace(nonAlphaNumeric, " ")
                .replace(spaces, " ")
                .trim()

        private fun String.tokens(): Set<String> =
            split(' ')
                .filter { it.length > 1 }
                .map { token -> token.singularize() }
                .toSet()

        private fun String.singularize(): String = when {
            endsWith("ies") && length > 4 -> dropLast(3) + "y"
            endsWith("ses") && length > 4 -> dropLast(2)
            endsWith("s") && !endsWith("ss") && length > 3 -> dropLast(1)
            else -> this
        }

        private fun similarity(first: Set<String>, second: Set<String>): Double {
            if (first.isEmpty() || second.isEmpty()) return 0.0
            val intersection = first.intersect(second).size.toDouble()
            return intersection / first.union(second).size
        }
    }
}

class CatalogProductNameNormalizer(
    private val catalog: IngredientNameCatalog,
) : ProductNameNormalizer {
    override suspend fun normalize(products: List<ProductNormalizationInput>): List<NormalizedProductName> =
        products.map { product ->
            val normalizedName = catalog.resolve(product.categories.lastOrNull().orEmpty())
                ?: catalog.resolve(product.name.substringBefore("—"))
            NormalizedProductName(
                product.name,
                normalizedName?.let(catalog::relatedNames).orEmpty(),
            )
        }
}
