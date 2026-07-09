package com.bdshelf.app

import android.app.Application
import androidx.room.Room
import com.bdshelf.app.data.local.AppDatabase
import com.bdshelf.app.data.prefs.UserPreferencesRepository
import com.bdshelf.app.data.remote.IsbnLookupService
import com.bdshelf.app.data.remote.ReleasesApi
import com.bdshelf.app.data.repo.CollectionRepository
import com.bdshelf.app.data.repo.ReleasesRepository
import com.bdshelf.app.data.seed.SeedImporter
import com.bdshelf.app.work.ReleasesSyncWorker

/**
 * Point d'injection manuelle (pas de Hilt, §STACK).
 *
 * Toutes les dépendances partagées (base de données, repositories) sont
 * construites ici une seule fois et exposées aux ViewModels via
 * `(application as BdShelfApplication)`.
 */
class BdShelfApplication : Application() {

    lateinit var database: AppDatabase
        private set

    lateinit var collectionRepository: CollectionRepository
        private set

    lateinit var releasesRepository: ReleasesRepository
        private set

    lateinit var userPreferencesRepository: UserPreferencesRepository
        private set

    lateinit var seedImporter: SeedImporter
        private set

    lateinit var isbnLookupService: IsbnLookupService
        private set

    override fun onCreate() {
        super.onCreate()

        database = Room.databaseBuilder(this, AppDatabase::class.java, AppDatabase.DATABASE_NAME)
            .addMigrations(AppDatabase.MIGRATION_1_2)
            .build()
        collectionRepository = CollectionRepository(database)
        releasesRepository = ReleasesRepository(this, ReleasesApi())
        userPreferencesRepository = UserPreferencesRepository(this)
        seedImporter = SeedImporter(this, database.seriesDao(), database.albumDao())
        isbnLookupService = IsbnLookupService(cache = database.isbnLookupCacheDao())

        ReleasesSyncWorker.schedulePeriodic(this)
    }
}
