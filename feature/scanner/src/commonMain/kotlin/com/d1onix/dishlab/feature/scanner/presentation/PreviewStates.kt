package com.d1onix.dishlab.feature.scanner.presentation

import com.d1onix.dishlab.domain.model.Nutrient
import com.d1onix.dishlab.domain.model.Product
import com.d1onix.dishlab.domain.model.ProductAlternative
import com.d1onix.dishlab.domain.model.ProductId

/** See `HomePreviewStates` for why the fixtures are common and the previews are not. */
internal object ScanPreviewStates {

    val Idle = ScanUiState()

    val ManualEntry = ScanUiState(manualEntryVisible = true, manualBarcode = "4011200296908")

    /** A barcode is in flight — the overlay stops accepting new detections. */
    val Resolving = ScanUiState(isResolving = true)

    val ProductReview = ScanUiState(
        reviewedProduct = Product(
            id = ProductId("oats"),
            barcode = "4011200296908",
            name = "Rolled Oats",
            category = "Grains",
            score = 82,
            accentColor = 0xFFC8FF4D,
            initial = "O",
            nutrients = listOf(
                Nutrient("Protein", "13", "g"),
                Nutrient("Fiber", "10", "g"),
                Nutrient("Sugar", "1", "g"),
                Nutrient("Fat", "7", "g"),
            ),
            summary = "Whole-grain oats with high fiber and minimal added sugar.",
            hasCompleteData = true,
            alternatives = listOf(ProductAlternative("Steel-cut oats", 89)),
        )
    )
}

internal object ScanNotFoundPreviewStates {

    val Default = ScanNotFoundUiState(barcode = "000000000000")
}
