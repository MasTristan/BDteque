package com.bdshelf.app.ui.series

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.bdshelf.app.BdShelfApplication
import com.bdshelf.app.data.local.dao.SeriesWithCounts
import com.bdshelf.app.domain.normalizedForSearch
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update

data class SeriesListUiState(
    val query: String = "",
    val series: List<SeriesWithCounts> = emptyList(),
)

/** Liste alphabétique des séries + recherche insensible à la casse et aux accents (§6.5). */
class SeriesListViewModel(application: Application) : AndroidViewModel(application) {

    private val app = application as BdShelfApplication

    private val query = MutableStateFlow("")

    val uiState: StateFlow<SeriesListUiState> = combine(
        query,
        app.collectionRepository.allSeriesWithCounts(),
    ) { q, allSeries ->
        val filtered = if (q.isBlank()) {
            allSeries
        } else {
            val normalizedQuery = q.normalizedForSearch()
            allSeries.filter { it.title.normalizedForSearch().contains(normalizedQuery) }
        }
        SeriesListUiState(query = q, series = filtered)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), SeriesListUiState())

    fun onQueryChange(value: String) {
        query.update { value }
    }
}
