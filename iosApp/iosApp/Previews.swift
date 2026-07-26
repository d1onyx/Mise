import SwiftUI
import Shared

/// Hosts one Compose screen from `DishLabPreviews` in the SwiftUI canvas.
///
/// There is no Compose preview renderer on iOS, so an iOS preview is the real
/// framework running in the simulator — which is the point: this is where the
/// safe-area insets, the keyboard, and the actual font rasterisation show up.
/// Android's `@Preview` functions live in each feature's `androidMain` and
/// cover the per-state matrix instead; these cover the platform.
///
/// The controllers render fixed state and never build the DI graph, so opening
/// the canvas starts no repository, no data store and no camera.
private struct ComposePreview: UIViewControllerRepresentable {
    let controller: () -> UIViewController

    func makeUIViewController(context: Self.Context) -> UIViewController {
        controller()
    }

    func updateUIViewController(_ uiViewController: UIViewController, context: Self.Context) {}
}

#Preview("Home") {
    ComposePreview { DishLabPreviews.shared.home() }.ignoresSafeArea()
}

#Preview("Scan") {
    ComposePreview { DishLabPreviews.shared.scan() }.ignoresSafeArea()
}

#Preview("Scan · manual entry") {
    ComposePreview { DishLabPreviews.shared.scanManualEntry() }.ignoresSafeArea()
}

#Preview("Scan · not found") {
    ComposePreview { DishLabPreviews.shared.scanNotFound() }.ignoresSafeArea()
}

#Preview("Graph") {
    ComposePreview { DishLabPreviews.shared.graph() }.ignoresSafeArea()
}

#Preview("Graph · detail sheet") {
    ComposePreview { DishLabPreviews.shared.graphSheet() }.ignoresSafeArea()
}

#Preview("History") {
    ComposePreview { DishLabPreviews.shared.history() }.ignoresSafeArea()
}

#Preview("Recipes") {
    ComposePreview { DishLabPreviews.shared.recipeList() }.ignoresSafeArea()
}

#Preview("Recipe detail") {
    ComposePreview { DishLabPreviews.shared.recipeDetail() }.ignoresSafeArea()
}

#Preview("Cooking") {
    ComposePreview { DishLabPreviews.shared.cooking() }.ignoresSafeArea()
}

#Preview("Component gallery") {
    ComposePreview { DishLabPreviews.shared.componentGallery() }.ignoresSafeArea()
}
