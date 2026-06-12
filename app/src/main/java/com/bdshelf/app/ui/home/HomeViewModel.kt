package com.bdshelf.app.ui.home

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.bdshelf.app.BdShelfApplication
import com.bdshelf.app.domain.CollectionStats
import com.bdshelf.app.domain.crossReferenceReleases
import com.bdshelf.app.domain.missingCount
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class HomeUiState(
    val ownerName: String = "",
    val stats: CollectionStats = CollectionStats(seriesCount = 0, ownedCount = 0, missingCount = 0),
    val upcomingCount: Int = 0,
)

/** Écran d'accueil (§6.2) : wordmark, gros bouton scan, et deux cartes de synthèse. */
class HomeViewModel(application: Application) : AndroidViewModel(application) {

    private val app = application as BdShelfApplication

    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            app.userPreferencesRepository.ownerName.collect { name ->
                _uiState.update { it.copy(ownerName = name) }
            }
        }
        viewModelScope.launch {
            app.collectionRepository.collectionStats().collect { stats ->
                _uiState.update { it.copy(stats = stats) }
                refreshUpcomingCount()
            }
        }
    }

    /** Recompte les nouveautés à signaler à partir du dernier cache de sorties (§4.3). */
    private suspend fun refreshUpcomingCount() {
        val cached = app.releasesRepository.loadCached()
        val tracked = app.collectionRepository.trackedSeriesIds()
        val owned = app.collectionRepository.ownedTomeRefs()
        val upcoming = cached.releases.filter { it.status == "UPCOMING" }
        val count = crossReferenceReleases(upcoming, tracked, owned).missingCount()
        _uiState.update { it.copy(upcomingCount = count) }
    }
}
