package com.bdshelf.app.ui.home

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.bdshelf.app.BdShelfApplication
import com.bdshelf.app.domain.CollectionStats
import com.bdshelf.app.domain.buildShoppingList
import com.bdshelf.app.domain.crossReferenceReleases
import com.bdshelf.app.domain.itemCount
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
    val shoppingCount: Int = 0,
)

/** Écran d'accueil (§6.2) : wordmark, gros bouton scan, et trois cartes de synthèse. */
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
                refreshDerivedCounts()
            }
        }
    }

    /** Recompte nouveautés à signaler (§4.3) et liste d'achats (§6.10) depuis le cache des sorties. */
    private suspend fun refreshDerivedCounts() {
        val cached = app.releasesRepository.loadCached()
        val tracked = app.collectionRepository.trackedSeriesIds()
        val owned = app.collectionRepository.ownedTomeRefs()
        val upcoming = cached.releases.filter { it.status == "UPCOMING" }
        val upcomingCount = crossReferenceReleases(upcoming, tracked, owned).missingCount()

        val allSeries = app.database.seriesDao().allSeriesList()
        val allAlbums = app.database.albumDao().allAlbumsList()
        val crossed = crossReferenceReleases(cached.releases, tracked, owned)
        val shoppingCount = buildShoppingList(allSeries, allAlbums, crossed).itemCount()

        _uiState.update { it.copy(upcomingCount = upcomingCount, shoppingCount = shoppingCount) }
    }
}
