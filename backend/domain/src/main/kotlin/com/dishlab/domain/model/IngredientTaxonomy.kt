package com.dishlab.domain.model

import java.text.Normalizer
import java.util.Locale

fun String.toEnglishIngredientTaxonomyTag(): String {
    val slug = Normalizer.normalize(this, Normalizer.Form.NFKD)
        .replace(CombiningMarks, "")
        .lowercase(Locale.ROOT)
        .replace(Apostrophes, "")
        .replace(NonAlphaNumeric, "-")
        .trim('-')
        .replace(RepeatedDashes, "-")
    return slug.takeIf(String::isNotBlank)?.let { "en:$it" }.orEmpty()
}

private val CombiningMarks = Regex("\\p{M}+")
private val Apostrophes = Regex("['’`]")
private val NonAlphaNumeric = Regex("[^\\p{L}\\p{N}]+")
private val RepeatedDashes = Regex("-+")
