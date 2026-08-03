package com.dishlab.domain.model

import java.util.UUID

enum class FoodOrigin {
    ANIMAL,
    PLANT,
    FUNGI,
    ALGAE,
    MICROBIAL,
    MINERAL,
    SYNTHETIC,
    MIXED,
    UNKNOWN,
}

enum class FoodPreparationState {
    AS_SOLD,
    RAW,
    COOKED,
    BOILED,
    BAKED,
    ROASTED,
    FRIED,
    SMOKED,
    FERMENTED,
    DRIED,
    UNKNOWN,
}

enum class FoodPhysicalForm {
    WHOLE,
    SLICED,
    MINCED,
    GROUND,
    POWDER,
    LIQUID,
    PUREE,
    UNKNOWN,
}

enum class FoodCarbonation {
    STILL,
    LIGHTLY_SPARKLING,
    SPARKLING,
    HIGHLY_SPARKLING,
    NOT_APPLICABLE,
    UNKNOWN,
}

enum class FoodPreservation {
    FRESH,
    FROZEN,
    CHILLED,
    CANNED,
    PICKLED,
    SHELF_STABLE,
    UNKNOWN,
}

data class FoodConcept(
    val id: UUID,
    val canonicalName: String,
    val parentId: UUID? = null,
    val origin: FoodOrigin = FoodOrigin.UNKNOWN,
    val taxonomyVersion: Int = 1,
)

data class FoodVariant(
    val id: UUID,
    val conceptId: UUID,
    val canonicalName: String,
    val origin: FoodOrigin = FoodOrigin.UNKNOWN,
    val preparationState: FoodPreparationState = FoodPreparationState.AS_SOLD,
    val physicalForm: FoodPhysicalForm = FoodPhysicalForm.UNKNOWN,
    val carbonation: FoodCarbonation = FoodCarbonation.NOT_APPLICABLE,
    val preservation: FoodPreservation = FoodPreservation.UNKNOWN,
    val facets: Map<String, String> = emptyMap(),
)
