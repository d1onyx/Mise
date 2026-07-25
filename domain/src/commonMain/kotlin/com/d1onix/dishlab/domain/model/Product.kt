package com.d1onix.dishlab.domain.model

import kotlin.jvm.JvmInline

@JvmInline
value class ProductId(val value: String)

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
) {
    val verdict: ScoreVerdict get() = ScoreVerdict.of(score)
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
