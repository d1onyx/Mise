package com.d1onix.dishlab.data.demo

import com.d1onix.dishlab.domain.model.Product
import kotlin.math.absoluteValue

/**
 * Demo behaviour of the bundled catalogue.
 *
 * The app ships without a product API, so a real barcode would resolve to
 * nothing and most of the UI would be unreachable. While [ALWAYS_RESOLVE_SCANS]
 * is on, an unknown barcode is mapped onto a catalogue product instead of
 * failing, so any scan walks the user into the graph.
 *
 * Delete this file — and the `?:` fallback in `CatalogProductRepository` — when
 * the OpenFoodFacts client lands.
 */
object DemoMode {

    const val ALWAYS_RESOLVE_SCANS: Boolean = true

    /** The one barcode that still reports «not found», so that screen stays reachable. */
    const val NOT_FOUND_BARCODE: String = "000000000000"

    /** Recipes marked as saved on first launch, so the Saved screen is not empty. */
    val savedRecipeIds: List<String> = listOf("bowl", "overnight", "balls")

    /** Seeded scan history, oldest first. */
    val historyProductIds: List<String> = listOf("yogurt", "banana", "oats")
}

/**
 * Maps an unknown barcode onto a catalogue product.
 *
 * Deterministic on purpose: the same barcode always gives the same product, so
 * rescanning the same item behaves like a real catalogue would.
 */
fun List<Product>.resolveDemoBarcode(barcode: String): Product? {
    if (isEmpty()) return null
    if (!DemoMode.ALWAYS_RESOLVE_SCANS) return null
    if (barcode == DemoMode.NOT_FOUND_BARCODE) return null
    return this[barcode.hashCode().absoluteValue % size]
}
