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
    /** Whether DishLab could apply its own canonicalization to this product. */
    val dataOrigin: ProductDataOrigin = ProductDataOrigin.Canonical,
) {
    val verdict: ScoreVerdict get() = ScoreVerdict.of(score)
}

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
