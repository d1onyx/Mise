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
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.d1onix.dishlab.designsystem.anim.MiseEasing
import com.d1onix.dishlab.designsystem.anim.rememberSweep
import com.d1onix.dishlab.designsystem.anim.screenIn
import com.d1onix.dishlab.designsystem.component.EmptyState
import com.d1onix.dishlab.designsystem.component.MiseFact
import com.d1onix.dishlab.designsystem.component.MiseFactList
import com.d1onix.dishlab.designsystem.component.MiseGhostButton
import com.d1onix.dishlab.designsystem.component.MiseIconCircleButton
import com.d1onix.dishlab.designsystem.component.MiseGradeScoreScale
import com.d1onix.dishlab.designsystem.component.MisePrimaryButton
import com.d1onix.dishlab.designsystem.component.MiseSearchField
import com.d1onix.dishlab.designsystem.component.MiseTabPager
import com.d1onix.dishlab.designsystem.component.MiseTextAction
import com.d1onix.dishlab.designsystem.component.ScoreRing
import com.d1onix.dishlab.designsystem.component.SectionLabel
import com.d1onix.dishlab.designsystem.component.VerdictBadge
import com.d1onix.dishlab.designsystem.component.ZoomableProductImage
import com.d1onix.dishlab.designsystem.icon.MiseIcons
import com.d1onix.dishlab.designsystem.theme.MiseTheme
import com.d1onix.dishlab.domain.model.Product
import com.d1onix.dishlab.domain.model.ProductPackagingComponent
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
import com.d1onix.dishlab.feature.scanner.resources.scan_nutri_score
import com.d1onix.dishlab.feature.scanner.resources.scan_nutri_score_hint
import com.d1onix.dishlab.feature.scanner.resources.scan_eco_score
import com.d1onix.dishlab.feature.scanner.resources.scan_eco_score_hint
import com.d1onix.dishlab.feature.scanner.resources.scan_review_add
import com.d1onix.dishlab.feature.scanner.resources.scan_review_alternatives
import com.d1onix.dishlab.feature.scanner.resources.scan_review_incomplete
import com.d1onix.dishlab.feature.scanner.resources.scan_review_open_graph
import com.d1onix.dishlab.feature.scanner.resources.scan_review_question
import com.d1onix.dishlab.feature.scanner.resources.scan_review_compare_another
import com.d1onix.dishlab.feature.scanner.resources.scan_review_skip
import com.d1onix.dishlab.feature.scanner.resources.scan_review_title
import com.d1onix.dishlab.feature.scanner.resources.scan_no_extra_photos
import com.d1onix.dishlab.feature.scanner.resources.scan_not_set
import com.d1onix.dishlab.feature.scanner.resources.scan_nutrients_per
import com.d1onix.dishlab.feature.scanner.resources.scan_server_unavailable
import com.d1onix.dishlab.feature.scanner.resources.scan_server_retry
import com.d1onix.dishlab.feature.scanner.resources.scan_tab_composition
import com.d1onix.dishlab.feature.scanner.resources.scan_tab_nutrition
import com.d1onix.dishlab.feature.scanner.resources.scan_tab_overview
import com.d1onix.dishlab.feature.scanner.resources.scan_tab_photos
import com.d1onix.dishlab.feature.scanner.resources.scan_title
import com.d1onix.dishlab.feature.scanner.resources.scan_title_resolving
import com.d1onix.dishlab.feature.scanner.resources.scan_loading_fact_barcodes
import com.d1onix.dishlab.feature.scanner.resources.scan_loading_fact_data
import com.d1onix.dishlab.feature.scanner.resources.scan_loading_fact_graph
import com.d1onix.dishlab.feature.scanner.resources.scan_loading_fact_nutrition
import com.kashif.cameraK.permissions.providePermissions
import org.jetbrains.compose.resources.stringResource
import kotlinx.coroutines.delay

