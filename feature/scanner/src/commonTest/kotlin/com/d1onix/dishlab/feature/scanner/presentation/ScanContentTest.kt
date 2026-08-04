package com.d1onix.dishlab.feature.scanner.presentation

import kotlin.test.Test
import kotlin.test.assertNull

class ScanContentTest {
    @Test
    fun `root Scanner content has no navigation icon`() {
        assertNull(rootScannerTopBarModel().navigationIcon)
    }
}
