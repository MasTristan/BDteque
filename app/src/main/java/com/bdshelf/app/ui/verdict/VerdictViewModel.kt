package com.bdshelf.app.ui.verdict

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.bdshelf.app.BdShelfApplication
import com.bdshelf.app.data.local.dao.SeriesWithCounts
import com.bdshelf.app.data.local.entities.Album
import com.bdshelf.app.data.local.entities.Series
import com.bdshelf.app.domain.guessSeries
import com.bdshelf.app.domain.guessSeriesName
import com.bdshelf.app.domain.guessTomeNumber
import com.bdshelf.app.domain.normalizedForSearch
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.io.File

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
    val identifiedTitle: String? = null,
    val identifiedAuthors: String? = null,
    val suggestedSeriesId: String? = null,
    val suggestedSeriesTitle: String? = null,
    val suggestedNewSeriesName: String? = null,
    val suggestedTomeNumber: Int? = null,
    val suggestionDismissed: Boolean = false,
    val coverFile: File? = null,
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
                loadSuggestion(ean)
            }
            loadCover(ean)
        }
    }

    /**
     * Couverture de l'album scanné (§6.4), en tâche de fond : confirmation
     * visuelle que le bon livre a été identifié. Fichier local d'abord,
     * téléchargement une fois si le réglage Couvertures est activé.
     */
    private fun loadCover(ean: String) {
        viewModelScope.launch {
            val file = app.coverRepository.ensureCover(ean) ?: return@launch
            _uiState.update { it.copy(coverFile = file) }
        }
    }

    /**
     * Identifie le livre scanné (BnF puis Open Library) en tâche de fond, sans
     * bloquer l'affichage de l'état "Inconnu" (§6.4). Affiche le titre et
     * l'auteur trouvés, puis :
     * - si une série connue correspond (nom de collection BnF ou titre),
     *   la propose avec son tome ;
     * - sinon propose de créer une nouvelle série à partir du nom de collection
     *   (ou, à défaut, deviné depuis le titre).
     *
     * Le numéro de tome vient en priorité de la donnée structurée BnF
     * (collection 225$v), sinon d'une heuristique sur le titre. Dégradation
     * silencieuse : aucune suggestion si hors-ligne ou ISBN inconnu.
     */
    private fun loadSuggestion(ean: String) {
        viewModelScope.launch {
            val book = app.isbnLookupService.lookup(ean) ?: return@launch
            _uiState.update {
                it.copy(identifiedTitle = book.title, identifiedAuthors = book.authorsLabel())
            }

            val candidates = allSeries.map { it.id to it.title }
            val tomeNumber = book.tomeNumber
                ?: book.seriesName?.let(::guessTomeNumber)
                ?: guessTomeNumber(book.title)
            val seriesId = book.seriesName?.let { guessSeries(it, candidates) }
                ?: guessSeries(book.title, candidates)

            if (seriesId != null) {
                val seriesTitle = allSeries.first { it.id == seriesId }.title
                _uiState.update {
                    it.copy(
                        suggestedSeriesId = seriesId,
                        suggestedSeriesTitle = seriesTitle,
                        suggestedTomeNumber = tomeNumber,
                    )
                }
            } else {
                val newSeriesName = book.seriesName ?: guessSeriesName(book.title) ?: return@launch
                _uiState.update {
                    it.copy(suggestedNewSeriesName = newSeriesName, suggestedTomeNumber = tomeNumber)
                }
            }
        }
    }

    /** Accepte la suggestion : présélectionne la série, comme un choix manuel dans la liste. */
    fun onAcceptSuggestion() {
        val seriesId = _uiState.value.suggestedSeriesId ?: return
        onSeriesSelected(seriesId)
    }

    /** Rejette la suggestion : revient à la recherche manuelle habituelle. */
    fun onDismissSuggestion() {
        _uiState.update { it.copy(suggestionDismissed = true) }
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
