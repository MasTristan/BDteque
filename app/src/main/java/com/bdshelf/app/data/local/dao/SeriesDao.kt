package com.bdshelf.app.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.bdshelf.app.data.local.entities.Series
import kotlinx.coroutines.flow.Flow

@Dao
interface SeriesDao {

    @Query("SELECT * FROM series ORDER BY title ASC")
    fun allSeries(): Flow<List<Series>>

    @Query("SELECT * FROM series WHERE title LIKE '%' || :q || '%'")
    fun search(q: String): Flow<List<Series>>

    @Query("SELECT * FROM series WHERE id = :id")
    suspend fun byId(id: String): Series?

    /** Séries enrichies des compteurs possédés/total, pour la liste (§6.5). */
    @Query(
        """
        SELECT s.id, s.title, s.status, s.isTracked, s.color, s.knownTomeCount, s.notes,
               COALESCE(SUM(CASE WHEN a.owned = 1 THEN 1 ELSE 0 END), 0) AS ownedCount,
               COALESCE(COUNT(a.id), 0) AS totalCount
        FROM series s
        LEFT JOIN albums a ON a.seriesId = s.id
        GROUP BY s.id
        ORDER BY s.title ASC
        """,
    )
    fun allSeriesWithCounts(): Flow<List<SeriesWithCounts>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(series: Series)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(series: List<Series>)

    @Update
    suspend fun update(series: Series)

    @Delete
    suspend fun delete(series: Series)

    @Query("SELECT COUNT(*) FROM series")
    suspend fun seriesCount(): Int

    @Query("SELECT * FROM series")
    suspend fun allSeriesList(): List<Series>

    @Query("DELETE FROM series")
    suspend fun deleteAll()
}