@Composable
fun ScanScreen(viewModel: ScanViewModel, showBackNavigation: Boolean = false) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    ScanContent(
        state = state,
        onAction = viewModel::onAction,
        showBackNavigation = showBackNavigation,
        // The camera is a platform concern, injected as a slot so the content
        // itself stays renderable in a preview.
        cameraPreview = { controls, active, dispatch -> CameraLayer(controls, active, dispatch) },
    )
}

@Composable
internal fun ScanContent(
    state: ScanUiState,
    onAction: (ScanAction) -> Unit,
    showBackNavigation: Boolean = false,
    modifier: Modifier = Modifier,
    cameraPreview: @Composable (CameraControls, Boolean, (ScanAction) -> Unit) -> Unit = { _, _, _ -> },
) {
    val colors = MiseTheme.colors

    state.reviewedProduct?.let { product ->
        ScannedProductReview(
            product = product,
            alreadyAdded = state.reviewedProductAlreadyAdded,
            onAction = onAction,
            modifier = modifier,
        )
        return
    }

    if (state.isResolving) {
        ResolvingScanContent(
            state = state,
            showBackNavigation = showBackNavigation,
            onAction = onAction,
            modifier = modifier,
        )
        return
    }

    // A barcode is a small, flat thing — it never needed the full screen as a
    // viewfinder. Only the top half is live camera; the bottom half is a
    // plain panel of controls, not a floating overlay on top of the feed.
    // Typing a barcode needs neither: the camera goes away entirely and the
    // panel takes the whole screen, leaving room for the keyboard.
    Column(modifier.fillMaxSize().background(colors.backgroundDeep)) {
        // The camera composable stays mounted across the manual-entry toggle
        // instead of leaving composition — unbinding and rebinding CameraX
        // (plus closing/recreating the ML Kit scanner) on every keystroke
        // sheet open/close raced with in-flight analysis frames and could
        // freeze the app. Manual entry instead shrinks this box to zero and
        // tells the camera to stop analyzing, which is instant and safe.
        //
        // clipToBounds is the belt to COMPATIBLE's suspenders: even a
        // TextureView-backed preview can be measured a pixel taller than its
        // Compose bounds during a resize, and without a hard clip that
        // sliver of live feed shows past the seam into the panel below.
        Box(
            if (state.manualEntryVisible) {
                Modifier.size(0.dp)
            } else {
                Modifier.weight(1f).fillMaxWidth().clipToBounds()
            },
        ) {
            cameraPreview(state.camera, !state.manualEntryVisible, onAction)

            if (!state.manualEntryVisible) {
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

                    // Smaller than the old full-screen reticle: this box is
                    // now only half the display, so the frame has to fit
                    // alongside the top bar and status text without clipping.
                    ScanReticle(phase = state.phase, reticleSize = 190.dp)

                    Spacer(Modifier.height(16.dp))
                    ScanProgressTrail(phase = state.phase, modifier = Modifier.fillMaxWidth())

                    Spacer(Modifier.height(12.dp))
                    ScanStatus(state = state, onAction = onAction)

                    Spacer(Modifier.weight(1f))
                }
            }
        }

        ScannerControlPanel(
            state = state,
            onAction = onAction,
            modifier = Modifier.weight(1f).fillMaxWidth(),
        )
    }
}

/**
 * Everything below the viewfinder. Torch and lens-flip sit as labelled hints
 * in the top corners rather than a bottom-nav-style bar, since they are
 * occasional actions, not the primary flow through this screen. They step
 * aside while manual entry is open so the keyboard flow never has to share
 * the panel with them.
 */
