package com.d1onix.dishlab

import com.d1onix.dishlab.designsystem.preview.MiseComponentGalleryController
import com.d1onix.dishlab.feature.home.presentation.homePreviewController
import com.d1onix.dishlab.feature.products.presentation.graphPreviewController
import com.d1onix.dishlab.feature.products.presentation.graphSheetPreviewController
import com.d1onix.dishlab.feature.products.presentation.historyPreviewController
import com.d1onix.dishlab.feature.recipes.presentation.cookingPreviewController
import com.d1onix.dishlab.feature.recipes.presentation.recipeDetailPreviewController
import com.d1onix.dishlab.feature.recipes.presentation.recipeListPreviewController
import com.d1onix.dishlab.feature.scanner.presentation.scanManualEntryPreviewController
import com.d1onix.dishlab.feature.scanner.presentation.scanNotFoundPreviewController
import com.d1onix.dishlab.feature.scanner.presentation.scanPreviewController
import platform.UIKit.UIViewController

/**
 * The iOS preview catalogue, as seen from Swift.
 *
 * Feature modules are `implementation` dependencies of `:shared`, so their own
 * preview controllers are not part of the `Shared` framework. Listing them here
 * is what exports them, and it keeps the framework's public surface a
 * deliberate choice rather than everything every feature happens to declare.
 *
 * `iosApp/iosApp/Previews.swift` turns each of these into a SwiftUI `#Preview`.
 * None of them touch the DI graph: they render a fixed state, so opening the
 * canvas never starts a repository, a data store or the camera.
 */
object DishLabPreviews {

    fun home(): UIViewController = homePreviewController()

    fun scan(): UIViewController = scanPreviewController()

    fun scanManualEntry(): UIViewController = scanManualEntryPreviewController()

    fun scanNotFound(): UIViewController = scanNotFoundPreviewController()

    fun graph(): UIViewController = graphPreviewController()

    fun graphSheet(): UIViewController = graphSheetPreviewController()

    fun history(): UIViewController = historyPreviewController()

    fun recipeList(): UIViewController = recipeListPreviewController()

    fun recipeDetail(): UIViewController = recipeDetailPreviewController()

    fun cooking(): UIViewController = cookingPreviewController()

    fun componentGallery(): UIViewController = MiseComponentGalleryController()
}
