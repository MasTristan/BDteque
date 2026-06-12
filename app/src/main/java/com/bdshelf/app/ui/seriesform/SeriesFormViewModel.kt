package com.bdshelf.app.ui.seriesform

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.bdshelf.app.BdShelfApplication
import com.bdshelf.app.data.local.entities.Series
import com.bdshelf.app.data.local.entities.SeriesStatus
import com.bdshelf.app.domain.seriesSpineColor
import com.bdshelf.app.domain.toArgbLong
import com.bdshelf.app.domain.toSlug
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class SeriesFormUiState(
    val isLoading: Boolean = true,
    val isNew: Boolean = true,
    val seriesId: String? = null,
    val title: String = "",
    val status: SeriesStatus = SeriesStatus.ONGOING,
    val isTracked: Boolean = true,
    val knownTomeCountText: String = "",
    val notes: String = "",
    val titleError: Boolean = false,
    val saved: Boolean = false,
    val deleted: Boolean = false,
)

/** Ajout/édition manuelle d'une série (§6.8, PLUS). */
class SeriesFormViewModel(application: Application) : AndroidViewModel(application) {

    private val app = application as BdShelfApplication

    private val _uiState = MutableStateFlow(SeriesFormUiState())
    val uiState: StateFlow<SeriesFormUiState> = _uiState.asStateFlow()

    private var loaded = false

    fun load(seriesId: String?) {
        if (loaded) return
        loaded = true

        if (seriesId == null) {
            _uiState.update { it.copy(isLoading = false, isNew = true) }
            return
        }

        viewModelScope.launch {
            val series = app.collectionRepository.seriesById(seriesId)
            if (series != null) {
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        isNew = false,
                        seriesId = series.id,
                        title = series.title,
                        status = series.status,
                        isTracked = series.isTracked,
                        knownTomeCountText = series.knownTomeCount?.toString() ?: "",
                        notes = series.notes.orEmpty(),
                    )
                }
            }
        }
    }

    fun onTitleChange(value: String) {
        _uiState.update { it.copy(title = value, titleError = false) }
    }

    fun onStatusChange(value: SeriesStatus) {
        _uiState.update { it.copy(status = value) }
    }

    fun onTrackedChange(value: Boolean) {
        _uiState.update { it.copy(isTracked = value) }
    }

    fun onKnownTomeCountChange(text: String) {
        _uiState.update { it.copy(knownTomeCountText = text.filter { c -> c.isDigit() }) }
    }

    fun onNotesChange(value: String) {
        _uiState.update { it.copy(notes = value) }
    }

    /** Enregistre. Le titre est obligatoire (§6.8). */
    fun onSave() {
        val state = _uiState.value
        val title = state.title.trim()
        if (title.isBlank()) {
            _uiState.update { it.copy(titleError = true) }
            return
        }
        val knownTomeCount = state.knownTomeCountText.toIntOrNull()
        val notes = state.notes.trim().ifBlank { null }

        viewModelScope.launch {
            if (state.isNew) {
                val id = generateSeriesId(title)
                val series = Series(
                    id = id,
                    title = title,
                    status = state.status,
                    isTracked = state.isTracked,
                    color = seriesSpineColor(id).toArgbLong(),
                    knownTomeCount = knownTomeCount,
                    notes = notes,
                )
                app.collectionRepository.upsertSeries(series)
            } else {
                val existing = app.collectionRepository.seriesById(state.seriesId!!) ?: return@launch
                val updated = existing.copy(
                    title = title,
                    status = state.status,
                    isTracked = state.isTracked,
                    knownTomeCount = knownTomeCount,
                    notes = notes,
                )
                app.collectionRepository.updateSeries(updated)
            }
            _uiState.update { it.copy(saved = true) }
        }
    }

    /** Slug stable dérivé du titre, désambiguïsé en cas de collision. */
    private suspend fun generateSeriesId(title: String): String {
        val base = title.toSlug().ifBlank { "serie" }
        var candidate = base
        var suffix = 2
        while (app.collectionRepository.seriesById(candidate) != null) {
            candidate = "$base-$suffix"
            suffix++
        }
        return candidate
    }

    /** Supprime la série et, par cascade, tous ses albums. */
    fun onDeleteConfirmed() {
        val seriesId = _uiState.value.seriesId ?: return
        viewModelScope.launch {
            val existing = app.collectionRepository.seriesById(seriesId) ?: return@launch
            app.collectionRepository.deleteSeries(existing)
            _uiState.update { it.copy(deleted = true) }
        }
    }
}
