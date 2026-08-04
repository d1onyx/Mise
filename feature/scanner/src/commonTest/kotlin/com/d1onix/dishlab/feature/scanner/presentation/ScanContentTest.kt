package com.d1onix.dishlab.feature.scanner.presentation

import kotlin.test.Test
import kotlin.test.assertFalse

class ScanContentTest {
    @Test
    fun `root Scanner content has no navigation icon`() {
        assertFalse(ROOT_SCANNER_HAS_NAVIGATION_ICON)
    }
}
