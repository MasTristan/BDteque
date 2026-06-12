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

/** Préférences utilisateur : prénom du destinataire, état de l'import seed, réglages avancés. */
class UserPreferencesRepository(private val context: Context) {

    private object Keys {
        val OWNER_NAME = stringPreferencesKey("owner_name")
        val SEED_IMPORTED = booleanPreferencesKey("seed_imported")
        val NOTIFICATIONS_ENABLED = booleanPreferencesKey("notifications_enabled")
        val RELEASES_URL_OVERRIDE = stringPreferencesKey("releases_url_override")
        val NOTIFIED_RELEASE_KEYS = stringSetPreferencesKey("notified_release_keys")
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
}
