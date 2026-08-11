package com.d1onix.dishlab.feature.scanner.presentation

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.d1onix.dishlab.designsystem.anim.MiseEasing
import com.d1onix.dishlab.designsystem.anim.rememberPulse
import com.d1onix.dishlab.designsystem.anim.rememberSweep
import com.d1onix.dishlab.designsystem.anim.screenIn
import com.d1onix.dishlab.designsystem.component.MiseGhostButton
import com.d1onix.dishlab.designsystem.component.MiseIconCircleButton
import com.d1onix.dishlab.designsystem.component.MisePrimaryButton
import com.d1onix.dishlab.designsystem.component.MiseSearchField
import com.d1onix.dishlab.designsystem.component.MiseTextAction
import com.d1onix.dishlab.designsystem.component.ScoreRing
import com.d1onix.dishlab.designsystem.component.SectionLabel
import com.d1onix.dishlab.designsystem.component.VerdictBadge
import com.d1onix.dishlab.designsystem.icon.MiseIcons
import com.d1onix.dishlab.designsystem.theme.MiseTheme
import com.d1onix.dishlab.domain.model.Product
import com.d1onix.dishlab.domain.model.ProductDataOrigin
import com.d1onix.dishlab.domain.model.ScoreVerdict
import com.d1onix.dishlab.feature.scanner.resources.Res
import com.d1onix.dishlab.feature.scanner.resources.scan_back
import com.d1onix.dishlab.feature.scanner.resources.scan_camera_denied
import com.d1onix.dishlab.feature.scanner.resources.scan_camera_starting
import com.d1onix.dishlab.feature.scanner.resources.scan_front_camera_note
import com.d1onix.dishlab.feature.scanner.resources.scan_hint_secondary
import com.d1onix.dishlab.feature.scanner.resources.scan_phase_failed
import com.d1onix.dishlab.feature.scanner.resources.scan_phase_resolving
import com.d1onix.dishlab.feature.scanner.resources.scan_phase_searching
import com.d1onix.dishlab.feature.scanner.resources.scan_step_lookup
import com.d1onix.dishlab.feature.scanner.resources.scan_step_read
import com.d1onix.dishlab.feature.scanner.resources.scan_step_result
import com.d1onix.dishlab.feature.scanner.resources.scan_switch_camera
import com.d1onix.dishlab.feature.scanner.resources.scan_torch_off
import com.d1onix.dishlab.feature.scanner.resources.scan_torch_on
import com.d1onix.dishlab.feature.scanner.resources.scan_manual_cancel
import com.d1onix.dishlab.feature.scanner.resources.scan_manual_entry
import com.d1onix.dishlab.feature.scanner.resources.scan_manual_placeholder
import com.d1onix.dishlab.feature.scanner.resources.scan_browse_recipes
import com.d1onix.dishlab.feature.scanner.resources.scan_manual_submit
import com.d1onix.dishlab.feature.scanner.resources.scan_review_add
import com.d1onix.dishlab.feature.scanner.resources.scan_review_alternatives
import com.d1onix.dishlab.feature.scanner.resources.scan_review_incomplete
import com.d1onix.dishlab.feature.scanner.resources.scan_review_nutrition
import com.d1onix.dishlab.feature.scanner.resources.scan_review_open_graph
import com.d1onix.dishlab.feature.scanner.resources.scan_review_question
import com.d1onix.dishlab.feature.scanner.resources.scan_review_compare_another
import com.d1onix.dishlab.feature.scanner.resources.scan_review_skip
import com.d1onix.dishlab.feature.scanner.resources.scan_review_title
import com.d1onix.dishlab.feature.scanner.resources.scan_server_unavailable
import com.d1onix.dishlab.feature.scanner.resources.scan_server_fallback_body
import com.d1onix.dishlab.feature.scanner.resources.scan_server_fallback_title
import com.d1onix.dishlab.feature.scanner.resources.scan_server_retry
import com.d1onix.dishlab.feature.scanner.resources.scan_title
import com.d1onix.dishlab.feature.scanner.resources.scan_title_resolving
import com.kashif.cameraK.permissions.providePermissions
import org.jetbrains.compose.resources.stringResource
import coil3.compose.AsyncImage
import kotlin.math.PI
import kotlin.math.sin

