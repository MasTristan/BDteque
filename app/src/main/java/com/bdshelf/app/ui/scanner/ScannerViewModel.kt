package com.bdshelf.app.ui.scanner

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

data class ScannerUiState(
    val scannedEan: String? = null,
)

/** Scanner code-barres (§6.3) : retient le premier EAN détecté pour déclencher le verdict une seule fois. */
class ScannerViewModel : ViewModel() {

    private val _uiState = MutableStateFlow(ScannerUiState())
    val uiState: StateFlow<ScannerUiState> = _uiState.asStateFlow()

    fun onBarcodeDetected(ean: String) {
        _uiState.update { if (it.scannedEan == null) it.copy(scannedEan = ean) else it }
    }
}