@Composable
private fun ScannerControlPanel(
    state: ScanUiState,
    onAction: (ScanAction) -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = MiseTheme.colors
    Box(
        modifier
            .background(colors.surface)
            .then(
                // Only a straight top rule — no sides, no rounding. It's the
                // seam with the camera above; the panel expands to fill the
                // whole screen for manual entry, so there's no seam to mark.
                if (state.manualEntryVisible) {
                    Modifier
                } else {
                    Modifier.drawBehind {
                        // Fading mint accent instead of a flat hairline —
                        // it reads as a deliberate seam, not a stray rule.
                        drawLine(
                            brush = Brush.horizontalGradient(
                                listOf(Color.Transparent, colors.mint.copy(alpha = 0.7f), Color.Transparent),
                            ),
                            start = Offset.Zero,
                            end = Offset(size.width, 0f),
                            strokeWidth = 2.dp.toPx(),
                        )
                    }
                },
            )
            .safeDrawingPadding()
            // Only relevant once the panel fills the screen for manual entry:
            // keeps the field and buttons clear of the keyboard instead of
            // sitting underneath it.
            .imePadding()
            .padding(horizontal = 22.dp, vertical = 20.dp)
            .verticalScroll(rememberScrollState()),
    ) {
        if (!state.manualEntryVisible) {
            if (state.camera.canSwitchFacing) {
                ScanHintButton(
                    icon = MiseIcons.CameraFlip,
                    label = stringResource(Res.string.scan_switch_camera),
                    onClick = { onAction(ScanAction.CameraFacingToggled) },
                    modifier = Modifier.align(Alignment.TopStart),
                )
            }

            if (state.camera.canToggleTorch) {
                ScanHintButton(
                    icon = if (state.camera.torchOn) MiseIcons.Flash else MiseIcons.FlashOff,
                    label = stringResource(
                        if (state.camera.torchOn) Res.string.scan_torch_off else Res.string.scan_torch_on,
                    ),
                    onClick = { onAction(ScanAction.TorchToggled) },
                    highlighted = state.camera.torchOn,
                    modifier = Modifier.align(Alignment.TopEnd),
                )
            }
        }

        val manualBarcodeFocusRequester = remember { FocusRequester() }
        // Grabs focus the moment the field appears, so the keyboard is
        // already up and ready to type — no extra tap to reach it.
        LaunchedEffect(state.manualEntryVisible) {
            if (state.manualEntryVisible) manualBarcodeFocusRequester.requestFocus()
        }

        Column(
            Modifier
                .align(if (state.manualEntryVisible) Alignment.Center else Alignment.TopCenter)
                .widthIn(max = 320.dp)
                .fillMaxWidth()
                // Not manual entry: anchored below the corner hints instead
                // of dead-centered, so it can never sit under them. Manual
                // entry: centered in the (now full-screen, keyboard-clear)
                // panel instead of pinned near the top, so the field sits at
                // a comfortable thumb reach rather than up near the notch.
                .then(if (state.manualEntryVisible) Modifier else Modifier.padding(top = 84.dp)),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            if (state.manualEntryVisible) {
                MiseSearchField(
                    value = state.manualBarcode,
                    onValueChange = { onAction(ScanAction.ManualBarcodeChanged(it)) },
                    placeholder = stringResource(Res.string.scan_manual_placeholder),
                    modifier = Modifier.fillMaxWidth(),
                    textFieldModifier = Modifier.focusRequester(manualBarcodeFocusRequester),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
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
                Spacer(Modifier.height(14.dp))
                MiseTextAction(
                    text = stringResource(Res.string.scan_browse_recipes),
                    onClick = { onAction(ScanAction.RecipesClicked) },
                )
            }
        }
    }
}

/** A corner affordance: icon + short caption, read as a hint rather than a toolbar button. */
@Composable
private fun ScanHintButton(
    icon: ImageVector,
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    highlighted: Boolean = false,
) {
    val colors = MiseTheme.colors
    Column(
        modifier = modifier.width(104.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        MiseIconCircleButton(
            icon = icon,
            contentDescription = label,
            onClick = onClick,
            size = 44,
            iconSize = 18,
            tint = if (highlighted) colors.onLime else colors.text,
            borderColor = if (highlighted) colors.mint else colors.borderStrong,
            background = if (highlighted) colors.mint else colors.panel,
        )
        Spacer(Modifier.height(6.dp))
        Text(
            text = label,
            style = MiseTheme.typography.monoTiny,
            color = colors.textMuted,
            textAlign = TextAlign.Center,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

/**
 * The camera is intentionally removed as soon as a barcode is accepted: the
 * next useful thing is the lookup, not a frozen viewfinder. Rotating facts
 * make a longer network lookup feel intentional without hiding its progress.
 */
@Composable
private fun ResolvingScanContent(
    state: ScanUiState,
    showBackNavigation: Boolean,
    onAction: (ScanAction) -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = MiseTheme.colors
    val facts = listOf(
        stringResource(Res.string.scan_loading_fact_barcodes),
        stringResource(Res.string.scan_loading_fact_nutrition),
        stringResource(Res.string.scan_loading_fact_graph),
        stringResource(Res.string.scan_loading_fact_data),
    )
    var factIndex by remember { mutableStateOf(0) }

    LaunchedEffect(facts.size) {
        while (true) {
            delay(6_000)
            factIndex = (factIndex + 1) % facts.size
        }
    }

    Column(
        modifier
            .fillMaxSize()
            .background(Color.Black)
            .safeDrawingPadding()
            .padding(horizontal = 28.dp, vertical = 20.dp)
            .screenIn(),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        RootScannerTopBar(
            isResolving = true,
            showBackNavigation = showBackNavigation,
            onBackClick = { onAction(ScanAction.BackClicked) },
        )
        Spacer(Modifier.weight(1f))
        CircularProgressIndicator(
            color = colors.cyan,
            strokeWidth = 3.dp,
            modifier = Modifier.size(52.dp),
        )
        Spacer(Modifier.height(28.dp))
        Text(
            text = stringResource(Res.string.scan_phase_resolving),
            style = MiseTheme.typography.title,
            color = colors.text,
            textAlign = TextAlign.Center,
        )
        state.visibleBarcode?.let { barcode ->
            Spacer(Modifier.height(10.dp))
            Text(
                text = barcode,
                style = MiseTheme.typography.monoSmall,
                color = colors.cyan,
            )
        }
        Spacer(Modifier.height(28.dp))
        Text(
            text = facts[factIndex],
            style = MiseTheme.typography.body,
            color = colors.textMuted,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth(),
        )
        Spacer(Modifier.weight(1f))
        ScanProgressTrail(phase = ScanPhase.Resolving, modifier = Modifier.fillMaxWidth())
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
            Spacer(Modifier.weight(1f))
            MiseIconCircleButton(
                icon = MiseIcons.Scale,
                contentDescription = stringResource(Res.string.scan_review_compare_another),
                onClick = { onAction(ScanAction.CompareWithAnotherClicked) },
            )
        }

        Column(Modifier.weight(1f).padding(horizontal = 20.dp, vertical = 12.dp)) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(scoreColor.copy(alpha = 0.08f), RoundedCornerShape(8.dp))
                    .border(1.dp, scoreColor.copy(alpha = 0.4f), RoundedCornerShape(8.dp))
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    ScoreRing(score = product.score, color = scoreColor, size = 68, strokeWidth = 6) {
                        Text(
                            text = product.score.toString(),
                            style = MiseTheme.typography.mono,
                            color = scoreColor,
                        )
                    }
                    Spacer(Modifier.height(6.dp))
                    VerdictBadge(product.verdict.label, scoreColor)
                }
                Column(Modifier.weight(1f).padding(horizontal = 16.dp)) {
                    Text(product.name, style = MiseTheme.typography.title, color = colors.text)
                    Text(
                        product.category,
                        style = MiseTheme.typography.monoSmall,
                        color = colors.textMuted,
                    )
                }
            }

            if (!product.hasCompleteData) {
                Spacer(Modifier.height(12.dp))
                Text(
                    text = stringResource(Res.string.scan_review_incomplete),
                    style = MiseTheme.typography.monoSmall,
                    color = colors.amber,
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(colors.amber.copy(alpha = 0.10f), RoundedCornerShape(8.dp))
                        .border(1.dp, colors.amber.copy(alpha = 0.3f), RoundedCornerShape(8.dp))
                        .padding(12.dp),
                )
            }

            Spacer(Modifier.height(14.dp))

            val tabs = listOf(
                stringResource(Res.string.scan_tab_overview),
                stringResource(Res.string.scan_tab_nutrition),
                stringResource(Res.string.scan_tab_composition),
                stringResource(Res.string.scan_tab_photos),
            )
            MiseTabPager(tabs = tabs, modifier = Modifier.weight(1f)) { page ->
                Column(
                    Modifier.fillMaxSize().verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    when (page) {
                        0 -> ScanOverviewPage(product)
                        1 -> ScanNutritionPage(product)
                        2 -> ScanCompositionPage(product)
                        else -> ScanPhotosPage(product)
                    }
                    Spacer(Modifier.height(8.dp))
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
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                MisePrimaryButton(
                    text = stringResource(
                        if (alreadyAdded) {
                            Res.string.scan_review_open_graph
                        } else {
                            Res.string.scan_review_add
                        }
                    ),
                    onClick = { onAction(ScanAction.AddReviewedProductClicked) },
                    modifier = Modifier
                        .weight(1f)
                        .height(54.dp),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 15.dp),
                )
                MiseGhostButton(
                    text = stringResource(Res.string.scan_review_skip),
                    onClick = { onAction(ScanAction.ReviewedProductSkipped) },
                    modifier = Modifier
                        .weight(1f)
                        .height(54.dp),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 15.dp),
                )
            }
        }
    }
}

