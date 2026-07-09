package com.bdshelf.app.ui.releases

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.bdshelf.app.BdShelfApplication
import com.bdshelf.app.BuildConfig
import com.bdshelf.app.domain.ReleaseWithOwnership
import com.bdshelf.app.domain.crossReferenceReleases
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class ReleasesUiState(
    val isLoading: Boolean = true,
    val isRefreshing: Boolean = false,
    val upcoming: List<ReleaseWithOwnership> = emptyList(),
    val released: List<ReleaseWithOwnership> = emptyList(),
    val refreshError: Boolean = false,
)

/** À paraître (§6.7) : sorties des séries suivies, croisées avec la collection. */
class ReleasesViewModel(application: Application) : AndroidViewModel(application) {

    private val app = application as BdShelfApplication

    private val _uiState = MutableStateFlow(ReleasesUiState())
    val uiState: StateFlow<ReleasesUiState> = _uiState.asStateFlow()

    private var loaded = false

    fun load() {
        if (loaded) return
        loaded = true
        viewModelScope.launch {
            refreshFromCache()
            _uiState.update { it.copy(isLoading = false) }
        }
    }

    /** Bouton « Mettre à jour les nouveautés » : fetch manuel, conserve le cache en cas d'échec (§6.7). */
    fun onRefresh() {
        viewModelScope.launch {
            _uiState.update { it.copy(isRefreshing = true, refreshError = false) }
            val url = app.userPreferencesRepository.releasesUrlOverride.first()?.takeIf { it.isNotBlank() }
                ?: BuildConfig.RELEASES_URL
            val result = app.releasesRepository.refresh(url)
            refreshFromCache()
            _uiState.update { it.copy(isRefreshing = false, refreshError = result.isFailure) }
        }
    }

    private suspend fun refreshFromCache() {
        val document = app.releasesRepository.loadCached()
        val tracked = app.collectionRepository.trackedSeriesIds()
        val owned = app.collectionRepository.ownedTomeRefs()
        val crossed = crossReferenceReleases(document.releases, tracked, owned)
        _uiState.update {
            it.copy(
                upcoming = crossed.filter { r -> r.release.status == "UPCOMING" }.sortedBy { r -> r.release.expectedDate },
                released = crossed.filter { r -> r.release.status == "RELEASED" }.sortedByDescending { r -> r.release.expectedDate },
            )
        }
    }
}
