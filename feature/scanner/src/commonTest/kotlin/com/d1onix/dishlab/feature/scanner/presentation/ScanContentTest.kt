package com.d1onix.dishlab.feature.scanner.presentation

import kotlin.test.Test
import kotlin.test.assertTrue

class ScanContentTest {
    @Test
    fun `scanner content has back navigation`() {
        assertTrue(rootScannerTopBarModel().hasNavigationIcon)
    }
}
