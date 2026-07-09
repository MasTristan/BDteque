package com.bdshelf.app.ui.scanner

import androidx.lifecycle.ViewModel
import com.bdshelf.app.domain.canonicalEan
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

data class ScannerUiState(
    val scannedEan: String? = null,
)

/**
 * Scanner code-barres (§6.3) : retient le premier EAN confirmé pour déclencher
 * le verdict une seule fois.
 *
 * Deux garde-fous de fiabilité avant d'accepter une lecture caméra :
 * - somme de contrôle EAN vérifiée ([canonicalEan]) — une image floue peut
 *   faire lire un chiffre de travers à ML Kit ;
 * - le même code doit être lu sur [REQUIRED_CONSECUTIVE_READS] images
 *   consécutives : deux lectures erronées identiques d'affilée sont
 *   improbables, et à ~10-30 images/s la confirmation reste imperceptible.
 *
 * La saisie manuelle ne passe que la somme de contrôle (une seule "lecture").
 */
class ScannerViewModel : ViewModel() {

    private val _uiState = MutableStateFlow(ScannerUiState())
    val uiState: StateFlow<ScannerUiState> = _uiState.asStateFlow()

    private var candidate: String? = null
    private var candidateReads = 0

    fun onBarcodeDetected(raw: String) {
        if (_uiState.value.scannedEan != null) return
        val ean = canonicalEan(raw) ?: return
        if (ean == candidate) {
            candidateReads++
        } else {
            candidate = ean
            candidateReads = 1
        }
        if (candidateReads >= REQUIRED_CONSECUTIVE_READS) {
            _uiState.update { if (it.scannedEan == null) it.copy(scannedEan = ean) else it }
        }
    }

    /** Saisie manuelle d'un ISBN validé en amont ([canonicalEan]) : accepté immédiatement. */
    fun onManualEan(raw: String) {
        val ean = canonicalEan(raw) ?: return
        _uiState.update { if (it.scannedEan == null) it.copy(scannedEan = ean) else it }
    }

    companion object {
        const val REQUIRED_CONSECUTIVE_READS = 3
    }
}