@Composable
fun ScanScreen(viewModel: ScanViewModel, showBackNavigation: Boolean = false) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    ScanContent(
        state = state,
        onAction = viewModel::onAction,
        showBackNavigation = showBackNavigation,
        // The camera is a platform concern, injected as a slot so the content
        // itself stays renderable in a preview.
        cameraPreview = { controls, dispatch -> CameraLayer(controls, dispatch) },
    )
}

@Composable
internal fun ScanContent(
    state: ScanUiState,
    onAction: (ScanAction) -> Unit,
    showBackNavigation: Boolean = false,
    modifier: Modifier = Modifier,
    cameraPreview: @Composable (CameraControls, (ScanAction) -> Unit) -> Unit = { _, _ -> },
) {
    val colors = MiseTheme.colors

    state.reviewedProduct?.let { product ->
        ScannedProductReview(
            product = product,
            alreadyAdded = state.reviewedProductAlreadyAdded,
            usedDeviceFallback = product.dataOrigin == ProductDataOrigin.DeviceFallback,
            onAction = onAction,
            modifier = modifier,
        )
        return
    }

    Box(modifier.fillMaxSize().background(colors.backgroundDeep)) {
        cameraPreview(state.camera, onAction)

        // The chrome sits on live video, so it needs its own contrast floor.
        Box(Modifier.fillMaxSize().background(viewfinderScrim()))

        Column(
            Modifier
                .fillMaxSize()
                .safeDrawingPadding()
                .padding(horizontal = 22.dp, vertical = 20.dp)
                .screenIn(),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            RootScannerTopBar(
                isResolving = state.isResolving,
                showBackNavigation = showBackNavigation,
                onBackClick = { onAction(ScanAction.BackClicked) },
            )

            Spacer(Modifier.weight(1f))

            ScanReticle(phase = state.phase)

            Spacer(Modifier.height(26.dp))
            ScanProgressTrail(phase = state.phase, modifier = Modifier.fillMaxWidth())

            Spacer(Modifier.height(18.dp))
            ScanStatus(state = state, onAction = onAction)

            Spacer(Modifier.weight(1f))

            if (state.camera.canToggleTorch || state.camera.canSwitchFacing) {
                CameraControlBar(controls = state.camera, onAction = onAction)
                Spacer(Modifier.height(16.dp))
            }

            if (state.manualEntryVisible) {
                MiseSearchField(
                    value = state.manualBarcode,
                    onValueChange = { onAction(ScanAction.ManualBarcodeChanged(it)) },
                    placeholder = stringResource(Res.string.scan_manual_placeholder),
                    modifier = Modifier.fillMaxWidth(),
                )
                Spacer(Modifier.height(12.dp))
                MisePrimaryButton(
                    text = stringResource(Res.string.scan_manual_submit),
                    onClick = { onAction(ScanAction.ManualBarcodeSubmitted) },
                    enabled = state.canSubmitManualBarcode,
                    modifier = Modifier.fillMaxWidth(),
                )
                Spacer(Modifier.height(10.dp))
                MiseGhostButton(
                    text = stringResource(Res.string.scan_manual_cancel),
                    onClick = { onAction(ScanAction.ManualEntryToggled) },
                    modifier = Modifier.fillMaxWidth(),
                )
            } else {
                MiseGhostButton(
                    text = stringResource(Res.string.scan_manual_entry),
                    onClick = { onAction(ScanAction.ManualEntryToggled) },
                    modifier = Modifier.fillMaxWidth(),
                )
                Spacer(Modifier.height(8.dp))
                // This is navigation, not an alternative way to scan. Keep it
                // thumb-reachable but visually quieter than scan controls.
                MiseTextAction(
                    text = stringResource(Res.string.scan_browse_recipes),
                    onClick = { onAction(ScanAction.RecipesClicked) },
                    modifier = Modifier.align(Alignment.End),
                )
            }
        }
    }
}

internal data class ScannerTopBarModel(val hasNavigationIcon: Boolean)

internal fun rootScannerTopBarModel(showBackNavigation: Boolean) =
    ScannerTopBarModel(hasNavigationIcon = showBackNavigation)

