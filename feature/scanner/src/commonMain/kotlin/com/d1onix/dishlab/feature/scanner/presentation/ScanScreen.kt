package com.d1onix.dishlab.feature.scanner.presentation

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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
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
import com.d1onix.dishlab.domain.model.ScoreVerdict
import com.d1onix.dishlab.feature.scanner.resources.Res
import com.d1onix.dishlab.feature.scanner.resources.scan_back
import com.d1onix.dishlab.feature.scanner.resources.scan_camera_denied
import com.d1onix.dishlab.feature.scanner.resources.scan_camera_starting
import com.d1onix.dishlab.feature.scanner.resources.scan_capture
import com.d1onix.dishlab.feature.scanner.resources.scan_hint_primary
import com.d1onix.dishlab.feature.scanner.resources.scan_hint_secondary
import com.d1onix.dishlab.feature.scanner.resources.scan_manual_cancel
import com.d1onix.dishlab.feature.scanner.resources.scan_manual_entry
import com.d1onix.dishlab.feature.scanner.resources.scan_manual_placeholder
import com.d1onix.dishlab.feature.scanner.resources.scan_manual_submit
import com.d1onix.dishlab.feature.scanner.resources.scan_review_add
import com.d1onix.dishlab.feature.scanner.resources.scan_review_alternatives
import com.d1onix.dishlab.feature.scanner.resources.scan_review_incomplete
import com.d1onix.dishlab.feature.scanner.resources.scan_review_nutrition
import com.d1onix.dishlab.feature.scanner.resources.scan_review_open_graph
import com.d1onix.dishlab.feature.scanner.resources.scan_review_question
import com.d1onix.dishlab.feature.scanner.resources.scan_review_skip
import com.d1onix.dishlab.feature.scanner.resources.scan_review_title
import com.d1onix.dishlab.feature.scanner.resources.scan_simulate_not_found
import com.d1onix.dishlab.feature.scanner.resources.scan_title
import com.d1onix.dishlab.feature.scanner.resources.scan_title_resolving
import com.kashif.cameraK.compose.CameraKScreen
import com.kashif.cameraK.compose.rememberCameraKState
import com.kashif.cameraK.permissions.providePermissions
import com.kashif.qrscannerplugin.QRScannerPlugin
import org.jetbrains.compose.resources.stringResource
import kotlin.math.PI
import kotlin.math.sin

@Composable
fun ScanScreen(viewModel: ScanViewModel) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    ScanContent(
        state = state,
        onAction = viewModel::onAction,
        // The camera is a platform concern, injected as a slot so the content
        // itself stays renderable in a preview.
        cameraPreview = { onBarcode -> CameraLayer(onBarcode) },
    )
}

