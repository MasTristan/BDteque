package com.bdshelf.app.ui.crash

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.bdshelf.app.BdShelfApplication
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class CrashReportUiState(
    val fileName: String? = null,
    val content: String = "",
    val reportVisible: Boolean = false,
)

/**
 * Bannière de plantage au démarrage suivant (§E6 5.4) : ne s'affiche que si
 * un rapport existe ET n'a pas déjà été acquitté (vu, envoyé ou refusé).
 */
class CrashReportViewModel(application: Application) : AndroidViewModel(application) {

    private val app = application as BdShelfApplication

    private val _uiState = MutableStateFlow(CrashReportUiState())
    val uiState: StateFlow<CrashReportUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            val report = app.crashReporter.latestReport() ?: return@launch
            val lastAcknowledged = app.userPreferencesRepository.lastAcknowledgedCrashFile.first()
            if (report.fileName != lastAcknowledged) {
                _uiState.value = CrashReportUiState(fileName = report.fileName, content = report.content)
            }
        }
    }

    fun onViewReport() {
        _uiState.update { it.copy(reportVisible = true) }
    }

    /** « Non merci » ou fermeture de la vue détaillée : acquitte sans envoyer. */
    fun onDismiss() {
        acknowledge()
    }

    /** L'envoi lui-même passe par le sélecteur système, côté UI ; ceci n'acquitte que le rapport. */
    fun onSent() {
        acknowledge()
    }

    private fun acknowledge() {
        val fileName = _uiState.value.fileName ?: return
        viewModelScope.launch { app.userPreferencesRepository.setLastAcknowledgedCrashFile(fileName) }
        _uiState.value = CrashReportUiState()
    }
}