@Composable
private fun RootScannerTopBar(
    isResolving: Boolean,
    showBackNavigation: Boolean,
    onBackClick: () -> Unit,
) {
    val colors = MiseTheme.colors
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        if (showBackNavigation) {
            MiseIconCircleButton(
                icon = MiseIcons.ChevronLeft,
                contentDescription = stringResource(Res.string.scan_back),
                onClick = onBackClick,
            )
        }
        Box(Modifier.weight(1f), contentAlignment = Alignment.Center) {
            SectionLabel(
                text = if (isResolving) stringResource(Res.string.scan_title_resolving)
                else stringResource(Res.string.scan_title),
                color = colors.textMuted,
            )
        }
    }
}

@Composable
private fun ScannedProductReview(
    product: Product,
    alreadyAdded: Boolean,
    usedDeviceFallback: Boolean,
    onAction: (ScanAction) -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = MiseTheme.colors
    val scoreColor = when (product.verdict) {
        ScoreVerdict.Buy -> colors.lime
        ScoreVerdict.Maybe -> colors.amber
        ScoreVerdict.Skip -> colors.red
    }

    Column(
        modifier
            .fillMaxSize()
            .background(colors.background)
            .safeDrawingPadding()
            .screenIn(),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            MiseIconCircleButton(
                icon = MiseIcons.ChevronLeft,
                contentDescription = stringResource(Res.string.scan_back),
                onClick = { onAction(ScanAction.ReviewBackClicked) },
            )
            SectionLabel(
                text = stringResource(Res.string.scan_review_title),
                modifier = Modifier.padding(start = 14.dp),
            )
        }

        LazyColumn(
            modifier = Modifier.weight(1f),
            contentPadding = PaddingValues(horizontal = 20.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(18.dp),
        ) {
            if (usedDeviceFallback) {
                item {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(colors.amber.copy(alpha = 0.10f), RoundedCornerShape(8.dp))
                            .border(1.dp, colors.amber.copy(alpha = 0.32f), RoundedCornerShape(8.dp))
                            .padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp),
                    ) {
                        Text(
                            text = stringResource(Res.string.scan_server_fallback_title),
                            style = MiseTheme.typography.monoSmall,
                            color = colors.amber,
                        )
                        Text(
                            text = stringResource(Res.string.scan_server_fallback_body),
                            style = MiseTheme.typography.bodySmall,
                            color = colors.textMuted,
                        )
                    }
                }
            }
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(scoreColor.copy(alpha = 0.08f), RoundedCornerShape(8.dp))
                        .border(
                            1.dp,
                            scoreColor.copy(alpha = 0.4f),
                            RoundedCornerShape(8.dp),
                        )
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    ScoreRing(score = product.score, color = scoreColor, size = 68, strokeWidth = 6) {
                        Text(
                            text = product.score.toString(),
                            style = MiseTheme.typography.mono,
                            color = scoreColor,
                        )
                    }
                    Column(Modifier.weight(1f).padding(horizontal = 16.dp)) {
                        Text(product.name, style = MiseTheme.typography.title, color = colors.text)
                        Text(
                            product.category,
                            style = MiseTheme.typography.monoSmall,
                            color = colors.textMuted,
                        )
                    }
                    VerdictBadge(product.verdict.label, scoreColor)
                }
            }

            if (!product.hasCompleteData) {
                item {
                    Text(
                        text = stringResource(Res.string.scan_review_incomplete),
                        style = MiseTheme.typography.monoSmall,
                        color = colors.amber,
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(colors.amber.copy(alpha = 0.10f), RoundedCornerShape(8.dp))
                            .border(
                                1.dp,
                                colors.amber.copy(alpha = 0.3f),
                                RoundedCornerShape(8.dp),
                            )
                            .padding(12.dp),
                    )
                }
            }

            product.details.imageUrl.trim().takeIf { it.startsWith("https://") }?.let { imageUrl ->
                item {
                    Box(
                        Modifier
                            .fillMaxWidth()
                            .height(204.dp)
                            .clip(RoundedCornerShape(18.dp))
                            .background(colors.surface),
                        contentAlignment = Alignment.Center,
                    ) {
                        AsyncImage(
                            model = imageUrl,
                            contentDescription = product.name,
                            contentScale = ContentScale.Fit,
                            modifier = Modifier.fillMaxSize().padding(12.dp),
                        )
                    }
                }
            }

            item {
                Text(
                    text = product.summary,
                    style = MiseTheme.typography.body,
                    color = colors.text.copy(alpha = 0.82f),
                )
            }

            product.scanDetails().forEach { (title, value) ->
                item { ScannedDetailTile(title, value) }
            }

            if (product.nutrients.isNotEmpty()) {
                item { SectionLabel(stringResource(Res.string.scan_review_nutrition)) }
                product.nutrients.chunked(3).forEach { nutrients ->
                    item {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            nutrients.forEach { nutrient ->
                                Column(
                                    modifier = Modifier
                                        .weight(1f)
                                        .background(colors.panel, RoundedCornerShape(8.dp))
                                        .border(1.dp, colors.border, RoundedCornerShape(8.dp))
                                        .padding(12.dp),
                                ) {
                                    Text(
                                        nutrient.name.uppercase(),
                                        style = MiseTheme.typography.monoTiny,
                                        color = colors.textMuted,
                                    )
                                    Row(verticalAlignment = Alignment.Bottom) {
                                        Text(
                                            nutrient.amount,
                                            style = MiseTheme.typography.titleSmall,
                                            color = colors.text,
                                        )
                                        Text(
                                            nutrient.unit,
                                            style = MiseTheme.typography.monoTiny,
                                            color = colors.textMuted,
                                            modifier = Modifier.padding(start = 2.dp, bottom = 1.dp),
                                        )
                                    }
                                }
                            }
                            repeat(3 - nutrients.size) { Spacer(Modifier.weight(1f)) }
                        }
                    }
                }
            }

            if (product.alternatives.isNotEmpty()) {
                item { SectionLabel(stringResource(Res.string.scan_review_alternatives)) }
                items(product.alternatives.size) { index ->
                    val alternative = product.alternatives[index]
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(colors.lime.copy(alpha = 0.07f), RoundedCornerShape(8.dp))
                            .border(
                                1.dp,
                                colors.lime.copy(alpha = 0.2f),
                                RoundedCornerShape(8.dp),
                            )
                            .padding(horizontal = 14.dp, vertical = 11.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(alternative.name, style = MiseTheme.typography.body, color = colors.text)
                        Text(
                            alternative.score.toString(),
                            style = MiseTheme.typography.mono,
                            color = colors.lime,
                        )
                    }
                }
            }
        }

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(colors.surface)
                .border(1.dp, colors.border)
                .padding(horizontal = 20.dp, vertical = 16.dp),
        ) {
            Text(
                text = stringResource(Res.string.scan_review_question),
                style = MiseTheme.typography.bodySmall,
                color = colors.textMuted,
                modifier = Modifier.padding(bottom = 10.dp),
            )
            MisePrimaryButton(
                text = stringResource(
                    if (alreadyAdded) {
                        Res.string.scan_review_open_graph
                    } else {
                        Res.string.scan_review_add
                    }
                ),
                onClick = { onAction(ScanAction.AddReviewedProductClicked) },
                modifier = Modifier.fillMaxWidth(),
            )
            Spacer(Modifier.height(8.dp))
            MiseGhostButton(
                text = stringResource(Res.string.scan_review_compare_another),
                onClick = { onAction(ScanAction.CompareWithAnotherClicked) },
                modifier = Modifier.fillMaxWidth(),
            )
            Spacer(Modifier.height(8.dp))
            MiseGhostButton(
                text = stringResource(Res.string.scan_review_skip),
                onClick = { onAction(ScanAction.ReviewedProductSkipped) },
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}

@Composable
private fun ScannedDetailTile(title: String, value: String) {
    val colors = MiseTheme.colors
    Column(
        Modifier
            .fillMaxWidth()
            .background(colors.panel, RoundedCornerShape(10.dp))
            .border(1.dp, colors.border, RoundedCornerShape(10.dp))
            .padding(12.dp),
    ) {
        Text(title.uppercase(), style = MiseTheme.typography.monoTiny, color = colors.textMuted)
        Spacer(Modifier.height(4.dp))
        Text(value, style = MiseTheme.typography.bodySmall, color = colors.text)
    }
}

private fun Product.scanDetails(): List<Pair<String, String>> = buildList {
    fun String.clean() = trim().takeIf { it.isNotEmpty() && !it.equals("null", ignoreCase = true) }
    fun List<String>.tags() = mapNotNull { it.clean()?.substringAfter(':')?.replace('-', ' ') }
        .joinToString().takeIf(String::isNotBlank)

    details.brand.clean()?.let { add("Brand" to it) }
    details.quantity.clean()?.let { add("Quantity" to it) }
    details.servingSize.clean()?.let { add("Serving size" to it) }
    details.ingredientsText.clean()?.let { add("Ingredients" to it) }
    details.allergens.tags()?.let { add("Allergens" to it) }
    details.categories.tags()?.let { add("Categories" to it) }
    details.labels.tags()?.let { add("Labels" to it) }
    details.nutriScore.clean()?.uppercase()?.takeIf { it in setOf("A", "B", "C", "D", "E") }
        ?.let { add("Nutri-Score" to "$it · A is better, E is lower") }
    details.novaGroup?.takeIf { it in 1..4 }?.let { group ->
        add("NOVA $group" to when (group) {
            1 -> "Unprocessed or minimally processed food"
            2 -> "Processed culinary ingredient"
            3 -> "Processed food"
            else -> "Ultra-processed food"
        })
    }
    details.ecoScore.clean()?.uppercase()?.takeIf { it in setOf("A", "B", "C", "D", "E") }
        ?.let { add("Eco-Score" to it) }
}

/**
 * Camera preview kept out of the content composable so previews and manual
 * barcode entry work even when a camera is unavailable.
 */
@Composable
private fun CameraLayer(controls: CameraControls, onAction: (ScanAction) -> Unit) {
    val permissions = providePermissions()
    var granted by remember { mutableStateOf(permissions.hasCameraPermission()) }
    var denied by remember { mutableStateOf(false) }

    if (!granted && !denied) {
        permissions.RequestCameraPermission(
            onGranted = { granted = true },
            onDenied = { denied = true },
        )
    }

    if (!granted) {
        CameraUnavailable(permissionDenied = denied)
        return
    }

    ProductBarcodeCamera(
        facing = controls.facing,
        torchOn = controls.torchOn,
        onBarcodeDetected = { onAction(ScanAction.BarcodeDetected(it)) },
        onCapabilitiesChanged = { onAction(ScanAction.CameraCapabilitiesChanged(it)) },
        modifier = Modifier.fillMaxSize(),
    )
}

@Composable
private fun CameraUnavailable(permissionDenied: Boolean) {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Text(
            text = if (permissionDenied) {
                stringResource(Res.string.scan_camera_denied)
            } else {
                stringResource(Res.string.scan_camera_starting)
            },
            style = MiseTheme.typography.monoSmall,
            color = MiseTheme.colors.textFaint,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(horizontal = 40.dp),
        )
    }
}

