package com.d1onix.dishlab.domain.model

import kotlin.jvm.JvmInline

@JvmInline
value class ProductId(val value: String)

/** Resolution-independent top-left position of a product on the graph canvas. */
data class ProductGraphPosition(
    val xFraction: Float,
    val yFraction: Float,
)

/** An undirected user-defined relationship between two scanned products. */
data class ProductConnection(
    val first: ProductId,
    val second: ProductId,
) {
    init {
        require(first != second) { "A product cannot be connected to itself" }
    }

    fun contains(id: ProductId): Boolean = first == id || second == id

    fun other(id: ProductId): ProductId? = when (id) {
        first -> second
        second -> first
        else -> null
    }

    companion object {
        fun between(first: ProductId, second: ProductId): ProductConnection =
            if (first.value <= second.value) {
                ProductConnection(first, second)
            } else {
                ProductConnection(second, first)
            }
    }
}

/**
 * A scanned product with the nutrition facts the app rates it on.
 *
 * [accentColor] is an ARGB value carried by the catalogue rather than picked in
 * the UI: the colour identifies the product across the graph, the recipe cards
 * and the history, so it belongs to the product itself.
 */
data class Product(
    val id: ProductId,
    val barcode: String,
    val name: String,
    val category: String,
    val score: Int,
    val accentColor: Long,
    val initial: String,
    val nutrients: List<Nutrient>,
    val summary: String,
    /** `false` when some nutrients were estimated — the UI warns about it. */
    val hasCompleteData: Boolean,
    val alternatives: List<ProductAlternative>,
    /** Backend-canonical ingredient tags used to match this product to recipes. */
    val canonicalTags: List<String> = emptyList(),
    /** Whether DishLab could apply its own canonicalization to this product. */
    val dataOrigin: ProductDataOrigin = ProductDataOrigin.Canonical,
    val details: ProductDetails = ProductDetails(),
) {
    val verdict: ScoreVerdict get() = ScoreVerdict.of(score)
}

data class ProductDetails(
    val brand: String = "",
    val quantity: String = "",
    val servingSize: String = "",
    val ingredientsText: String = "",
    /** Structured breakdown of [ingredientsText] — percent, vegan/vegetarian status. */
    val ingredients: List<ProductIngredient> = emptyList(),
    val allergens: List<String> = emptyList(),
    val traces: List<String> = emptyList(),
    val additives: List<String> = emptyList(),
    val categories: List<String> = emptyList(),
    val labels: List<String> = emptyList(),
    val countries: List<String> = emptyList(),
    val origins: List<String> = emptyList(),
    val nutriScore: String = "",
    val novaGroup: Int? = null,
    val ecoScore: String = "",
    val imageUrl: String = "",
    val ingredientsImageUrl: String = "",
    val nutritionImageUrl: String = "",
    val packagingImageUrl: String = "",
    val packaging: List<ProductPackagingComponent> = emptyList(),
    /** Every nutrient the source reported, not just the four headline ones on [Product.nutrients]. */
    val nutrients: List<Nutrient> = emptyList(),
    /** e.g. "fat" to "high" — OFF's quick quality read on fat/salt/sugars/saturated fat. */
    val nutrientLevels: Map<String, String> = emptyMap(),
    val foodGroups: List<String> = emptyList(),
    val manufacturingPlaces: List<String> = emptyList(),
    val purchasePlaces: List<String> = emptyList(),
    val stores: List<String> = emptyList(),
    /** PNNS food classification — finer-grained than [foodGroups], e.g. "Sweets" under "Sugary snacks". */
    val pnnsGroup: String = "",
    val pnnsSubgroup: String = "",
    /** The category the score was benchmarked against, e.g. "Spreads". */
    val comparedToCategory: String = "",
    /** Free-form "best before" on the scanned package, e.g. "12-2024". Not a parsed date — OFF sends it as OFF entered it. */
    val expirationDate: String = "",
    /** Which Nutri-Score formula produced [nutriScore], e.g. "2023". */
    val nutriScoreVersion: String = "",
    /** Free-text origin note, distinct from the structured [origins] tag list. */
    val originNote: String = "",
    /** What [nutrients] are reported per — "100g" or "serving". Never assume 100g: label the section with this. */
    val nutrientsPer: String = "",
)

data class ProductIngredient(
    val name: String,
    val percent: Double? = null,
    val percentEstimate: Double? = null,
    val vegan: String = "",
    val vegetarian: String = "",
)

data class ProductPackagingComponent(
    val numberOfUnits: Int? = null,
    val quantityPerUnit: String = "",
    val material: String = "",
    val shape: String = "",
    val recycling: String = "",
)

enum class ProductDataOrigin {
    Canonical,
    DeviceFallback,
}

data class Nutrient(
    val name: String,
    val amount: String,
    val unit: String,
)

/** A better-scoring stand-in suggested for a low-scoring product. */
data class ProductAlternative(
    val name: String,
    val score: Int,
) {
    val verdict: ScoreVerdict get() = ScoreVerdict.of(score)
}

/** The buy/skip call derived from a 0..100 score. */
enum class ScoreVerdict(val label: String) {
    Buy("Buy"),
    Maybe("Maybe"),
    Skip("Skip");

    companion object {
        fun of(score: Int): ScoreVerdict = when {
            score >= 70 -> Buy
            score >= 45 -> Maybe
            else -> Skip
        }
    }
}
