package com.bdshelf.app.ui.series

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.bdshelf.app.BdShelfApplication
import com.bdshelf.app.data.local.dao.SeriesWithCounts
import com.bdshelf.app.domain.SeriesFilter
import com.bdshelf.app.domain.SeriesSort
import com.bdshelf.app.domain.filterAndSortSeries
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update

data class SeriesListUiState(
    val query: String = "",
    val filter: SeriesFilter = SeriesFilter.ALL,
    val sort: SeriesSort = SeriesSort.TITLE,
    val series: List<SeriesWithCounts> = emptyList(),
)

/**
 * Liste des séries (§6.5) : recherche insensible à la casse et aux accents
 * (titres de séries ET d'albums), filtres rapides et tris — calculés en pur
 * ([filterAndSortSeries]).
 */
class SeriesListViewModel(application: Application) : AndroidViewModel(application) {

    private val app = application as BdShelfApplication

    private val query = MutableStateFlow("")
    private val filter = MutableStateFlow(SeriesFilter.ALL)
    private val sort = MutableStateFlow(SeriesSort.TITLE)

    val uiState: StateFlow<SeriesListUiState> = combine(
        query,
        filter,
        sort,
        app.collectionRepository.allSeriesWithCounts(),
        app.collectionRepository.allAlbums(),
    ) { q, f, s, allSeries, allAlbums ->
        SeriesListUiState(
            query = q,
            filter = f,
            sort = s,
            series = filterAndSortSeries(allSeries, allAlbums, q, f, s),
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), SeriesListUiState())

    fun onQueryChange(value: String) {
        query.update { value }
    }

    fun onFilterChange(value: SeriesFilter) {
        filter.update { value }
    }

    fun onSortChange(value: SeriesSort) {
        sort.update { value }
    }
}
