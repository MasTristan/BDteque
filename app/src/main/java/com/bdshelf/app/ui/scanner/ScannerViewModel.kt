package com.bdshelf.app.ui.scanner

import androidx.lifecycle.ViewModel
import com.bdshelf.app.domain.BarcodeConfirmer
import com.bdshelf.app.domain.canonicalEan
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

data class ScannerUiState(
    val scannedEan: String? = null,
)

/**
 * Scanner code-barres (§6.3) : retient le premier EAN confirmé
 * ([BarcodeConfirmer] : somme de contrôle + lectures consécutives) pour
 * déclencher le verdict une seule fois.
 *
 * La saisie manuelle ne passe que la somme de contrôle (une seule "lecture").
 */
class ScannerViewModel : ViewModel() {

    private val _uiState = MutableStateFlow(ScannerUiState())
    val uiState: StateFlow<ScannerUiState> = _uiState.asStateFlow()

    private val confirmer = BarcodeConfirmer()

    fun onBarcodeDetected(raw: String) {
        if (_uiState.value.scannedEan != null) return
        val ean = confirmer.offer(raw) ?: return
        _uiState.update { if (it.scannedEan == null) it.copy(scannedEan = ean) else it }
    }

    /** Saisie manuelle d'un ISBN validé en amont ([canonicalEan]) : accepté immédiatement. */
    fun onManualEan(raw: String) {
        val ean = canonicalEan(raw) ?: return
        _uiState.update { if (it.scannedEan == null) it.copy(scannedEan = ean) else it }
    }

    companion object {
        const val REQUIRED_CONSECUTIVE_READS = BarcodeConfirmer.DEFAULT_REQUIRED_READS
    }
}
