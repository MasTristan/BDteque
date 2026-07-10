package com.bdshelf.app.ui.shopping

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.bdshelf.app.BdShelfApplication
import com.bdshelf.app.data.local.entities.ReadStatus
import com.bdshelf.app.domain.ReleaseWithOwnership
import com.bdshelf.app.domain.ShoppingGroup
import com.bdshelf.app.domain.ShoppingItem
import com.bdshelf.app.domain.buildShoppingList
import com.bdshelf.app.domain.crossReferenceReleases
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class ShoppingUiState(
    val isLoading: Boolean = true,
    val groups: List<ShoppingGroup> = emptyList(),
)

/**
 * Liste d'achats « À acheter » (§6.10) : tomes manquants de la collection +
 * sorties parues des séries suivies, en une seule liste de courses.
 *
 * Cocher un item = « je l'ai acheté » : l'album passe possédé (ou est créé
 * possédé s'il venait d'une sortie), et l'item disparaît de la liste par
 * réactivité des flows — la liste EST l'état de la collection.
 */
class ShoppingViewModel(application: Application) : AndroidViewModel(application) {

    private val app = application as BdShelfApplication

    // Les sorties ne sont pas un flow (cache fichier, §4.3) : chargées une fois,
    // croisées ensuite avec les flows réactifs de la collection.
    private val releases = MutableStateFlow<List<ReleaseWithOwnership>>(emptyList())

    val uiState: StateFlow<ShoppingUiState> = combine(
        app.collectionRepository.allSeries(),
        app.collectionRepository.allAlbums(),
        releases,
    ) { series, albums, releasedUnowned ->
        ShoppingUiState(isLoading = false, groups = buildShoppingList(series, albums, releasedUnowned))
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), ShoppingUiState())

    init {
        viewModelScope.launch {
            val document = app.releasesRepository.loadCached()
            val tracked = app.collectionRepository.trackedSeriesIds()
            val owned = app.collectionRepository.ownedTomeRefs()
            releases.value = crossReferenceReleases(document.releases, tracked, owned)
        }
    }

    /** « Je l'ai acheté » : bascule possédé, ou crée la fiche possédée pour une sortie. */
    fun onItemBought(item: ShoppingItem) {
        viewModelScope.launch {
            val albumId = item.albumId
            if (albumId != null) {
                val album = app.collectionRepository.albumById(albumId) ?: return@launch
                app.collectionRepository.setOwned(album, true)
            } else {
                app.collectionRepository.addAlbum(
                    seriesId = item.seriesId,
                    tomeNumber = item.tomeNumber,
                    title = item.title,
                    owned = true,
                    readStatus = ReadStatus.UNREAD,
                    edition = null,
                )
            }
        }
    }
}
