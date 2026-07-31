package com.d1onix.dishlab.domain.model

data class UserSession(
    val isAuthenticated: Boolean = false,
    val onboardingCompleted: Boolean = false,
)

enum class DietPreference {
    Vegetarian,
    Vegan,
    Pescatarian,
    GlutenFree,
    LowCarb,
}

enum class AllergenPreference {
    Milk,
    Eggs,
    Peanuts,
    TreeNuts,
    Soy,
    Gluten,
    Fish,
    Shellfish,
}

enum class TastePreference {
    Savory,
    Spicy,
    Sweet,
    Sour,
    Smoky,
    Mild,
}

enum class KitchenEquipment {
    Oven,
    Microwave,
    Blender,
    AirFryer,
    Multicooker,
    Grill,
}

data class CookingPreferences(
    val diets: Set<DietPreference> = emptySet(),
    val allergens: Set<AllergenPreference> = emptySet(),
    val tastes: Set<TastePreference> = emptySet(),
    val equipment: Set<KitchenEquipment> = emptySet(),
)
