package com.bdshelf.app.data.prefs

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore by preferencesDataStore(name = "bdshelf_prefs")

/** Apparence de l'application : suit le système, ou forcée claire/sombre (§6.9). */
enum class ThemeMode {
    SYSTEM,
    LIGHT,
    DARK,
}

/** Préférences utilisateur : prénom du destinataire, état de l'import seed, réglages avancés. */
class UserPreferencesRepository(private val context: Context) {

    private object Keys {
        val OWNER_NAME = stringPreferencesKey("owner_name")
        val SEED_IMPORTED = booleanPreferencesKey("seed_imported")
        val NOTIFICATIONS_ENABLED = booleanPreferencesKey("notifications_enabled")
        val RELEASES_URL_OVERRIDE = stringPreferencesKey("releases_url_override")
        val NOTIFIED_RELEASE_KEYS = stringSetPreferencesKey("notified_release_keys")
        val THEME_MODE = stringPreferencesKey("theme_mode")
        val DOWNLOAD_COVERS = booleanPreferencesKey("download_covers")
    }

    val ownerName: Flow<String> = context.dataStore.data.map { it[Keys.OWNER_NAME] ?: "" }

    val seedImported: Flow<Boolean> = context.dataStore.data.map { it[Keys.SEED_IMPORTED] ?: false }

    val notificationsEnabled: Flow<Boolean> = context.dataStore.data.map { it[Keys.NOTIFICATIONS_ENABLED] ?: true }

    val releasesUrlOverride: Flow<String?> = context.dataStore.data.map { it[Keys.RELEASES_URL_OVERRIDE] }

    /** Sorties déjà notifiées (clé "seriesId-tomeNumber"), pour ne jamais notifier deux fois (§7). */
    val notifiedReleaseKeys: Flow<Set<String>> = context.dataStore.data.map { it[Keys.NOTIFIED_RELEASE_KEYS] ?: emptySet() }

    suspend fun setOwnerName(name: String) {
        context.dataStore.edit { it[Keys.OWNER_NAME] = name }
    }

    suspend fun setSeedImported(value: Boolean) {
        context.dataStore.edit { it[Keys.SEED_IMPORTED] = value }
    }

    suspend fun setNotificationsEnabled(value: Boolean) {
        context.dataStore.edit { it[Keys.NOTIFICATIONS_ENABLED] = value }
    }

    suspend fun setReleasesUrlOverride(url: String?) {
        context.dataStore.edit {
            if (url.isNullOrBlank()) it.remove(Keys.RELEASES_URL_OVERRIDE) else it[Keys.RELEASES_URL_OVERRIDE] = url
        }
    }

    suspend fun addNotifiedReleaseKeys(keys: Set<String>) {
        context.dataStore.edit {
            val current = it[Keys.NOTIFIED_RELEASE_KEYS] ?: emptySet()
            it[Keys.NOTIFIED_RELEASE_KEYS] = current + keys
        }
    }

    /** Valeur inconnue (préférence jamais écrite, ou future valeur retirée) = SYSTEM. */
    val themeMode: Flow<ThemeMode> = context.dataStore.data.map { prefs ->
        prefs[Keys.THEME_MODE]?.let { stored -> ThemeMode.entries.firstOrNull { it.name == stored } }
            ?: ThemeMode.SYSTEM
    }

    suspend fun setThemeMode(mode: ThemeMode) {
        context.dataStore.edit { it[Keys.THEME_MODE] = mode.name }
    }

    /**
     * Téléchargement des couvertures d'albums (§6.4). Désactivé par défaut :
     * l'app reste 100 % hors ligne tant que l'utilisateur n'a pas choisi le
     * contraire — et une fois téléchargée, une couverture est servie depuis le
     * stockage local, jamais depuis le réseau.
     */
    val downloadCovers: Flow<Boolean> = context.dataStore.data.map { it[Keys.DOWNLOAD_COVERS] ?: false }

    suspend fun setDownloadCovers(value: Boolean) {
        context.dataStore.edit { it[Keys.DOWNLOAD_COVERS] = value }
    }
}
