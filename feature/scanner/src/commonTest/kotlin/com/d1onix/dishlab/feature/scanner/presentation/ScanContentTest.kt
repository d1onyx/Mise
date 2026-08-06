package com.d1onix.dishlab.feature.scanner.presentation

import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class ScanContentTest {
    @Test
    fun `root scanner hides back navigation`() {
        assertFalse(rootScannerTopBarModel(showBackNavigation = false).hasNavigationIcon)
    }

    @Test
    fun `scanner opened from graph shows back navigation`() {
        assertTrue(rootScannerTopBarModel(showBackNavigation = true).hasNavigationIcon)
    }
}
