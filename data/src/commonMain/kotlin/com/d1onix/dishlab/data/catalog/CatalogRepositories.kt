package com.d1onix.dishlab.data.catalog

import com.d1onix.dishlab.data.catalog.off.ClientProductSnapshotDto
import com.d1onix.dishlab.data.catalog.off.OpenFoodFactsProductDataSource
import com.d1onix.dishlab.domain.model.Nutrient
import com.d1onix.dishlab.domain.model.Product
import com.d1onix.dishlab.domain.model.ProductAlternative
import com.d1onix.dishlab.domain.model.ProductDataOrigin
import com.d1onix.dishlab.domain.model.ProductId
import com.d1onix.dishlab.domain.model.Recipe
import com.d1onix.dishlab.domain.model.RecipeId
import com.d1onix.dishlab.domain.repository.ProductRepository
import com.d1onix.dishlab.domain.repository.RecipeRepository
import com.d1onyx.core.datastore.KeyValueStorage
import com.d1onyx.core.datastore.PreferenceKey
import com.d1onyx.core.datastore.get
import com.d1onyx.core.essentials.di.AppScope
import com.d1onyx.core.essentials.exceptions.BackendException
import com.d1onyx.core.essentials.exceptions.ConnectionException
import dev.zacsweers.metro.ContributesBinding
import dev.zacsweers.metro.Inject
import dev.zacsweers.metro.SingleIn
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import kotlinx.coroutines.CancellationException
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlin.math.roundToInt

/**
 * Sends device-fetched product snapshots to the Ktor API for canonicalization.
 *
 * `open` so a test can stand in for the network without an HTTP round trip;
 * nothing in the app overrides it.
 */
@SingleIn(AppScope::class)
@Inject
open class BackendProductDataSource(
    private val client: HttpClient,
) {
    open suspend fun resolveClientProduct(snapshot: ClientProductSnapshotDto): BackendProductDto =
        client.post("api/v1/products/resolve") { setBody(snapshot) }.body()
}

/**
 * Small durable snapshot cache for products that came from the server.
 *
 * The graph persists identifiers only. Keeping the matching product payloads
 * here lets a graph and scan history render after an app restart or offline.
 */
@SingleIn(AppScope::class)
@Inject
class RemoteProductCache(
    private val storage: KeyValueStorage,
    private val json: Json,
) {
    suspend fun byBarcode(barcode: String): Product? = products().firstOrNull { it.barcode == barcode }?.toDomain()

    suspend fun byId(id: ProductId): Product? = products().firstOrNull { it.id == id.value }?.toDomain()

    suspend fun all(): List<Product> = products().map(CachedRemoteProductDto::toDomain)

    suspend fun put(product: Product) {
        val cached = product.toCached()
        val updated = (listOf(cached) + products().filterNot { it.id == cached.id })
            .take(REMOTE_PRODUCT_CACHE_LIMIT)
        storage.put(RemoteProductCacheKey, json.encodeToString(updated))
    }

    private suspend fun products(): List<CachedRemoteProductDto> =
        storage.get(RemoteProductCacheKey)
            ?.let { raw -> runCatching { json.decodeFromString<List<CachedRemoteProductDto>>(raw) }.getOrNull() }
            .orEmpty()

    private companion object {
        val RemoteProductCacheKey = PreferenceKey.StringKey("remote_product_cache_v1")
        const val REMOTE_PRODUCT_CACHE_LIMIT = 100
    }
}

/** DishLab's stable product response, independent of the Open Food Facts schema. */
@Serializable
data class BackendProductDto(
    val barcode: String,
    val name: String,
    val brand: String = "",
    val category: String = "",
    val categories: List<String> = emptyList(),
    val unit: String = "шт",
    val calories: String = "",
    val protein: String = "",
    val fat: String = "",
    val carbs: String = "",
    val imageUrl: String = "",
    val packageQuantity: Int = 0,
    val nutritionGrade: String = "",
    val canonicalTags: List<String> = emptyList(),
)

