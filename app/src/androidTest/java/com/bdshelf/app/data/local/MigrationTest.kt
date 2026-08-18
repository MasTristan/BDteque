package com.bdshelf.app.data.local

import android.content.Context
import androidx.sqlite.db.SupportSQLiteDatabase
import androidx.sqlite.db.SupportSQLiteOpenHelper
import androidx.sqlite.db.framework.FrameworkSQLiteOpenHelperFactory
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Vérifie que [AppDatabase.MIGRATION_1_2] transforme une base « version 1 »
 * réelle en base « version 2 » sans perdre une ligne (§E6, porte de qualité G5,
 * dette D3 : cette migration est en production depuis `c932ef1` sans avoir
 * jamais été testée contre son schéma de référence).
 *
 * Approche volontairement différente de [androidx.room.testing.MigrationTestHelper] :
 * `app/schemas/1.json` n'existe pas — il n'a jamais été exporté avant ce
 * commit, et il est impossible de le reconstruire fidèlement sans recompiler
 * le code tel qu'il était à `c932ef1^` (identityHash calculé par le
 * compilateur Room). Fabriquer ce fichier à la main produirait un test qui a
 * l'air de vérifier la migration sans réellement la vérifier — pire que pas
 * de test du tout.
 *
 * On construit donc la base « version 1 » avec le SQL réel qu'aurait généré
 * Room pour les entités `Series`/`Album` (inchangées entre v1 et v2, seule
 * `isbn_lookup_cache` est neuve — voir le commentaire de MIGRATION_1_2), on y
 * insère des données, puis on appelle la VRAIE migration de production.
 * Aucune donnée fabriquée sur le chemin testé : seul le point de départ est
 * reconstitué à la main, la migration elle-même est celle qui tourne pour de
 * vrai sur le téléphone d'un utilisateur.
 *
 * Pour les migrations futures (2→3 et suivantes), `app/schemas/2.json` sera
 * généré automatiquement par la CI dès la première compilation réussie
 * (`room { schemaDirectory(...) }` déjà configuré) : elles pourront utiliser
 * [androidx.room.testing.MigrationTestHelper], la voie normale, désormais
 * outillée (voir `androidTest.assets.srcDirs` dans `app/build.gradle.kts`).
 */
@RunWith(AndroidJUnit4::class)
class MigrationTest {

    private val context = ApplicationProvider.getApplicationContext<Context>()
    private val dbName = "migration-test-1-2.db"
    private lateinit var helper: SupportSQLiteOpenHelper

    @Before
    fun setUp() {
        context.deleteDatabase(dbName)
    }

    @After
    fun tearDown() {
        if (::helper.isInitialized) helper.close()
        context.deleteDatabase(dbName)
    }

    @Test
    fun migrate1To2_preservesExistingSeriesAndAlbums() {
        helper = openVersion1Database()
        val db = helper.writableDatabase

        db.execSQL(
            "INSERT INTO series (id, title, status, isTracked, color, knownTomeCount, notes) " +
                "VALUES ('thorgal', 'Thorgal', 'ONGOING', 1, 4287269261, NULL, NULL)",
        )
        db.execSQL(
            "INSERT INTO albums (id, seriesId, tomeNumber, title, owned, readStatus, edition, ean, dateAdded) " +
                "VALUES ('thorgal-1', 'thorgal', 1, 'La Magicienne trahie', 1, 'READ', NULL, '9782803611119', 1000)",
        )
        db.execSQL(
            "INSERT INTO albums (id, seriesId, tomeNumber, title, owned, readStatus, edition, ean, dateAdded) " +
                "VALUES ('thorgal-2', 'thorgal', 2, NULL, 0, 'UNREAD', NULL, NULL, 2000)",
        )

        AppDatabase.MIGRATION_1_2.migrate(db)

        // Les données pré-existantes n'ont pas bougé : c'est tout l'enjeu de
        // la migration, celui qui casse une collection réelle si on se trompe.
        db.query("SELECT id, title FROM series").use { cursor ->
            assertEquals(1, cursor.count)
            assertTrue(cursor.moveToFirst())
            assertEquals("thorgal", cursor.getString(0))
            assertEquals("Thorgal", cursor.getString(1))
        }
        db.query("SELECT id, owned, ean FROM albums ORDER BY id").use { cursor ->
            assertEquals(2, cursor.count)
            assertTrue(cursor.moveToFirst())
            assertEquals("thorgal-1", cursor.getString(0))
            assertEquals(1, cursor.getInt(1))
            assertEquals("9782803611119", cursor.getString(2))
            assertTrue(cursor.moveToNext())
            assertEquals("thorgal-2", cursor.getString(0))
            assertEquals(0, cursor.getInt(1))
            assertTrue(cursor.isNull(2))
        }

        // La table neuve existe, avec exactement les colonnes de MIGRATION_1_2.
        db.query("PRAGMA table_info(isbn_lookup_cache)").use { cursor ->
            val columns = mutableSetOf<String>()
            while (cursor.moveToNext()) {
                columns.add(cursor.getString(cursor.getColumnIndexOrThrow("name")))
            }
            assertEquals(
                setOf("isbn", "title", "seriesName", "tomeNumber", "authors", "source", "fetchedAt"),
                columns,
            )
        }
        db.query("SELECT COUNT(*) FROM isbn_lookup_cache").use { cursor ->
            cursor.moveToFirst()
            assertEquals(0, cursor.getInt(0))
        }
    }

