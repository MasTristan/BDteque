package com.bdshelf.app.ui.settings

import android.app.Application
import android.net.Uri
import androidx.core.content.FileProvider
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.bdshelf.app.BdShelfApplication
import com.bdshelf.app.BuildConfig
import com.bdshelf.app.data.prefs.ThemeMode
import com.bdshelf.app.data.repo.CollectionSnapshot
import com.bdshelf.app.domain.SnapshotValidation
import com.bdshelf.app.domain.toCsv
import com.bdshelf.app.domain.validate
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.File

data class SettingsUiState(
    val isLoading: Boolean = true,
    val ownerName: String = "",
    val notificationsEnabled: Boolean = false,
    val themeMode: ThemeMode = ThemeMode.SYSTEM,
    val downloadCovers: Boolean = false,
    val isRefreshing: Boolean = false,
    val refreshError: Boolean = false,
    val releasesUrlOverride: String = "",
    val exportUri: Uri? = null,
    val exportMimeType: String = "",
    val importSuccess: Boolean = false,
    val importError: Boolean = false,
    val lastBackupAt: Long? = null,
    val isBackingUp: Boolean = false,
)

/** Réglages & À propos (§6.9). */
class SettingsViewModel(application: Application) : AndroidViewModel(application) {

    private val app = application as BdShelfApplication

    private val _uiState = MutableStateFlow(SettingsUiState())
    val uiState: StateFlow<SettingsUiState> = _uiState.asStateFlow()

    private val json = Json {
        prettyPrint = true
        ignoreUnknownKeys = true
    }

    private var loaded = false
    private var ownerNameJob: Job? = null

    fun load() {
        if (loaded) return
        loaded = true
        viewModelScope.launch {
            val ownerName = app.userPreferencesRepository.ownerName.first()
            val notificationsEnabled = app.userPreferencesRepository.notificationsEnabled.first()
            val releasesUrlOverride = app.userPreferencesRepository.releasesUrlOverride.first() ?: ""
            val themeMode = app.userPreferencesRepository.themeMode.first()
            val downloadCovers = app.userPreferencesRepository.downloadCovers.first()
            val lastBackupAt = app.backupManager.lastBackupAt()
            _uiState.update {
                it.copy(
                    isLoading = false,
                    ownerName = ownerName,
                    notificationsEnabled = notificationsEnabled,
                    themeMode = themeMode,
                    downloadCovers = downloadCovers,
                    releasesUrlOverride = releasesUrlOverride,
                    lastBackupAt = lastBackupAt,
                )
            }
        }
    }

    /** Réglage Apparence : appliqué immédiatement, MainActivity observe la préférence. */
    fun onThemeModeChange(mode: ThemeMode) {
        _uiState.update { it.copy(themeMode = mode) }
        viewModelScope.launch { app.userPreferencesRepository.setThemeMode(mode) }
    }

    /** Téléchargement des couvertures (§6.4) : opt-in, voir [com.bdshelf.app.data.covers.CoverRepository]. */
    fun onDownloadCoversToggle(enabled: Boolean) {
        _uiState.update { it.copy(downloadCovers = enabled) }
        viewModelScope.launch { app.userPreferencesRepository.setDownloadCovers(enabled) }
    }

    /** Bouton « Sauvegarder maintenant » : sauvegarde locale immédiate (§6.9). */
    fun onBackupNow() {
        viewModelScope.launch {
            _uiState.update { it.copy(isBackingUp = true) }
            app.backupManager.backupNow()
            _uiState.update { it.copy(isBackingUp = false, lastBackupAt = app.backupManager.lastBackupAt()) }
        }
    }

    fun onOwnerNameChange(value: String) {
        _uiState.update { it.copy(ownerName = value) }
        // Debounce : une seule écriture DataStore après la fin de la saisie,
        // plutôt qu'une écriture par frappe.
        ownerNameJob?.cancel()
        ownerNameJob = viewModelScope.launch {
            delay(OWNER_NAME_DEBOUNCE_MS)
            app.userPreferencesRepository.setOwnerName(value)
        }
    }

    /** [enabled] reflète la permission `POST_NOTIFICATIONS` accordée ou non (§6.9). */
    fun onNotificationsToggle(enabled: Boolean) {
        _uiState.update { it.copy(notificationsEnabled = enabled) }
        viewModelScope.launch { app.userPreferencesRepository.setNotificationsEnabled(enabled) }
    }

    fun onReleasesUrlOverrideChange(value: String) {
        _uiState.update { it.copy(releasesUrlOverride = value) }
        viewModelScope.launch { app.userPreferencesRepository.setReleasesUrlOverride(value) }
    }

    /** Bouton « Mettre à jour les nouveautés maintenant » (§6.9). */
    fun onRefreshReleases() {
        viewModelScope.launch {
            _uiState.update { it.copy(isRefreshing = true, refreshError = false) }
            val url = _uiState.value.releasesUrlOverride.takeIf { it.isNotBlank() } ?: BuildConfig.RELEASES_URL
            val result = app.releasesRepository.refresh(url)
            _uiState.update { it.copy(isRefreshing = false, refreshError = result.isFailure) }
        }
    }

    /** Export JSON (sauvegarde complète, §6.9). */
    fun onExportJson() {
        viewModelScope.launch {
            val snapshot = app.collectionRepository.exportSnapshot()
            val uri = writeExportFile("bdshelf_collection.json", json.encodeToString(snapshot))
            _uiState.update { it.copy(exportUri = uri, exportMimeType = "application/json") }
        }
    }

    /** Export CSV lisible (§6.9). */
    fun onExportCsv() {
        viewModelScope.launch {
            val snapshot = app.collectionRepository.exportSnapshot()
            val uri = writeExportFile("bdshelf_collection.csv", snapshot.toCsv())
            _uiState.update { it.copy(exportUri = uri, exportMimeType = "text/csv") }
        }
    }

    /** Réinitialise l'événement d'export une fois le partage lancé. */
    fun onExportHandled() {
        _uiState.update { it.copy(exportUri = null, exportMimeType = "") }
    }

    /** Import depuis un fichier de sauvegarde JSON (§6.9). */
    fun onImportFile(uri: Uri) {
        viewModelScope.launch {
            val result = runCatching {
                val text = withContext(Dispatchers.IO) {
                    app.contentResolver.openInputStream(uri)?.bufferedReader()?.use { it.readText() }
                        ?: error("Fichier introuvable")
                }
                val snapshot = json.decodeFromString<CollectionSnapshot>(text)
                val validation = snapshot.validate()
                if (validation is SnapshotValidation.Invalid) {
                    error(validation.reason)
                }
                // Point de restauration : la collection actuelle est sauvegardée
                // avant d'être remplacée, pour pouvoir revenir en arrière si le
                // fichier importé n'était pas le bon (§6.9).
                app.backupManager.backupNow()
                // La collection n'est effacée que si le fichier est valide et complet.
                app.collectionRepository.importSnapshot(snapshot)
            }
            _uiState.update { it.copy(importSuccess = result.isSuccess, importError = result.isFailure) }
        }
    }

    fun onImportMessageShown() {
        _uiState.update { it.copy(importSuccess = false, importError = false) }
    }

    private suspend fun writeExportFile(fileName: String, content: String): Uri = withContext(Dispatchers.IO) {
        val dir = File(app.cacheDir, "exports").apply { mkdirs() }
        val file = File(dir, fileName)
        file.writeText(content)
        FileProvider.getUriForFile(app, "${app.packageName}.fileprovider", file)
    }

    private companion object {
        const val OWNER_NAME_DEBOUNCE_MS = 400L
    }
}
