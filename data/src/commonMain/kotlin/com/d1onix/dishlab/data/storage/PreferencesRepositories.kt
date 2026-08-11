package com.d1onix.dishlab.data.storage

import com.d1onix.dishlab.domain.model.ProductId
import com.d1onix.dishlab.domain.model.ProfileSettings
import com.d1onix.dishlab.domain.model.AllergenPreference
import com.d1onix.dishlab.domain.model.CookingPreferences
import com.d1onix.dishlab.domain.model.DietPreference
import com.d1onix.dishlab.domain.model.KitchenEquipment
import com.d1onix.dishlab.domain.model.RecipeId
import com.d1onix.dishlab.domain.model.TastePreference
import com.d1onix.dishlab.domain.repository.CookingPreferencesRepository
import com.d1onix.dishlab.domain.repository.SavedRecipesRepository
import com.d1onix.dishlab.domain.repository.ScanHistoryRepository
import com.d1onix.dishlab.domain.repository.ProfileSettingsRepository
import com.d1onyx.core.datastore.KeyValueStorage
import com.d1onyx.core.datastore.PreferenceKey
import com.d1onyx.core.datastore.get
import com.d1onyx.core.essentials.di.AppScope
import dev.zacsweers.metro.ContributesBinding
import dev.zacsweers.metro.Inject
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.combine

private object DishLabKeys {
    val SavedRecipes = PreferenceKey.StringSetKey("saved_recipes")
    /** Ordered, most recent first — a set would lose that, so it is one string. */
    val ScanHistory = PreferenceKey.StringKey("scan_history")
    val ProfileName = PreferenceKey.StringKey("profile_name")
    val AutoConnectProducts = PreferenceKey.BooleanKey("auto_connect_products")
    val ReduceGraphMotion = PreferenceKey.BooleanKey("reduce_graph_motion")
    val ShowProductScores = PreferenceKey.BooleanKey("show_product_scores")
    val Diets = PreferenceKey.StringSetKey("cooking_diets")
    val Allergens = PreferenceKey.StringSetKey("cooking_allergens")
    val Tastes = PreferenceKey.StringSetKey("cooking_tastes")
    val Equipment = PreferenceKey.StringSetKey("cooking_equipment")
}

private const val HISTORY_SEPARATOR = ","
private const val HISTORY_LIMIT = 30

@ContributesBinding(AppScope::class)
@Inject
class StoredSavedRecipesRepository(
    private val storage: KeyValueStorage,
) : SavedRecipesRepository {

    override val saved: Flow<Set<RecipeId>> =
        storage.observe(DishLabKeys.SavedRecipes).map { ids ->
            ids.orEmpty().map(::RecipeId).toSet()
        }

    override suspend fun toggle(id: RecipeId) {
        val current = storage.get(DishLabKeys.SavedRecipes).orEmpty()
        val updated = if (id.value in current) current - id.value else current + id.value
        storage.put(DishLabKeys.SavedRecipes, updated)
    }
}

@ContributesBinding(AppScope::class)
@Inject
class StoredScanHistoryRepository(
    private val storage: KeyValueStorage,
) : ScanHistoryRepository {

    override val history: Flow<List<ProductId>> =
        storage.observe(DishLabKeys.ScanHistory).map { raw -> raw.decodeHistory() }

    override suspend fun add(id: ProductId) {
        val current = storage.get(DishLabKeys.ScanHistory).decodeHistory()
        val updated = (listOf(id) + current.filterNot { it == id }).take(HISTORY_LIMIT)
        storage.put(DishLabKeys.ScanHistory, updated.joinToString(HISTORY_SEPARATOR) { it.value })
    }

    override suspend fun remove(id: ProductId) {
        val updated = storage.get(DishLabKeys.ScanHistory)
            .decodeHistory()
            .filterNot { it == id }
        storage.put(DishLabKeys.ScanHistory, updated.joinToString(HISTORY_SEPARATOR) { it.value })
    }

    override suspend fun clear() {
        storage.remove(DishLabKeys.ScanHistory)
    }
}

private fun String?.decodeHistory(): List<ProductId> =
    this?.split(HISTORY_SEPARATOR)
        ?.filter { it.isNotBlank() }
        ?.map(::ProductId)
        .orEmpty()

@ContributesBinding(AppScope::class)
@Inject
class StoredProfileSettingsRepository(
    private val storage: KeyValueStorage,
) : ProfileSettingsRepository {

    override val settings: Flow<ProfileSettings> = combine(
        storage.observe(DishLabKeys.ProfileName),
        storage.observe(DishLabKeys.AutoConnectProducts),
        storage.observe(DishLabKeys.ReduceGraphMotion),
        storage.observe(DishLabKeys.ShowProductScores),
    ) { name, autoConnect, reduceMotion, showScores ->
        ProfileSettings(
            displayName = name?.takeIf { it.isNotBlank() } ?: "Alex Kim",
            autoConnectNewProducts = autoConnect ?: true,
            reduceGraphMotion = reduceMotion ?: false,
            showProductScores = showScores ?: true,
        )
    }

    override suspend fun setDisplayName(value: String) {
        storage.put(DishLabKeys.ProfileName, value.trim().take(40))
    }

    override suspend fun setAutoConnectNewProducts(enabled: Boolean) {
        storage.put(DishLabKeys.AutoConnectProducts, enabled)
    }

    override suspend fun setReduceGraphMotion(enabled: Boolean) {
        storage.put(DishLabKeys.ReduceGraphMotion, enabled)
    }

    override suspend fun setShowProductScores(enabled: Boolean) {
        storage.put(DishLabKeys.ShowProductScores, enabled)
    }
}

@ContributesBinding(AppScope::class)
@Inject
class StoredCookingPreferencesRepository(
    private val storage: KeyValueStorage,
) : CookingPreferencesRepository {

    override val preferences: Flow<CookingPreferences> = combine(
        storage.observe(DishLabKeys.Diets),
        storage.observe(DishLabKeys.Allergens),
        storage.observe(DishLabKeys.Tastes),
        storage.observe(DishLabKeys.Equipment),
    ) { diets, allergens, tastes, equipment ->
        CookingPreferences(
            diets = diets.toEnumSet<DietPreference>(),
            allergens = allergens.toEnumSet<AllergenPreference>(),
            tastes = tastes.toEnumSet<TastePreference>(),
            equipment = equipment.toEnumSet<KitchenEquipment>(),
        )
    }

    override suspend fun save(preferences: CookingPreferences) {
        storage.put(DishLabKeys.Diets, preferences.diets.toNames())
        storage.put(DishLabKeys.Allergens, preferences.allergens.toNames())
        storage.put(DishLabKeys.Tastes, preferences.tastes.toNames())
        storage.put(DishLabKeys.Equipment, preferences.equipment.toNames())
    }
}

private inline fun <reified T : Enum<T>> Set<String>?.toEnumSet(): Set<T> =
    orEmpty().mapNotNull { value -> enumValues<T>().firstOrNull { it.name == value } }.toSet()

private fun Set<Enum<*>>.toNames(): Set<String> = mapTo(mutableSetOf()) { it.name }
