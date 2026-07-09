package com.bdshelf.app.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.bdshelf.app.data.local.dao.AlbumDao
import com.bdshelf.app.data.local.dao.IsbnLookupCacheDao
import com.bdshelf.app.data.local.dao.SeriesDao
import com.bdshelf.app.data.local.entities.Album
import com.bdshelf.app.data.local.entities.CachedIsbnLookup
import com.bdshelf.app.data.local.entities.Series

@Database(
    entities = [Series::class, Album::class, CachedIsbnLookup::class],
    version = 2,
    exportSchema = true,
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun seriesDao(): SeriesDao
    abstract fun albumDao(): AlbumDao
    abstract fun isbnLookupCacheDao(): IsbnLookupCacheDao

    companion object {
        const val DATABASE_NAME = "bdshelf.db"

        /** v2 : cache des identifications ISBN (§6.4). Table neuve, aucune donnée à migrer. */
        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS `isbn_lookup_cache` (" +
                        "`isbn` TEXT NOT NULL, " +
                        "`title` TEXT NOT NULL, " +
                        "`seriesName` TEXT, " +
                        "`tomeNumber` INTEGER, " +
                        "`authors` TEXT NOT NULL, " +
                        "`source` TEXT NOT NULL, " +
                        "`fetchedAt` INTEGER NOT NULL, " +
                        "PRIMARY KEY(`isbn`))",
                )
            }
        }
    }
}
