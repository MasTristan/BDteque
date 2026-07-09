package com.bdshelf.app.ui.scanner

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ScannerViewModelTest {

    private val thorgal = "9782803672172"
    private val asterix = "9782012101333"

    private fun detect(viewModel: ScannerViewModel, ean: String, times: Int) {
        repeat(times) { viewModel.onBarcodeDetected(ean) }
    }

    @Test
    fun `single read is not enough`() {
        val viewModel = ScannerViewModel()
        detect(viewModel, thorgal, ScannerViewModel.REQUIRED_CONSECUTIVE_READS - 1)
        assertNull(viewModel.uiState.value.scannedEan)
    }

    @Test
    fun `consecutive identical reads confirm the code`() {
        val viewModel = ScannerViewModel()
        detect(viewModel, thorgal, ScannerViewModel.REQUIRED_CONSECUTIVE_READS)
        assertEquals(thorgal, viewModel.uiState.value.scannedEan)
    }

    @Test
    fun `a different read resets the confirmation count`() {
        val viewModel = ScannerViewModel()
        detect(viewModel, thorgal, ScannerViewModel.REQUIRED_CONSECUTIVE_READS - 1)
        viewModel.onBarcodeDetected(asterix) // lecture parasite : repart de zéro
        detect(viewModel, thorgal, ScannerViewModel.REQUIRED_CONSECUTIVE_READS - 1)
        assertNull(viewModel.uiState.value.scannedEan)
        viewModel.onBarcodeDetected(thorgal)
        assertEquals(thorgal, viewModel.uiState.value.scannedEan)
    }

    @Test
    fun `invalid checksum reads are ignored`() {
        val viewModel = ScannerViewModel()
        detect(viewModel, "9782803672173", 10) // dernier chiffre faux
        assertNull(viewModel.uiState.value.scannedEan)
    }

    @Test
    fun `only the first confirmed code is kept`() {
        val viewModel = ScannerViewModel()
        detect(viewModel, thorgal, ScannerViewModel.REQUIRED_CONSECUTIVE_READS)
        detect(viewModel, asterix, ScannerViewModel.REQUIRED_CONSECUTIVE_READS)
        assertEquals(thorgal, viewModel.uiState.value.scannedEan)
    }

    @Test
    fun `manual entry is accepted immediately and normalized`() {
        val viewModel = ScannerViewModel()
        viewModel.onManualEan("978-2-8036-7217-2")
        assertEquals(thorgal, viewModel.uiState.value.scannedEan)
    }

    @Test
    fun `manual entry with invalid code is ignored`() {
        val viewModel = ScannerViewModel()
        viewModel.onManualEan("9782803672173")
        assertNull(viewModel.uiState.value.scannedEan)
    }
}
