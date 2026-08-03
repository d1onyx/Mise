package com.dishlab.application.service

data class TagValidationResult(
    val valid: Boolean,
    val tag: String,
    val reason: String? = null,
)

interface TagValidationProvider {
    suspend fun validate(tag: String, productName: String): TagValidationResult
}

data class IngredientTagEntry(
    val name: String,
    val category: String,
)

interface IngredientTagCatalog {
    fun search(query: String, limit: Int = 20): List<String>
    fun addNew(name: String, category: String = "other"): Boolean
    fun resolve(name: String): String?
    fun categories(): List<String>

    /** Full catalog enumeration used by the client offline-sync endpoint. */
    fun all(): List<IngredientTagEntry>

    /** Opaque content version that changes whenever the catalog gains entries. */
    fun version(): String
}

interface TagCategorizationProvider {
    suspend fun categorize(tag: String, availableCategories: List<String>): String
}