/** Darkens the top and bottom of the video so the chrome keeps its contrast. */
@Composable
private fun viewfinderScrim(): Brush = Brush.verticalGradient(
    0f to Color.Black.copy(alpha = 0.55f),
    0.30f to Color.Transparent,
    0.66f to Color.Transparent,
    1f to Color.Black.copy(alpha = 0.72f),
)

/**
 * The 250dp reticle. Everything about it is derived from [phase], so the frame
 * itself reports what the scanner is doing: lime and sweeping while it hunts,
 * snapped wide and still the moment a code is locked, cyan with a spinner while
 * the lookup runs, red when it failed.
 */
@Composable
private fun ScanReticle(phase: ScanPhase, modifier: Modifier = Modifier) {
    val colors = MiseTheme.colors
    val accent by animateColorAsState(
        targetValue = when (phase) {
            ScanPhase.Searching -> colors.lime
            ScanPhase.Resolving -> colors.cyan
            ScanPhase.Failed -> colors.red
        },
        animationSpec = tween(durationMillis = 320, easing = MiseEasing),
        label = "reticleAccent",
    )
    // Corners stretch on lock — the cheapest way to make «got it» feel physical.
    val cornerFraction by animateFloatAsState(
        targetValue = if (phase == ScanPhase.Searching) 0.14f else 0.28f,
        animationSpec = tween(durationMillis = 340, easing = MiseEasing),
        label = "reticleCorner",
    )
    val barAlpha by animateFloatAsState(
        targetValue = if (phase == ScanPhase.Searching) 1f else 0.25f,
        animationSpec = tween(durationMillis = 340, easing = MiseEasing),
        label = "reticleBars",
    )

    val sweep = rememberSweep(durationMillis = 2400, label = "scanLine")
    val pulse = rememberPulse(durationMillis = 2400, from = 0.25f, to = 0.6f, label = "scanPulse")
    val spin = rememberSweep(durationMillis = 1100, label = "scanSpin")

    Canvas(modifier.size(250.dp)) {
        val corner = size.minDimension * cornerFraction
        val stroke = 3.dp.toPx()
        val inset = 24.dp.toPx()

        listOf(
            Offset(0f, 0f) to listOf(Offset(corner, 0f), Offset(0f, corner)),
            Offset(size.width, 0f) to listOf(Offset(size.width - corner, 0f), Offset(size.width, corner)),
            Offset(0f, size.height) to listOf(Offset(corner, size.height), Offset(0f, size.height - corner)),
            Offset(size.width, size.height) to listOf(
                Offset(size.width - corner, size.height),
                Offset(size.width, size.height - corner),
            ),
        ).forEach { (origin, ends) ->
            ends.forEach { end ->
                drawLine(accent, origin, end, strokeWidth = stroke, cap = StrokeCap.Round)
            }
        }

        // The decorative barcode behind the line, faded out once a real code won.
        val bars = listOf(8, 3, 5, 2, 7, 3, 9, 2, 4, 6, 3, 8, 2, 5, 3, 7)
        var x = inset + 20.dp.toPx()
        bars.forEach { width ->
            drawRect(
                color = accent,
                topLeft = Offset(x, size.height / 2 - 45.dp.toPx()),
                size = Size(width.dp.toPx() * 0.7f, 90.dp.toPx()),
                alpha = pulse.value * barAlpha,
            )
            x += (width + 3).dp.toPx() * 0.7f
        }

        if (phase == ScanPhase.Searching) {
            // `sin` turns the linear sweep into the prototype's back-and-forth line.
            val travel = sin(sweep.value * 2f * PI.toFloat()) * (size.height / 2 - inset)
            val y = size.height / 2 + travel
            drawLine(
                brush = Brush.horizontalGradient(
                    listOf(Color.Transparent, accent, Color.Transparent),
                ),
                start = Offset(inset, y),
                end = Offset(size.width - inset, y),
                strokeWidth = 2.dp.toPx(),
            )
        }

        if (phase == ScanPhase.Resolving) {
            val radius = size.minDimension * 0.17f
            drawArc(
                color = accent,
                startAngle = spin.value * 360f,
                sweepAngle = 96f,
                useCenter = false,
                topLeft = Offset(center.x - radius, center.y - radius),
                size = Size(radius * 2, radius * 2),
                style = Stroke(width = 3.dp.toPx(), cap = StrokeCap.Round),
            )
        }
    }
}

