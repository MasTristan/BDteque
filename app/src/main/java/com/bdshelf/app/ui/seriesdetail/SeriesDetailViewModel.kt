package com.bdshelf.app.ui.seriesdetail

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.bdshelf.app.BdShelfApplication
import com.bdshelf.app.data.local.entities.Album
import com.bdshelf.app.data.local.entities.Series
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class SeriesDetailUiState(
    val series: Series? = null,
    val albums: List<Album> = emptyList(),
    val ownedCount: Int = 0,
    val totalCount: Int = 0,
)

/** Détail d'une série = l'étagère de tranches + en-tête de stats (§6.6). */
class SeriesDetailViewModel(application: Application) : AndroidViewModel(application) {

    private val app = application as BdShelfApplication

    private val _uiState = MutableStateFlow(SeriesDetailUiState())
    val uiState: StateFlow<SeriesDetailUiState> = _uiState.asStateFlow()

    private var loadedSeriesId: String? = null

    fun load(seriesId: String) {
        if (loadedSeriesId == seriesId) return
        loadedSeriesId = seriesId

        viewModelScope.launch {
            val series = app.collectionRepository.seriesById(seriesId)
            _uiState.update { it.copy(series = series) }
        }
        viewModelScope.launch {
            app.collectionRepository.albumsForSeries(seriesId).collect { albums ->
                _uiState.update {
                    it.copy(
                        albums = albums,
                        ownedCount = albums.count { album -> album.owned },
                        totalCount = albums.size,
                    )
                }
            }
        }
    }
}