@Serializable
private data class CachedRemoteProductDto(
    val id: String,
    val barcode: String,
    val name: String,
    val category: String,
    val score: Int,
    val accentColor: Long,
    val initial: String,
    val nutrients: List<CachedNutrientDto>,
    val summary: String,
    val hasCompleteData: Boolean,
    val canonicalTags: List<String> = emptyList(),
    val dataOrigin: ProductDataOrigin = ProductDataOrigin.Canonical,
)

@Serializable
private data class CachedNutrientDto(
    val name: String,
    val amount: String,
    val unit: String,
)

/** Product repository backed by device-side OFF lookup, DishLab canonicalization and a local cache. */
@ContributesBinding(AppScope::class)
@Inject
class BackendProductRepository(
    private val backend: BackendProductDataSource,
    private val openFoodFacts: OpenFoodFactsProductDataSource,
    private val remoteCache: RemoteProductCache,
) : ProductRepository {

    override suspend fun byBarcode(barcode: String): Product? {
        val normalizedBarcode = barcode.filter(Char::isDigit)
        val cached = remoteCache.byBarcode(normalizedBarcode)

        // A canonical product is final and answers from the cache. A device
        // fallback is only a stand-in for an outage, so it never satisfies a
        // fresh scan: canonicalization is retried, and the stand-in is kept as
        // the fallback for that retry. Otherwise one outage would pin a product
        // to Open Food Facts data for as long as the install lives.
        if (cached != null && cached.dataOrigin == ProductDataOrigin.Canonical) return cached

        val snapshot = try {
            openFoodFacts.findByBarcode(normalizedBarcode)
        } catch (exception: Throwable) {
            if (exception is CancellationException) throw exception
            return cached ?: throw exception
        } ?: return cached

        return try {
            backend.resolveClientProduct(snapshot).toDomain().let { product ->
                remoteCache.put(product)
                product
            }
        } catch (exception: Throwable) {
            if (exception is CancellationException) throw exception
            if (!exception.isBackendUnavailable()) throw exception
            snapshot.toBackendProduct().toDomain()
                .copy(dataOrigin = ProductDataOrigin.DeviceFallback)
                .also { remoteCache.put(it) }
        }
    }

    override suspend fun byId(id: ProductId): Product? = remoteCache.byId(id)

    override suspend fun byIds(ids: List<ProductId>): List<Product> {
        val requested = ids.toSet()
        val cached = remoteCache.all().filter { it.id in requested }
        return cached.associateBy(Product::id).let { byId -> ids.mapNotNull(byId::get) }
    }

    override suspend fun all(): List<Product> = remoteCache.all()
}

private fun ClientProductSnapshotDto.toBackendProduct(): BackendProductDto {
    val nutrientValues = nutrition?.nutrients.orEmpty()
    val categoryNames = categories
        .filter { it.startsWith("en:") }
        .map { it.removePrefix("en:").replace('-', ' ').replaceFirstChar(Char::uppercaseChar) }
    return BackendProductDto(
        barcode = barcode,
        name = listOf(name, brand).filter(String::isNotBlank).joinToString(" — "),
        brand = brand,
        category = categoryNames.lastOrNull().orEmpty(),
        categories = categoryNames,
        unit = productQuantityUnit.lowercase().ifBlank { parseUnit(quantity) },
        calories = nutrientValues["energy-kcal"]?.value.displayOrEmpty(),
        protein = nutrientValues["proteins"]?.value.displayOrEmpty(),
        fat = nutrientValues["fat"]?.value.displayOrEmpty(),
        carbs = nutrientValues["carbohydrates"]?.value.displayOrEmpty(),
        imageUrl = imageFrontSmallUrl.ifBlank { imageFrontUrl },
        packageQuantity = productQuantity?.toInt() ?: 0,
        nutritionGrade = nutritionGrade,
    )
}

@ContributesBinding(AppScope::class)
@Inject
class CatalogRecipeRepository(
    private val catalog: RecipeCatalogDataSource,
) : RecipeRepository {

    override suspend fun all(): List<Recipe> = catalog.recipes()

    override suspend fun byId(id: RecipeId): Recipe? =
        catalog.recipes().firstOrNull { it.id == id }

    override suspend fun forProducts(productIds: List<ProductId>): List<Recipe> {
        if (productIds.isEmpty()) return all()
        return all()
            .filter { recipe -> recipe.productIds.any { it in productIds } }
            .sortedByDescending { recipe -> recipe.productIds.count { it in productIds } }
    }
}

