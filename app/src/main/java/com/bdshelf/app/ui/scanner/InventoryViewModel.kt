package com.bdshelf.app.ui.scanner

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.bdshelf.app.BdShelfApplication
import com.bdshelf.app.data.local.entities.Album
import com.bdshelf.app.data.local.entities.ReadStatus
import com.bdshelf.app.domain.BarcodeConfirmer
import com.bdshelf.app.domain.canonicalEan
import com.bdshelf.app.domain.guessSeries
import com.bdshelf.app.domain.guessTomeNumber
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/** État d'une lecture du mode inventaire (§6.11). */
enum class InventoryScanStatus {
    IDENTIFYING, // verdict en cours de calcul
    OWNED, // déjà dans la collection, possédé
    MISSING, // dans la collection, mais pas possédé
    SUGGESTED, // inconnu, mais série + tome reconnus : ajout en un geste
    UNKNOWN, // inconnu, sans suggestion exploitable : renvoyer au verdict complet
}

data class InventoryScan(
    val ean: String,
    val status: InventoryScanStatus,
    val seriesId: String? = null,
    val seriesTitle: String? = null,
    val tomeNumber: Int? = null,
    val albumId: String? = null, // renseigné pour OWNED/MISSING
    val identifiedTitle: String? = null,
    val actionTaken: Boolean = false, // "je l'ai" / "ajouter" déjà effectué
    // Annulation d'une action : instantané de l'album avant modification, ou
    // identifiant de l'album créé. Un scan en rafale se prête aux faux gestes.
    val undoAlbum: Album? = null,
    val createdAlbumId: String? = null,
) {
    val canUndo: Boolean get() = actionTaken && (undoAlbum != null || createdAlbumId != null)
}

data class InventoryUiState(
    val scans: List<InventoryScan> = emptyList(), // plus récent en tête
) {
    val scannedCount: Int get() = scans.size
    val addedCount: Int get() = scans.count { it.actionTaken }
}

/**
 * Mode inventaire (§6.11) : scan en rafale pour cataloguer une pile d'albums
 * sans quitter la caméra. Chaque code confirmé ([BarcodeConfirmer]) reçoit un
 * mini-verdict sur place :
 * - possédé → simple confirmation ;
 * - manquant → « Je l'ai » bascule possédé ;
 * - inconnu identifié (série + tome reconnus, §6.4) → ajout en un geste ;
 * - inconnu sans suggestion → renvoi vers le verdict complet.
 *
 * Un même code n'est traité qu'une fois par session : le confirmer se réarme
 * en continu tant que le livre reste visé, la liste dédoublonne.
 */
class InventoryViewModel(application: Application) : AndroidViewModel(application) {

    private val app = application as BdShelfApplication

    private val _uiState = MutableStateFlow(InventoryUiState())
    val uiState: StateFlow<InventoryUiState> = _uiState.asStateFlow()

    private val confirmer = BarcodeConfirmer()

    fun onBarcodeDetected(raw: String) {
        val ean = confirmer.offer(raw) ?: return
        handleConfirmed(ean)
    }

    /** Saisie manuelle (code-barres abîmé) : même mini-verdict que le scan. */
    fun onManualEan(raw: String) {
        val ean = canonicalEan(raw) ?: return
        handleConfirmed(ean)
    }

    private fun handleConfirmed(ean: String) {
        // Déjà traité cette session (le livre est encore devant la caméra) : rien à faire.
        if (_uiState.value.scans.any { it.ean == ean }) return
        _uiState.update {
            it.copy(scans = listOf(InventoryScan(ean = ean, status = InventoryScanStatus.IDENTIFYING)) + it.scans)
        }
        viewModelScope.launch { resolve(ean) }
    }