/**
 * Read → Look up → Result. The scan used to jump from an idle animation straight
 * to a product sheet; this is the part that says which of those steps is running.
 */
@Composable
private fun ScanProgressTrail(phase: ScanPhase, modifier: Modifier = Modifier) {
    val colors = MiseTheme.colors
    val steps = listOf(
        stringResource(Res.string.scan_step_read),
        stringResource(Res.string.scan_step_lookup),
        stringResource(Res.string.scan_step_result),
    )
    val activeIndex = when (phase) {
        ScanPhase.Searching -> -1
        ScanPhase.Resolving, ScanPhase.Failed -> 1
    }
    val accent = if (phase == ScanPhase.Failed) colors.red else colors.lime

    Row(modifier, verticalAlignment = Alignment.CenterVertically) {
        steps.forEachIndexed { index, label ->
            if (index > 0) {
                Box(
                    Modifier
                        .weight(1f)
                        .padding(horizontal = 6.dp)
                        .height(1.dp)
                        .background(if (index <= activeIndex) accent else colors.border),
                )
            }
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                val done = index < activeIndex
                val active = index == activeIndex
                Box(
                    Modifier
                        .size(if (active) 9.dp else 7.dp)
                        .clip(CircleShape)
                        .background(if (done || active) accent else colors.borderStrong),
                )
                Spacer(Modifier.height(6.dp))
                Text(
                    text = label,
                    style = MiseTheme.typography.monoTiny,
                    color = when {
                        active -> accent
                        done -> colors.textMuted
                        else -> colors.textDim
                    },
                )
            }
        }
    }
}

