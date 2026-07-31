package com.d1onix.dishlab.feature.scanner.presentation

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

/** Platform camera preview that continuously emits product barcode values. */
@Composable
expect fun ProductBarcodeCamera(
    onBarcodeDetected: (String) -> Unit,
    modifier: Modifier = Modifier,
)