    @Test
    fun migrate1To2_isIdempotentAcrossReruns() {
        // WorkManager, un import, et une session de test peuvent en théorie
        // rejouer une migration sur une base déjà à jour : `CREATE TABLE IF
        // NOT EXISTS` doit rester sans effet la seconde fois, jamais une
        // erreur qui ferait échouer l'ouverture de la base au démarrage.
        helper = openVersion1Database()
        val db = helper.writableDatabase
        AppDatabase.MIGRATION_1_2.migrate(db)
        AppDatabase.MIGRATION_1_2.migrate(db)

        db.query("SELECT COUNT(*) FROM isbn_lookup_cache").use { cursor ->
            cursor.moveToFirst()
            assertEquals(0, cursor.getInt(0))
        }
    }

    /**
     * Ouvre une base neuve avec exactement le schéma qu'a produit Room pour
     * `Series`/`Album` en version 1 — les deux tables sont restées
     * structurellement identiques jusqu'à aujourd'hui (seule
     * `isbn_lookup_cache` est apparue en v2), donc ce SQL décrit à la fois
     * l'état v1 réel et l'état actuel de ces deux tables.
     */
    private fun openVersion1Database(): SupportSQLiteOpenHelper {
        val configuration = SupportSQLiteOpenHelper.Configuration.builder(context)
            .name(dbName)
            .callback(object : SupportSQLiteOpenHelper.Callback(1) {
                override fun onCreate(db: SupportSQLiteDatabase) {
                    db.execSQL(
                        "CREATE TABLE IF NOT EXISTS `series` (`id` TEXT NOT NULL, `title` TEXT NOT NULL, " +
                            "`status` TEXT NOT NULL, `isTracked` INTEGER NOT NULL, `color` INTEGER NOT NULL, " +
                            "`knownTomeCount` INTEGER, `notes` TEXT, PRIMARY KEY(`id`))",
                    )
                    db.execSQL(
                        "CREATE TABLE IF NOT EXISTS `albums` (`id` TEXT NOT NULL, `seriesId` TEXT NOT NULL, " +
                            "`tomeNumber` INTEGER, `title` TEXT, `owned` INTEGER NOT NULL, " +
                            "`readStatus` TEXT NOT NULL, `edition` TEXT, `ean` TEXT, `dateAdded` INTEGER NOT NULL, " +
                            "PRIMARY KEY(`id`), FOREIGN KEY(`seriesId`) REFERENCES `series`(`id`) " +
                            "ON UPDATE NO ACTION ON DELETE CASCADE )",
                    )
                    db.execSQL("CREATE INDEX IF NOT EXISTS `index_albums_seriesId` ON `albums` (`seriesId`)")
                    db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS `index_albums_ean` ON `albums` (`ean`)")
                }

                override fun onUpgrade(db: SupportSQLiteDatabase, oldVersion: Int, newVersion: Int) = Unit
            })
            .build()
        return FrameworkSQLiteOpenHelperFactory().create(configuration)
    }
}