@Composable
internal fun ScanContent(
    state: ScanUiState,
    onAction: (ScanAction) -> Unit,
    modifier: Modifier = Modifier,
    cameraPreview: @Composable (onBarcode: (String) -> Unit) -> Unit = {},
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

    Box(modifier.fillMaxSize().background(colors.backgroundDeep)) {
        cameraPreview { barcode -> onAction(ScanAction.BarcodeDetected(barcode)) }

        Column(
            Modifier
                .fillMaxSize()
                .safeDrawingPadding()
                .padding(horizontal = 22.dp, vertical = 20.dp)
                .screenIn(),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                MiseIconCircleButton(
                    icon = MiseIcons.ChevronLeft,
                    contentDescription = stringResource(Res.string.scan_back),
                    onClick = { onAction(ScanAction.BackClicked) },
                    size = 40,
                )
                Box(Modifier.weight(1f), contentAlignment = Alignment.Center) {
                    SectionLabel(
                        text = if (state.isResolving) {
                            stringResource(Res.string.scan_title_resolving)
                        } else {
                            stringResource(Res.string.scan_title)
                        },
                        color = colors.textMuted,
                    )
                }
                Spacer(Modifier.size(40.dp))
            }

            Spacer(Modifier.weight(1f))

            ScanFrame()

            Spacer(Modifier.height(34.dp))
            Text(
                text = stringResource(Res.string.scan_hint_primary),
                style = MiseTheme.typography.bodyLarge,
                color = colors.text.copy(alpha = 0.75f),
                textAlign = TextAlign.Center,
            )
            Spacer(Modifier.height(6.dp))
            Text(
                text = stringResource(Res.string.scan_hint_secondary),
                style = MiseTheme.typography.monoSmall,
                color = colors.textFaint,
            )

            Spacer(Modifier.weight(1f))

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
                MisePrimaryButton(
                    text = stringResource(Res.string.scan_capture),
                    onClick = { onAction(ScanAction.CaptureClicked) },
                    enabled = !state.isResolving,
                    modifier = Modifier.fillMaxWidth(),
                )
                Spacer(Modifier.height(10.dp))
                MiseGhostButton(
                    text = stringResource(Res.string.scan_manual_entry),
                    onClick = { onAction(ScanAction.ManualEntryToggled) },
                    modifier = Modifier.fillMaxWidth(),
                )
                Spacer(Modifier.height(8.dp))
                MiseTextAction(
                    text = stringResource(Res.string.scan_simulate_not_found),
                    onClick = { onAction(ScanAction.SimulateNotFoundClicked) },
                    modifier = Modifier.fillMaxWidth(),
                )
            }
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
        }

        LazyColumn(
            modifier = Modifier.weight(1f),
            contentPadding = PaddingValues(horizontal = 20.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(18.dp),
        ) {
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

            item {
                Text(
                    text = product.summary,
                    style = MiseTheme.typography.body,
                    color = colors.text.copy(alpha = 0.82f),
                )
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
                text = stringResource(Res.string.scan_review_skip),
                onClick = { onAction(ScanAction.ReviewedProductSkipped) },
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}

/**
 * Camera preview plus the ZXing/AVFoundation barcode plugin. Kept out of the
 * content composable so the screen renders without a camera — in a preview, on
 * an emulator, or when the permission is refused.
 */
@Composable
private fun CameraLayer(onBarcodeDetected: (String) -> Unit) {
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

    val scope = rememberCoroutineScope()
    val qrPlugin = remember(scope) { QRScannerPlugin(scope) }
    val cameraState by rememberCameraKState(
        setupPlugins = { holder -> qrPlugin.attachToStateHolder(holder) },
    )

    LaunchedEffect(qrPlugin) {
        qrPlugin.getQrCodeFlow().collect(onBarcodeDetected)
    }

    CameraKScreen(
        modifier = Modifier.fillMaxSize(),
        cameraState = cameraState,
        loadingContent = { Box(Modifier.fillMaxSize().background(MiseTheme.colors.backgroundDeep)) },
        errorContent = { CameraUnavailable(permissionDenied = false) },
        content = { },
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

/** The 250dp reticle: four lime corners, a sweeping line and a pulsing barcode. */
@Composable
private fun ScanFrame() {
    val colors = MiseTheme.colors
    val sweep = rememberSweep(durationMillis = 2400, label = "scanLine")
    val pulse = rememberPulse(durationMillis = 2400, from = 0.25f, to = 0.6f, label = "scanPulse")

    Canvas(Modifier.size(250.dp)) {
        val corner = 34.dp.toPx()
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
                drawLine(colors.lime, origin, end, strokeWidth = stroke)
            }
        }

        // The decorative barcode behind the line.
        val bars = listOf(8, 3, 5, 2, 7, 3, 9, 2, 4, 6, 3, 8, 2, 5, 3, 7)
        var x = inset + 20.dp.toPx()
        bars.forEach { width ->
            drawRect(
                color = colors.lime,
                topLeft = Offset(x, size.height / 2 - 45.dp.toPx()),
                size = Size(width.dp.toPx() * 0.7f, 90.dp.toPx()),
                alpha = pulse.value,
            )
            x += (width + 3).dp.toPx() * 0.7f
        }

        // `sin` turns the linear sweep into the prototype's back-and-forth line.
        val travel = sin(sweep.value * 2f * PI.toFloat()) * (size.height / 2 - inset)
        val y = size.height / 2 + travel
        drawLine(
            brush = Brush.horizontalGradient(
                listOf(Color.Transparent, colors.lime, Color.Transparent),
            ),
            start = Offset(inset, y),
            end = Offset(size.width - inset, y),
            strokeWidth = 2.dp.toPx(),
        )
    }
}
