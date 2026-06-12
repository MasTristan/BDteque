package com.bdshelf.app.data.seed

import android.content.Context
import com.bdshelf.app.data.local.dao.AlbumDao
import com.bdshelf.app.data.local.dao.SeriesDao
import com.bdshelf.app.data.local.entities.Album
import com.bdshelf.app.data.local.entities.ReadStatus
import com.bdshelf.app.data.local.entities.Series
import com.bdshelf.app.data.local.entities.SeriesStatus
import com.bdshelf.app.domain.seriesSpineColor
import com.bdshelf.app.domain.toArgbLong
import kotlinx.serialization.json.Json

private const val SEED_ASSET_NAME = "seed-collection.json"

/**
 * Import du fichier d'amorçage `assets/seed-collection.json`.
 *
 * Exécuté une seule fois (guard : `seed_imported` dans les préférences).
 * Idempotent si relancé : tous les inserts sont des upserts (REPLACE),
 * donc relancer l'import régénère exactement le même état.
 */
class SeedImporter(
    private val context: Context,
    private val seriesDao: SeriesDao,
    private val albumDao: AlbumDao,
) {
    private val json = Json { ignoreUnknownKeys = true }

    suspend fun import() {
        val text = context.assets.open(SEED_ASSET_NAME).bufferedReader().use { it.readText() }
        val document = json.decodeFromString<SeedDocument>(text)
        val now = System.currentTimeMillis()

        for (seedSeries in document.series) {
            seriesDao.insert(
                Series(
                    id = seedSeries.id,
                    title = seedSeries.title,
                    status = runCatching { SeriesStatus.valueOf(seedSeries.status) }.getOrDefault(SeriesStatus.UNKNOWN),
                    isTracked = seedSeries.tracked,
                    color = seriesSpineColor(seedSeries.id).toArgbLong(),
                    knownTomeCount = seedSeries.knownTomeCount,
                    notes = seedSeries.notes.ifBlank { null },
                ),
            )

            val ownedTomes = seedSeries.ownedTomes.toSet()
            val maxOwned = ownedTomes.maxOrNull() ?: 0
            val albums = mutableListOf<Album>()

            // 1. Tomes possédés.
            for (n in ownedTomes) {
                albums += Album(
                    id = albumId(seedSeries.id, n),
                    seriesId = seedSeries.id,
                    tomeNumber = n,
                    title = null,
                    owned = true,
                    readStatus = ReadStatus.UNREAD,
                    edition = null,
                    ean = null,
                    dateAdded = now,
                )
            }

            // 2. Tomes connus mais non possédés au-delà du dernier tome possédé.
            val knownTomeCount = seedSeries.knownTomeCount
            if (knownTomeCount != null && knownTomeCount > maxOwned) {
                for (n in (maxOwned + 1)..knownTomeCount) {
                    albums += missingAlbum(seedSeries.id, n, now)
                }
            }

            // 3. Trous internes entre 1 et le dernier tome possédé.
            for (n in 1 until maxOwned) {
                if (n !in ownedTomes) {
                    albums += missingAlbum(seedSeries.id, n, now)
                }
            }

            albumDao.insertAll(albums)
        }
    }

    private fun missingAlbum(seriesId: String, tomeNumber: Int, now: Long) = Album(
        id = albumId(seriesId, tomeNumber),
        seriesId = seriesId,
        tomeNumber = tomeNumber,
        title = null,
        owned = false,
        readStatus = ReadStatus.UNREAD,
        edition = null,
        ean = null,
        dateAdded = now,
    )

    private fun albumId(seriesId: String, tomeNumber: Int) = "$seriesId-$tomeNumber"
}
