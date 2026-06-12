package com.bdshelf.app.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.bdshelf.app.data.local.dao.AlbumDao
import com.bdshelf.app.data.local.dao.SeriesDao
import com.bdshelf.app.data.local.entities.Album
import com.bdshelf.app.data.local.entities.Series

@Database(
    entities = [Series::class, Album::class],
    version = 1,
    exportSchema = true,
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun seriesDao(): SeriesDao
    abstract fun albumDao(): AlbumDao

    companion object {
        const val DATABASE_NAME = "bdshelf.db"
    }
}
