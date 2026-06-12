package com.bdshelf.app.ui.albumform

import android.app.Application
import androidx.compose.ui.graphics.Color
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.bdshelf.app.BdShelfApplication
import com.bdshelf.app.data.local.entities.ReadStatus
import com.bdshelf.app.domain.GapDetector
import com.bdshelf.app.domain.toSpineColor
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class AlbumFormUiState(
    val isLoading: Boolean = true,
    val isNew: Boolean = true,
    val seriesId: String = "",
    val seriesColor: Color = Color.Gray,
    val tomeNumber: Int? = null,
    val tomeNumberText: String = "",
    val isUnnumbered: Boolean = false,
    val title: String = "",
    val owned: Boolean = false,
    val readStatus: ReadStatus = ReadStatus.UNREAD,
    val edition: String = "",
    val ean: String? = null,
    val duplicateError: Boolean = false,
    val saved: Boolean = false,
    val deleted: Boolean = false,
)

/** Fiche tome (§6.6, §6.8) : ajout ou édition d'un album, toggle possédé. */
class AlbumFormViewModel(application: Application) : AndroidViewModel(application) {

    private val app = application as BdShelfApplication

    private val _uiState = MutableStateFlow(AlbumFormUiState())
    val uiState: StateFlow<AlbumFormUiState> = _uiState.asStateFlow()

    private var albumId: String? = null
    private var loaded = false

    fun load(seriesId: String, albumId: String?, prefilledTomeNumber: Int?) {
        if (loaded) return
        loaded = true
        this.albumId = albumId

        viewModelScope.launch {
            val series = app.collectionRepository.seriesById(seriesId)
            val seriesColor = series?.color?.toSpineColor() ?: Color.Gray

            if (albumId != null) {
                val album = app.collectionRepository.albumById(albumId)
                if (album != null) {
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            isNew = false,
                            seriesId = seriesId,
                            seriesColor = seriesColor,
                            tomeNumber = album.tomeNumber,
                            tomeNumberText = album.tomeNumber?.toString() ?: "",
                            isUnnumbered = album.tomeNumber == null,
                            title = album.title.orEmpty(),
                            owned = album.owned,
                            readStatus = album.readStatus,
                            edition = album.edition.orEmpty(),
                            ean = album.ean,
                        )
                    }
                }
            } else {
                val albums = app.collectionRepository.albumsForSeries(seriesId).first()
                val tomeNumber = prefilledTomeNumber ?: GapDetector.nextTomeNumber(albums)
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        isNew = true,
                        seriesId = seriesId,
                        seriesColor = seriesColor,
                        tomeNumber = tomeNumber,
                        tomeNumberText = tomeNumber.toString(),
                        isUnnumbered = false,
                    )
                }
            }
        }
    }

    fun onTomeNumberChange(text: String) {
        val digitsOnly = text.filter { it.isDigit() }
        _uiState.update { it.copy(tomeNumberText = digitsOnly, duplicateError = false) }
    }

    fun onUnnumberedChange(value: Boolean) {
        _uiState.update { it.copy(isUnnumbered = value, duplicateError = false) }
    }

    fun onTitleChange(value: String) {
        _uiState.update { it.copy(title = value) }
    }

    fun onOwnedChange(value: Boolean) {
        _uiState.update { it.copy(owned = value) }
    }

    fun onReadStatusChange(value: ReadStatus) {
        _uiState.update { it.copy(readStatus = value) }
    }

    fun onEditionChange(value: String) {
        _uiState.update { it.copy(edition = value) }
    }

    /** Enregistre. Si le numéro de tome existe déjà dans la série, affiche une erreur claire (§6.8). */
    fun onSave() {
        val state = _uiState.value
        val tomeNumber = if (state.isUnnumbered) null else state.tomeNumberText.toIntOrNull()

        viewModelScope.launch {
            val isDuplicate = app.collectionRepository.isDuplicateTome(state.seriesId, tomeNumber, excludingAlbumId = albumId)
            if (isDuplicate) {
                _uiState.update { it.copy(duplicateError = true) }
                return@launch
            }

            val currentAlbumId = albumId
            if (currentAlbumId == null) {
                val created = app.collectionRepository.addAlbum(
                    seriesId = state.seriesId,
                    tomeNumber = tomeNumber,
                    title = state.title.trim().ifBlank { null },
                    owned = state.owned,
                    readStatus = state.readStatus,
                    edition = state.edition.trim().ifBlank { null },
                )
                if (created == null) {
                    _uiState.update { it.copy(duplicateError = true) }
                    return@launch
                }
            } else {
                val existing = app.collectionRepository.albumById(currentAlbumId) ?: return@launch
                val becameOwned = !existing.owned && state.owned
                val updated = existing.copy(
                    title = state.title.trim().ifBlank { null },
                    owned = state.owned,
                    readStatus = state.readStatus,
                    edition = state.edition.trim().ifBlank { null },
                    dateAdded = if (becameOwned) System.currentTimeMillis() else existing.dateAdded,
                )
                app.collectionRepository.updateAlbum(updated)
            }

            _uiState.update { it.copy(saved = true) }
        }
    }

    fun onDeleteConfirmed() {
        val currentAlbumId = albumId ?: return
        viewModelScope.launch {
            val existing = app.collectionRepository.albumById(currentAlbumId) ?: return@launch
            app.collectionRepository.deleteAlbum(existing)
            _uiState.update { it.copy(deleted = true) }
        }
    }
}