    private suspend fun resolve(ean: String) {
        val album = app.collectionRepository.albumByEan(ean)
        if (album != null) {
            val series = app.collectionRepository.seriesById(album.seriesId)
            updateScan(ean) {
                it.copy(
                    status = if (album.owned) InventoryScanStatus.OWNED else InventoryScanStatus.MISSING,
                    seriesId = album.seriesId,
                    seriesTitle = series?.title,
                    tomeNumber = album.tomeNumber,
                    albumId = album.id,
                    identifiedTitle = album.title,
                )
            }
            return
        }

        // Inconnu : identification ISBN + suggestion série/tome, comme au verdict (§6.4).
        val book = app.isbnLookupService.lookup(ean)
        // Couverture opportuniste (opt-in) : prête pour le futur verdict ou la fiche.
        viewModelScope.launch { app.coverRepository.ensureCover(ean) }
        if (book == null) {
            updateScan(ean) { it.copy(status = InventoryScanStatus.UNKNOWN) }
            return
        }

        val candidates = app.collectionRepository.allSeriesWithCounts().first().map { it.id to it.title }
        val tomeNumber = book.tomeNumber
            ?: book.seriesName?.let(::guessTomeNumber)
            ?: guessTomeNumber(book.title)
        val seriesId = book.seriesName?.let { guessSeries(it, candidates) }
            ?: guessSeries(book.title, candidates)

        if (seriesId != null && tomeNumber != null) {
            val seriesTitle = candidates.first { it.first == seriesId }.second
            updateScan(ean) {
                it.copy(
                    status = InventoryScanStatus.SUGGESTED,
                    seriesId = seriesId,
                    seriesTitle = seriesTitle,
                    tomeNumber = tomeNumber,
                    identifiedTitle = book.title,
                )
            }
        } else {
            updateScan(ean) { it.copy(status = InventoryScanStatus.UNKNOWN, identifiedTitle = book.title) }
        }
    }

    /** MISSING : « Je l'ai » — bascule l'album possédé, comme au verdict. */
    fun onMarkOwned(scan: InventoryScan) {
        val albumId = scan.albumId ?: return
        viewModelScope.launch {
            val album = app.collectionRepository.albumById(albumId) ?: return@launch
            app.collectionRepository.setOwned(album, true)
            // `album` est l'état d'avant : c'est l'instantané de restauration.
            updateScan(scan.ean) { it.copy(actionTaken = true, undoAlbum = album) }
        }
    }

    /**
     * SUGGESTED : ajoute le tome à la série reconnue, possédé, avec l'EAN
     * appris. Si le tome existe déjà (vide de l'étagère), il est relié et
     * marqué possédé plutôt que dupliqué.
     */
    fun onAddSuggested(scan: InventoryScan) {
        val seriesId = scan.seriesId ?: return
        val tomeNumber = scan.tomeNumber ?: return
        viewModelScope.launch {
            val existing = app.collectionRepository.albumBySeriesAndTome(seriesId, tomeNumber)
            if (existing != null) {
                app.collectionRepository.linkEan(existing, scan.ean)
                app.collectionRepository.setOwned(existing.copy(ean = scan.ean), true)
                // Restaurer `existing` annule aussi l'écrasement de son EAN par linkEan.
                updateScan(scan.ean) { it.copy(actionTaken = true, undoAlbum = existing) }
            } else {
                val created = app.collectionRepository.addAlbum(
                    seriesId = seriesId,
                    tomeNumber = tomeNumber,
                    title = scan.identifiedTitle,
                    owned = true,
                    readStatus = ReadStatus.UNREAD,
                    edition = null,
                    ean = scan.ean,
                )
                updateScan(scan.ean) { it.copy(actionTaken = true, createdAlbumId = created?.id) }
            }
        }
    }

    /** Annule la dernière action de ce scan : restaure l'instantané, ou supprime l'album créé. */
    fun onUndo(scan: InventoryScan) {
        viewModelScope.launch {
            val createdAlbumId = scan.createdAlbumId
            val undoAlbum = scan.undoAlbum
            when {
                createdAlbumId != null ->
                    app.collectionRepository.albumById(createdAlbumId)?.let { app.collectionRepository.deleteAlbum(it) }
                undoAlbum != null -> app.collectionRepository.updateAlbum(undoAlbum)
                else -> return@launch
            }
            updateScan(scan.ean) { it.copy(actionTaken = false, undoAlbum = null, createdAlbumId = null) }
        }
    }

    private fun updateScan(ean: String, transform: (InventoryScan) -> InventoryScan) {
        _uiState.update { state ->
            state.copy(scans = state.scans.map { if (it.ean == ean) transform(it) else it })
        }
    }
}
