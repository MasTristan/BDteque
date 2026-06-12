package com.bdshelf.app.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.bdshelf.app.data.local.entities.Album
import kotlinx.coroutines.flow.Flow

@Dao
interface AlbumDao {

    @Query("SELECT * FROM albums WHERE seriesId = :sid ORDER BY tomeNumber ASC")
    fun forSeries(sid: String): Flow<List<Album>>

    @Query("SELECT * FROM albums WHERE ean = :ean LIMIT 1")
    suspend fun byEan(ean: String): Album?

    @Query("SELECT COUNT(*) FROM albums WHERE seriesId = :sid AND owned = 1")
    suspend fun ownedCount(sid: String): Int

    @Query("SELECT COUNT(*) FROM albums WHERE seriesId = :sid AND owned = 0")
    suspend fun missingCount(sid: String): Int

    @Query("SELECT * FROM albums WHERE seriesId = :sid AND tomeNumber = :n LIMIT 1")
    suspend fun bySeriesAndTome(sid: String, n: Int): Album?

    @Query("SELECT * FROM albums WHERE id = :id")
    suspend fun byId(id: String): Album?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(album: Album)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(albums: List<Album>)

    @Update
    suspend fun update(album: Album)

    @Delete
    suspend fun delete(album: Album)

    @Query("SELECT COUNT(*) FROM albums WHERE owned = 1")
    fun totalOwnedCount(): Flow<Int>

    @Query("SELECT COUNT(*) FROM albums WHERE owned = 0")
    fun totalMissingCount(): Flow<Int>

    @Query("SELECT * FROM albums")
    suspend fun allAlbumsList(): List<Album>

    @Query("SELECT seriesId, tomeNumber FROM albums WHERE owned = 1 AND tomeNumber IS NOT NULL")
    suspend fun ownedTomeRefs(): List<OwnedTomeRef>

    @Query("DELETE FROM albums")
    suspend fun deleteAll()
}

/** Référence légère (série, tome) pour le croisement avec les sorties (§4.3). */
data class OwnedTomeRef(val seriesId: String, val tomeNumber: Int)
