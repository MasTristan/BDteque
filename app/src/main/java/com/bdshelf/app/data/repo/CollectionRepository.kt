package com.bdshelf.app.data.repo

import com.bdshelf.app.data.local.dao.AlbumDao
import com.bdshelf.app.data.local.dao.SeriesDao
import com.bdshelf.app.data.local.dao.SeriesWithCounts
import com.bdshelf.app.data.local.entities.Album
import com.bdshelf.app.data.local.entities.ReadStatus
import com.bdshelf.app.data.local.entities.Series
import com.bdshelf.app.domain.CollectionStats
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine

/** Source de vérité unique pour la collection (Moitié A, §3 SPEC). */
class CollectionRepository(
    private val seriesDao: SeriesDao,
    private val albumDao: AlbumDao,
) {
    fun allSeries(): Flow<List<Series>> = seriesDao.allSeries()

    fun allSeriesWithCounts(): Flow<List<SeriesWithCounts>> = seriesDao.allSeriesWithCounts()

    suspend fun seriesById(id: String): Series? = seriesDao.byId(id)

    suspend fun upsertSeries(series: Series) = seriesDao.insert(series)

    suspend fun updateSeries(series: Series) = seriesDao.update(series)

    suspend fun deleteSeries(series: Series) = seriesDao.delete(series)

    fun albumsForSeries(seriesId: String): Flow<List<Album>> = albumDao.forSeries(seriesId)

    suspend fun albumById(id: String): Album? = albumDao.byId(id)

    suspend fun albumByEan(ean: String): Album? = albumDao.byEan(ean)

    suspend fun albumBySeriesAndTome(seriesId: String, tomeNumber: Int): Album? =
        albumDao.bySeriesAndTome(seriesId, tomeNumber)

    suspend fun upsertAlbum(album: Album) = albumDao.insert(album)

    suspend fun updateAlbum(album: Album) = albumDao.update(album)

    suspend fun deleteAlbum(album: Album) = albumDao.delete(album)

    /** Bascule possédé/manquant. Si l'album devient possédé, sa date d'ajout est mise à jour. */
    suspend fun setOwned(album: Album, owned: Boolean) {
        val updated = if (owned) album.copy(owned = true, dateAdded = System.currentTimeMillis()) else album.copy(owned = false)
        albumDao.update(updated)
    }

    /** Apprentissage d'un code-barres : associe l'EAN scanné à un album existant. */
    suspend fun linkEan(album: Album, ean: String) {
        albumDao.update(album.copy(ean = ean))
    }

    fun collectionStats(): Flow<CollectionStats> = combine(
        seriesDao.allSeries(),
        albumDao.totalOwnedCount(),
        albumDao.totalMissingCount(),
    ) { series, owned, missing -> CollectionStats(series.size, owned, missing) }

    suspend fun exportSnapshot(): CollectionSnapshot =
        CollectionSnapshot(series = seriesDao.allSeriesList(), albums = albumDao.allAlbumsList())

    /** Remplace intégralement la collection (import depuis un fichier de sauvegarde). */
    suspend fun importSnapshot(snapshot: CollectionSnapshot) {
        albumDao.deleteAll()
        seriesDao.deleteAll()
        seriesDao.insertAll(snapshot.series)
        albumDao.insertAll(snapshot.albums)
    }

    /** Crée un nouvel album manuellement (§6.8). Retourne `null` si le numéro existe déjà. */
    suspend fun addAlbum(
        seriesId: String,
        tomeNumber: Int?,
        title: String?,
        owned: Boolean,
        readStatus: ReadStatus,
        edition: String?,
        ean: String? = null,
    ): Album? {
        if (tomeNumber != null && albumDao.bySeriesAndTome(seriesId, tomeNumber) != null) {
            return null
        }
        val id = if (tomeNumber != null) {
            "$seriesId-$tomeNumber"
        } else {
            "$seriesId-hs-${System.currentTimeMillis()}"
        }
        val album = Album(
            id = id,
            seriesId = seriesId,
            tomeNumber = tomeNumber,
            title = title,
            owned = owned,
            readStatus = readStatus,
            edition = edition,
            ean = ean,
            dateAdded = System.currentTimeMillis(),
        )
        albumDao.insert(album)
        return album
    }

    /** Vérifie qu'un numéro de tome n'est pas déjà utilisé par un autre album de la série. */
    suspend fun isDuplicateTome(seriesId: String, tomeNumber: Int?, excludingAlbumId: String?): Boolean {
        if (tomeNumber == null) return false
        val existing = albumDao.bySeriesAndTome(seriesId, tomeNumber) ?: return false
        return existing.id != excludingAlbumId
    }

    /** Identifiants des séries suivies, pour le croisement avec les sorties (§4.3). */
    suspend fun trackedSeriesIds(): Set<String> =
        seriesDao.allSeriesList().filter { it.isTracked }.map { it.id }.toSet()

    /** Couples (série, tome) déjà possédés, pour le croisement avec les sorties (§4.3). */
    suspend fun ownedTomeRefs(): Set<Pair<String, Int>> =
        albumDao.ownedTomeRefs().map { it.seriesId to it.tomeNumber }.toSet()
}