internal fun BackendProductDto.toDomain(): Product {
    val calories = calories.toDoubleOrNull()
    val protein = protein.toDoubleOrNull()
    val fat = fat.toDoubleOrNull()
    val carbs = carbs.toDoubleOrNull()
    val displayName = name.ifBlank { brand.ifBlank { barcode } }
    val grade = nutritionGrade.lowercase().takeIf { it in NUTRITION_GRADES }
    return Product(
        id = ProductId("barcode:$barcode"),
        barcode = barcode,
        name = displayName,
        category = category.ifBlank { "Product" },
        score = grade.toDishLabScore(),
        accentColor = barcode.toAccentColor(),
        initial = displayName.firstOrNull()?.uppercase() ?: "?",
        nutrients = listOfNotNull(
            calories?.let { Nutrient("Energy", it.display(), "kcal") },
            carbs?.let { Nutrient("Carbs", it.display(), "g") },
            protein?.let { Nutrient("Protein", it.display(), "g") },
            fat?.let { Nutrient("Fat", it.display(), "g") },
        ),
        summary = buildString {
            if (brand.isNotBlank()) append(brand).append(" · ")
            append(category.ifBlank { "Food product" })
            append(". Nutrition data from Open Food Facts.")
            grade?.let { append(" Nutri-Score ").append(it.uppercase()).append('.') }
        },
        hasCompleteData = listOf(calories, protein, fat, carbs).all { it != null },
        alternatives = emptyList<ProductAlternative>(),
        canonicalTags = canonicalTags,
    )
}

private fun Product.toCached(): CachedRemoteProductDto = CachedRemoteProductDto(
    id = id.value,
    barcode = barcode,
    name = name,
    category = category,
    score = score,
    accentColor = accentColor,
    initial = initial,
    nutrients = nutrients.map { CachedNutrientDto(it.name, it.amount, it.unit) },
    summary = summary,
    hasCompleteData = hasCompleteData,
    canonicalTags = canonicalTags,
    dataOrigin = dataOrigin,
)

private fun CachedRemoteProductDto.toDomain(): Product = Product(
    id = ProductId(id),
    barcode = barcode,
    name = name,
    category = category,
    score = score,
    accentColor = accentColor,
    initial = initial,
    nutrients = nutrients.map { Nutrient(it.name, it.amount, it.unit) },
    summary = summary,
    hasCompleteData = hasCompleteData,
    canonicalTags = canonicalTags,
    dataOrigin = dataOrigin,
    alternatives = emptyList(),
)

private fun Throwable.isBackendUnavailable(): Boolean =
    this is ConnectionException || this is BackendException && httpCode.value >= HTTP_SERVER_ERROR

private const val HTTP_SERVER_ERROR = 500

private fun String?.toDishLabScore(): Int = when (this) {
    "a" -> 90
    "b" -> 75
    "c" -> 60
    "d" -> 40
    "e" -> 20
    else -> 50
}

private fun Double.display(): String = roundToInt().toString()

private fun Double?.displayOrEmpty(): String = this?.display().orEmpty()

private fun parseUnit(quantity: String): String =
    Regex("""[\d.,]+\s*([A-Za-zА-Яа-я]+)""").find(quantity)
        ?.groupValues
        ?.getOrNull(1)
        ?.lowercase()
        ?: "шт"

private fun String.toAccentColor(): Long {
    val index = (hashCode() and Int.MAX_VALUE) % PRODUCT_ACCENT_COLORS.size
    return PRODUCT_ACCENT_COLORS[index]
}

private val NUTRITION_GRADES = setOf("a", "b", "c", "d", "e")
private val PRODUCT_ACCENT_COLORS = longArrayOf(
    0xFFC8FF4DL,
    0xFF48DEDCL,
    0xFFFFC24EL,
    0xFF8B72FFL,
    0xFFFF7A6EL,
)