@Composable
private fun ScannedPhotoTile(title: String, imageUrl: String, contentDescription: String) {
    val colors = MiseTheme.colors
    Column(
        Modifier
            .fillMaxWidth()
            .background(colors.panel, RoundedCornerShape(10.dp))
            .border(1.dp, colors.border, RoundedCornerShape(10.dp))
            .padding(12.dp),
    ) {
        Text(title.uppercase(), style = MiseTheme.typography.monoTiny, color = colors.textMuted)
        Spacer(Modifier.height(6.dp))
        Box(
            Modifier
                .fillMaxWidth()
                .height(204.dp)
                .clip(RoundedCornerShape(18.dp))
                .background(colors.surface),
            contentAlignment = Alignment.Center,
        ) {
            ZoomableProductImage(
                imageUrl = imageUrl,
                contentDescription = contentDescription,
                modifier = Modifier.fillMaxSize(),
            )
        }
    }
}

@Composable
private fun ScannedGradeScoreScale(grade: String, label: String, hint: String) {
    val colors = MiseTheme.colors
    Column(
        Modifier
            .fillMaxWidth()
            .background(colors.panel, RoundedCornerShape(10.dp))
            .border(1.dp, colors.border, RoundedCornerShape(10.dp))
            .padding(12.dp),
    ) {
        MiseGradeScoreScale(
            selectedGrade = grade,
            label = label,
            hint = hint,
        )
    }
}

