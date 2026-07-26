package com.d1onix.dishlab.feature.scanner.presentation

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.d1onix.dishlab.designsystem.anim.screenIn
import com.d1onix.dishlab.designsystem.component.MiseIconCircleButton
import com.d1onix.dishlab.designsystem.component.MisePrimaryButton
import com.d1onix.dishlab.designsystem.component.MiseTextAction
import com.d1onix.dishlab.designsystem.icon.MiseIcons
import com.d1onix.dishlab.designsystem.theme.MiseTheme
import com.d1onix.dishlab.feature.scanner.resources.Res
import com.d1onix.dishlab.feature.scanner.resources.not_found_home
import com.d1onix.dishlab.feature.scanner.resources.not_found_message
import com.d1onix.dishlab.feature.scanner.resources.not_found_retry
import com.d1onix.dishlab.feature.scanner.resources.not_found_title
import com.d1onix.dishlab.feature.scanner.resources.scan_back
import org.jetbrains.compose.resources.stringResource

@Composable
fun ScanNotFoundScreen(viewModel: ScanNotFoundViewModel) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    ScanNotFoundContent(state = state, onAction = viewModel::onAction)
}

@Composable
internal fun ScanNotFoundContent(
    state: ScanNotFoundUiState,
    onAction: (ScanNotFoundAction) -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = MiseTheme.colors

    Column(
        modifier
            .fillMaxSize()
            .safeDrawingPadding()
            .padding(horizontal = 22.dp, vertical = 20.dp)
            .screenIn(),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(Modifier.fillMaxWidth()) {
            MiseIconCircleButton(
                icon = MiseIcons.ChevronLeft,
                contentDescription = stringResource(Res.string.scan_back),
                onClick = { onAction(ScanNotFoundAction.RetryClicked) },
                size = 40,
            )
        }

        Spacer(Modifier.weight(1f))

        Box(
            Modifier
                .size(96.dp)
                .background(colors.red.copy(alpha = 0.06f), CircleShape)
                .drawBehind {
                    drawCircle(
                        color = colors.red.copy(alpha = 0.5f),
                        style = Stroke(
                            width = 1.dp.toPx(),
                            pathEffect = PathEffect.dashPathEffect(
                                floatArrayOf(6.dp.toPx(), 6.dp.toPx()),
                            ),
                        ),
                    )
                },
            contentAlignment = Alignment.Center,
        ) {
            Icon(MiseIcons.NoResult, null, Modifier.size(40.dp), tint = colors.red)
        }

        Spacer(Modifier.height(22.dp))
        Text(
            text = stringResource(Res.string.not_found_title),
            style = MiseTheme.typography.headline,
            color = colors.text,
        )
        Spacer(Modifier.height(10.dp))
        Text(
            text = stringResource(Res.string.not_found_message, state.barcode),
            style = MiseTheme.typography.body,
            color = colors.textMuted,
            textAlign = TextAlign.Center,
            modifier = Modifier.widthIn(max = 260.dp),
        )

        Spacer(Modifier.weight(1f))

        MisePrimaryButton(
            text = stringResource(Res.string.not_found_retry),
            onClick = { onAction(ScanNotFoundAction.RetryClicked) },
            modifier = Modifier.fillMaxWidth(),
        )
        Spacer(Modifier.height(12.dp))
        MiseTextAction(
            text = stringResource(Res.string.not_found_home),
            onClick = { onAction(ScanNotFoundAction.HomeClicked) },
        )
    }
}
