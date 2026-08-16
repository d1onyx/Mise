package com.d1onix.dishlab.feature.products.presentation.comparison

import com.d1onix.dishlab.domain.model.ProductId

/** Health comparison rules used to highlight only values with an unambiguous winner. */
internal object ComparisonBestPolicy {
    private val lowerIsBetterNutrients = listOf(
        "sugar", "salt", "sodium", "saturated", "fat", "cholesterol", "energy", "calor",
    )
    private val higherIsBetterNutrients = listOf("protein", "fiber", "fibre")

    fun highestNumeric(values: List<Pair<ProductId, String>>): ProductId? =
        uniqueBest(values, String::toDoubleOrNull) { first, second -> first.compareTo(second) }

    fun lowestNumeric(values: List<Pair<ProductId, String>>): ProductId? =
        uniqueBest(values, String::toDoubleOrNull) { first, second -> second.compareTo(first) }

    fun bestGrade(values: List<Pair<ProductId, String>>): ProductId? =
        uniqueBest(
            values,
            { grade -> grade.trim().uppercase().let { "EDCBA".indexOf(it) + 1 }.takeIf { it > 0 } },
        ) { first, second -> first.compareTo(second) }

    fun bestNovaGroup(values: List<Pair<ProductId, String>>): ProductId? =
        uniqueBest(values, String::toIntOrNull) { first, second -> second.compareTo(first) }

    fun bestNutrientLevel(values: List<Pair<ProductId, String>>): ProductId? =
        uniqueBest(
            values,
            { level -> when (level.trim().lowercase()) { "low" -> 1; "moderate" -> 2; "high" -> 3; else -> null } },
        ) { first, second -> second.compareTo(first) }

    fun bestNutrient(name: String, values: List<Pair<ProductId, String>>): ProductId? = when {
        lowerIsBetterNutrients.any { name.contains(it, ignoreCase = true) } -> lowestNumeric(values)
        higherIsBetterNutrients.any { name.contains(it, ignoreCase = true) } -> highestNumeric(values)
        else -> null
    }

    private fun <T : Comparable<T>> uniqueBest(
        values: List<Pair<ProductId, String>>,
        parse: (String) -> T?,
        compare: (T, T) -> Int,
    ): ProductId? {
        val parsed = values.mapNotNull { (id, value) -> parse(value)?.let { id to it } }
        if (parsed.size != values.size || parsed.size < 2) return null
        val winningValue = parsed.maxWithOrNull { first, second -> compare(first.second, second.second) }?.second
            ?: return null
        return parsed.singleOrNull { it.second == winningValue }?.first
    }
}
