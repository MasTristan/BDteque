package com.bdshelf.app.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.bdshelf.app.data.local.entities.CachedIsbnLookup

@Dao
interface IsbnLookupCacheDao {

    @Query("SELECT * FROM isbn_lookup_cache WHERE isbn = :isbn")
    suspend fun byIsbn(isbn: String): CachedIsbnLookup?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(entry: CachedIsbnLookup)

    @Query("DELETE FROM isbn_lookup_cache")
    suspend fun deleteAll()
}
