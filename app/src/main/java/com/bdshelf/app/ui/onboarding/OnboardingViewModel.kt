package com.bdshelf.app.ui.onboarding

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.bdshelf.app.BdShelfApplication
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class OnboardingUiState(
    val name: String = "",
    val isImporting: Boolean = false,
    val importComplete: Boolean = false,
)

/** Premier lancement (§6.1) : saisie du prénom puis import silencieux du seed. */
class OnboardingViewModel(application: Application) : AndroidViewModel(application) {

    private val app = application as BdShelfApplication

    private val _uiState = MutableStateFlow(OnboardingUiState())
    val uiState: StateFlow<OnboardingUiState> = _uiState.asStateFlow()

    fun onNameChange(name: String) {
        _uiState.update { it.copy(name = name) }
    }

    fun onSubmit() {
        val name = _uiState.value.name.trim()
        if (name.isBlank() || _uiState.value.isImporting) return

        viewModelScope.launch {
            _uiState.update { it.copy(isImporting = true) }
            app.userPreferencesRepository.setOwnerName(name)
            app.seedImporter.import()
            app.userPreferencesRepository.setSeedImported(true)
            _uiState.update { it.copy(isImporting = false, importComplete = true) }
        }
    }
}
