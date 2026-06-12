package com.bdshelf.app.data.repo

import android.content.Context
import com.bdshelf.app.data.remote.ReleasesApi
import com.bdshelf.app.data.remote.ReleasesDocument
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.File

private const val CACHE_FILE_NAME = "releases_cache.json"
private const val DEFAULT_ASSET_NAME = "releases_default.json"

/**
 * Source de vérité des sorties (Moitié B, §3 SPEC).
 *
 * Ne stocke JAMAIS rien dans Room : le cache est un fichier JSON dans
 * `context.filesDir`. Hors-ligne, [loadCached] retourne le dernier cache,
 * ou le fichier par défaut embarqué si aucun cache n'existe encore.
 */
class ReleasesRepository(
    private val context: Context,
    private val api: ReleasesApi,
) {
    private val json = Json { ignoreUnknownKeys = true }

    private val cacheFile: File get() = File(context.filesDir, CACHE_FILE_NAME)

    suspend fun loadCached(): ReleasesDocument = withContext(Dispatchers.IO) {
        val cached = runCatching {
            cacheFile.takeIf { it.exists() }?.readText()?.let { json.decodeFromString<ReleasesDocument>(it) }
        }.getOrNull()
        cached ?: loadDefault()
    }

    private fun loadDefault(): ReleasesDocument {
        val text = context.assets.open(DEFAULT_ASSET_NAME).bufferedReader().use { it.readText() }
        return json.decodeFromString(text)
    }

    /** Tente de rafraîchir le cache depuis [url]. En cas d'échec, le cache existant est conservé. */
    suspend fun refresh(url: String): Result<ReleasesDocument> = withContext(Dispatchers.IO) {
        runCatching {
            val document = api.fetchReleases(url)
            cacheFile.writeText(json.encodeToString(document))
            document
        }
    }
}