/** The line of text under the trail: what is happening, and the digits it read. */
@Composable
private fun ScanStatus(
    state: ScanUiState,
    onAction: (ScanAction) -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = MiseTheme.colors
    Column(modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = when (state.phase) {
                ScanPhase.Searching -> stringResource(Res.string.scan_phase_searching)
                ScanPhase.Resolving -> stringResource(Res.string.scan_phase_resolving)
                ScanPhase.Failed -> stringResource(Res.string.scan_phase_failed)
            },
            style = MiseTheme.typography.bodyLarge,
            color = if (state.phase == ScanPhase.Failed) colors.red else colors.text.copy(alpha = 0.85f),
            textAlign = TextAlign.Center,
        )

        // Showing the digits is the proof that the camera actually read something.
        state.visibleBarcode?.let { barcode ->
            Spacer(Modifier.height(8.dp))
            Text(
                text = barcode,
                style = MiseTheme.typography.mono,
                color = colors.text,
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .background(colors.panelStrong)
                    .padding(horizontal = 12.dp, vertical = 6.dp),
            )
        }

        if (state.phase == ScanPhase.Searching) {
            Spacer(Modifier.height(6.dp))
            Text(
                text = stringResource(Res.string.scan_hint_secondary),
                style = MiseTheme.typography.monoSmall,
                color = colors.textFaint,
                textAlign = TextAlign.Center,
            )
        }

        if (state.camera.facing == CameraFacing.Front) {
            Spacer(Modifier.height(8.dp))
            Text(
                text = stringResource(Res.string.scan_front_camera_note),
                style = MiseTheme.typography.monoSmall,
                color = colors.amber,
                textAlign = TextAlign.Center,
            )
        }

        if (state.resolutionFailed) {
            Spacer(Modifier.height(10.dp))
            Text(
                text = stringResource(Res.string.scan_server_unavailable),
                style = MiseTheme.typography.bodySmall,
                color = colors.red,
                textAlign = TextAlign.Center,
            )
            Spacer(Modifier.height(10.dp))
            MiseGhostButton(
                text = stringResource(Res.string.scan_server_retry),
                onClick = { onAction(ScanAction.RetryResolutionClicked) },
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}

/** Torch and lens, shown only once the camera has confirmed it supports them. */
@Composable
private fun CameraControlBar(
    controls: CameraControls,
    onAction: (ScanAction) -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = MiseTheme.colors
    Row(
        modifier,
        horizontalArrangement = Arrangement.spacedBy(14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (controls.canToggleTorch) {
            MiseIconCircleButton(
                icon = if (controls.torchOn) MiseIcons.Flash else MiseIcons.FlashOff,
                contentDescription = stringResource(
                    if (controls.torchOn) Res.string.scan_torch_off else Res.string.scan_torch_on,
                ),
                onClick = { onAction(ScanAction.TorchToggled) },
                size = 48,
                iconSize = 19,
                tint = if (controls.torchOn) colors.onLime else colors.text,
                borderColor = if (controls.torchOn) colors.lime else colors.borderStrong,
                background = if (controls.torchOn) colors.lime else colors.panel,
            )
        }
        if (controls.canSwitchFacing) {
            MiseIconCircleButton(
                icon = MiseIcons.CameraFlip,
                contentDescription = stringResource(Res.string.scan_switch_camera),
                onClick = { onAction(ScanAction.CameraFacingToggled) },
                size = 48,
                iconSize = 19,
                background = colors.panel,
            )
        }
    }
}