@Composable
private fun ScanOverviewPage(product: Product) {
    val colors = MiseTheme.colors
    val placeholder = stringResource(Res.string.scan_not_set)

    product.details.imageUrl.trim().takeIf { it.startsWith("https://") }?.let { imageUrl ->
        Box(
            Modifier
                .fillMaxWidth()
                .height(204.dp)
                .clip(RoundedCornerShape(18.dp))
                .background(colors.surface),
            contentAlignment = Alignment.Center,
        ) {
            ZoomableProductImage(
                imageUrl = imageUrl,
                contentDescription = product.name,
                modifier = Modifier.fillMaxSize(),
            )
        }
    }
    Text(
        text = product.summary,
        style = MiseTheme.typography.body,
        color = colors.text.copy(alpha = 0.82f),
    )
    product.details.nutriScore.cleanNutriScoreGrade()?.let { grade ->
        ScannedGradeScoreScale(
            grade = grade,
            label = stringResource(Res.string.scan_nutri_score),
            hint = stringResource(Res.string.scan_nutri_score_hint),
        )
    }
    product.details.ecoScore.cleanNutriScoreGrade()?.let { grade ->
        ScannedGradeScoreScale(
            grade = grade,
            label = stringResource(Res.string.scan_eco_score),
            hint = stringResource(Res.string.scan_eco_score_hint),
        )
    }

    MiseFactList(placeholder = placeholder, facts = product.overviewFacts())

    if (product.alternatives.isNotEmpty()) {
        SectionLabel(stringResource(Res.string.scan_review_alternatives))
        product.alternatives.forEach { alternative ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(colors.lime.copy(alpha = 0.07f), RoundedCornerShape(8.dp))
                    .border(1.dp, colors.lime.copy(alpha = 0.2f), RoundedCornerShape(8.dp))
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

@Composable
private fun ScanNutritionPage(product: Product) {
    val placeholder = stringResource(Res.string.scan_not_set)

    MiseFactList(placeholder = placeholder, facts = product.nutritionFacts())

    val scannedNutrients = product.details.nutrients.ifEmpty { product.nutrients }
        .filter { nutrient -> nutrient.amount.toDoubleOrNull()?.let { it.isFinite() && it > 0 } == true }
    if (scannedNutrients.isNotEmpty()) {
        val perLabel = product.details.nutrientsPer.ifBlank { "100 g" }
        SectionLabel(stringResource(Res.string.scan_nutrients_per, perLabel))
        MiseFactList(
            placeholder = placeholder,
            facts = scannedNutrients.map { MiseFact(it.name, "${it.amount} ${it.unit}".trim()) },
        )
    }
}

@Composable
private fun ScanCompositionPage(product: Product) {
    MiseFactList(placeholder = stringResource(Res.string.scan_not_set), facts = product.compositionFacts())
}

@Composable
private fun ScanPhotosPage(product: Product) {
    val photos = listOfNotNull(
        product.details.ingredientsImageUrl.trim().takeIf { it.startsWith("https://") }?.let { "Ingredients photo" to it },
        product.details.nutritionImageUrl.trim().takeIf { it.startsWith("https://") }?.let { "Nutrition facts photo" to it },
        product.details.packagingImageUrl.trim().takeIf { it.startsWith("https://") }?.let { "Packaging photo" to it },
    )
    if (photos.isEmpty()) {
        EmptyState(text = stringResource(Res.string.scan_no_extra_photos))
        return
    }
    photos.forEach { (title, url) -> ScannedPhotoTile(title = title, imageUrl = url, contentDescription = product.name) }
}

private fun String.cleanScanValue() = trim().takeIf { it.isNotEmpty() && !it.equals("null", ignoreCase = true) }.orEmpty()

private fun List<String>.scanTags() = mapNotNull { it.cleanScanValue().ifBlank { null }?.substringAfter(':')?.replace('-', ' ') }
    .joinToString()

/** Identity facts a shopper reads at a glance — brand, portion, plain-text ingredients, allergens. */
internal fun Product.overviewFacts(): List<MiseFact> = listOf(
    MiseFact("Brand", details.brand.cleanScanValue()),
    MiseFact("Quantity", details.quantity.cleanScanValue()),
    MiseFact("Serving size", details.servingSize.cleanScanValue()),
    MiseFact("Best before", details.expirationDate.cleanScanValue()),
    MiseFact("Ingredients", details.ingredientsText.cleanScanValue()),
    MiseFact("Allergens", details.allergens.scanTags()),
)

/** Everything about how the nutrients themselves should be read. */
internal fun Product.nutritionFacts(): List<MiseFact> = listOf(
    MiseFact(
        "NOVA",
        details.novaGroup?.takeIf { it in 1..4 }?.let { group ->
            "$group · ${
                when (group) {
                    1 -> "Unprocessed or minimally processed food"
                    2 -> "Processed culinary ingredient"
                    3 -> "Processed food"
                    else -> "Ultra-processed food"
                }
            }"
        }.orEmpty(),
    ),
    MiseFact("Nutri-Score version", details.nutriScoreVersion.cleanScanValue()),
    MiseFact("Scored against", details.comparedToCategory.cleanScanValue()),
) + details.nutrientLevels.map { (name, level) -> MiseFact(name.cleanScanValue().ifBlank { name }, level) }

/** What the product is made of and where it comes from. */
internal fun Product.compositionFacts(): List<MiseFact> = listOf(
    MiseFact(
        "Ingredients breakdown",
        details.ingredients
            .filter { it.name.cleanScanValue().isNotBlank() }
            .joinToString { ingredient ->
                val percent = ingredient.percent ?: ingredient.percentEstimate
                ingredient.name.trim() + (percent?.let { " (${it.toInt()}%)" } ?: "")
            },
    ),
    MiseFact("May contain traces of", details.traces.scanTags()),
    MiseFact("Additives", details.additives.scanTags()),
    MiseFact("Categories", details.categories.scanTags()),
    MiseFact("Labels", details.labels.scanTags()),
    MiseFact(
        "Food classification",
        listOf(details.pnnsGroup, details.pnnsSubgroup).filter(String::isNotBlank).joinToString(" · ")
            .ifBlank { details.foodGroups.scanTags() },
    ),
    MiseFact("Sold in", details.countries.scanTags()),
    MiseFact("Ingredient origin", details.origins.scanTags().ifBlank { details.originNote.cleanScanValue() }),
    MiseFact("Manufactured in", details.manufacturingPlaces.scanTags()),
    MiseFact("Purchased in", details.purchasePlaces.scanTags()),
    MiseFact("Stores", details.stores.scanTags()),
    MiseFact("Packaging", details.packaging.joinToString { it.scanSummary() }),
)

private fun ProductPackagingComponent.scanSummary(): String = buildString {
    numberOfUnits?.takeIf { it > 0 }?.let { append(it).append("× ") }
    append(listOf(shape, material, recycling).filter(String::isNotBlank).joinToString(" · "))
    quantityPerUnit.takeIf(String::isNotBlank)?.let { append(" (").append(it).append(')') }
}.trim()

private fun String.cleanNutriScoreGrade(): String? = trim().uppercase()
    .takeIf { it in setOf("A", "B", "C", "D", "E") }

/**
 * Camera preview kept out of the content composable so previews and manual
 * barcode entry work even when a camera is unavailable.
 */
@Composable
private fun CameraLayer(controls: CameraControls, active: Boolean, onAction: (ScanAction) -> Unit) {
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
        active = active,
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
 * The reticle. Everything about it is derived from [phase], so the frame
 * itself reports what the scanner is doing: mint and sweeping top-to-bottom
 * while it hunts, snapped wide and still the moment a code is locked, cyan
 * with a spinner while the lookup runs, red when it failed.
 */
@Composable
private fun ScanReticle(phase: ScanPhase, modifier: Modifier = Modifier, reticleSize: Dp = 250.dp) {
    val colors = MiseTheme.colors
    val accent by animateColorAsState(
        targetValue = when (phase) {
            ScanPhase.Searching -> colors.mint
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
    val sweep = rememberSweep(durationMillis = 1800, label = "scanLine")
    val spin = rememberSweep(durationMillis = 1100, label = "scanSpin")

    Canvas(modifier.size(reticleSize)) {
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

        if (phase == ScanPhase.Searching) {
            // Straight top-to-bottom sweep that snaps back to the top on
            // restart — reads as the line "blinking" downward each pass.
            val y = inset + sweep.value * (size.height - 2 * inset)
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
    val accent = if (phase == ScanPhase.Failed) colors.red else colors.mint

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

