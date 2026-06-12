package com.bdshelf.app.ui.verdict

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.bdshelf.app.BdShelfApplication
import com.bdshelf.app.data.local.dao.SeriesWithCounts
import com.bdshelf.app.data.local.entities.Album
import com.bdshelf.app.data.local.entities.Series
import com.bdshelf.app.domain.normalizedForSearch
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

enum class VerdictOutcome { OWNED, MISSING, UNKNOWN }

data class VerdictUiState(
    val isLoading: Boolean = true,
    val ean: String = "",
    val outcome: VerdictOutcome = VerdictOutcome.UNKNOWN,
    val album: Album? = null,
    val series: Series? = null,
    val seriesAlbums: List<Album> = emptyList(),
    val seriesQuery: String = "",
    val seriesResults: List<SeriesWithCounts> = emptyList(),
    val selectedSeries: Series? = null,
    val selectedSeriesAlbums: List<Album> = emptyList(),
    val linkedAlbumId: String? = null,
)

/** Verdict de scan (§6.4) : trois états selon la correspondance entre l'EAN scanné et la collection. */
class VerdictViewModel(application: Application) : AndroidViewModel(application) {

    private val app = application as BdShelfApplication

    private val _uiState = MutableStateFlow(VerdictUiState())
    val uiState: StateFlow<VerdictUiState> = _uiState.asStateFlow()

    private var loaded = false
    private var allSeries: List<SeriesWithCounts> = emptyList()

    fun load(ean: String) {
        if (loaded) return
        loaded = true
        _uiState.update { it.copy(ean = ean) }

        viewModelScope.launch {
            val album = app.collectionRepository.albumByEan(ean)
            val series = album?.let { app.collectionRepository.seriesById(it.seriesId) }

            if (album != null && series != null) {
                if (album.owned) {
                    _uiState.update {
                        it.copy(isLoading = false, outcome = VerdictOutcome.OWNED, album = album, series = series)
                    }
                } else {
                    val albums = app.collectionRepository.albumsForSeries(series.id).first()
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            outcome = VerdictOutcome.MISSING,
                            album = album,
                            series = series,
                            seriesAlbums = albums,
                        )
                    }
                }
            } else {
                allSeries = app.collectionRepository.allSeriesWithCounts().first()
                _uiState.update {
                    it.copy(isLoading = false, outcome = VerdictOutcome.UNKNOWN, seriesResults = allSeries)
                }
            }
        }
    }

    /** État Manquant : « Je viens de l'acheter » -> bascule possédé (anime le tampon sur l'étagère). */
    fun onMarkOwned() {
        val album = _uiState.value.album ?: return
        viewModelScope.launch {
            app.collectionRepository.setOwned(album, true)
            val updated = album.copy(owned = true)
            val albums = app.collectionRepository.albumsForSeries(updated.seriesId).first()
            _uiState.update { it.copy(album = updated, seriesAlbums = albums) }
        }
    }

    /** État Inconnu : recherche d'une série pour lier le code-barres à un tome existant. */
    fun onSeriesQueryChange(query: String) {
        _uiState.update {
            it.copy(
                seriesQuery = query,
                seriesResults = allSeries.filter { s ->
                    s.title.normalizedForSearch().contains(query.normalizedForSearch())
                },
            )
        }
    }

    fun onSeriesSelected(seriesId: String) {
        viewModelScope.launch {
            val series = app.collectionRepository.seriesById(seriesId) ?: return@launch
            val albums = app.collectionRepository.albumsForSeries(seriesId).first()
            _uiState.update { it.copy(selectedSeries = series, selectedSeriesAlbums = albums) }
        }
    }

    fun onSeriesDeselected() {
        _uiState.update { it.copy(selectedSeries = null, selectedSeriesAlbums = emptyList()) }
    }

    /** Associe le code-barres scanné à un album existant : il sera reconnu aux scans suivants (§6.4). */
    fun onLinkAlbum(album: Album) {
        viewModelScope.launch {
            app.collectionRepository.linkEan(album, _uiState.value.ean)
            _uiState.update { it.copy(linkedAlbumId = album.id) }
        }
    }
}
