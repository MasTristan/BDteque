package com.bdshelf.app.data.repo

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.bdshelf.app.data.local.AppDatabase
import com.bdshelf.app.data.local.entities.ReadStatus
import com.bdshelf.app.data.local.entities.Series
import com.bdshelf.app.data.local.entities.SeriesStatus
import com.bdshelf.app.data.seed.SeedImporter
import com.bdshelf.app.domain.tomeGaps
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Parcours critiques bout en bout, sur une vraie base Room en mémoire (§E6,
 * porte de qualité G2). Ceux-ci ne dépendent pas de la caméra ni de
 * l'interface Compose ; ils vérifient la couche qui, si elle casse, casse
 * silencieusement une vraie collection :
 *
 * - P1 : premier lancement → import du seed → collection consultable.
 * - P2 : scan d'un EAN → verdict (possédé/manquant/inconnu) → collection à jour.
 * - P3 : basculer un tome possédé/manquant met les trous à jour.
 * - P4 : export → base neuve → import → collection identique.
 */
@RunWith(AndroidJUnit4::class)
class CollectionRepositoryTest {

    private lateinit var database: AppDatabase
    private lateinit var repository: CollectionRepository

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        database = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java).build()
        repository = CollectionRepository(database)
    }

    @After
    fun tearDown() {
        database.close()
    }

    /** P1 — premier lancement : la collection livrée à l'utilisateur d'origine reste consultable. */
    @Test
    fun seedImport_populatesFreshCollection_fromRealAssetFile() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val importer = SeedImporter(context, database.seriesDao(), database.albumDao())

        assertEquals(0, repository.allSeriesWithCounts().first().size)

        importer.import()

        val series = repository.allSeriesWithCounts().first()
        assertTrue("le seed embarqué doit peupler au moins une série", series.isNotEmpty())
        val albums = repository.allAlbums().first()
        assertTrue("le seed embarqué doit peupler au moins un album", albums.isNotEmpty())

        // Idempotence : relancer l'import sur une collection déjà peuplée ne double rien
        // (garde-fou contre une restauration de sauvegarde dont le drapeau seed_imported
        // aurait été réinitialisé — voir le commentaire de SeedImporter).
        importer.import()
        assertEquals(series.size, repository.allSeriesWithCounts().first().size)
        assertEquals(albums.size, repository.allAlbums().first().size)
    }

    /**
     * P2 — la boucle centrale : scanner un EAN déjà possédé le confirme sans
     * rien changer à la collection. C'est le verdict le plus fréquent — le
     * rassurer ne doit jamais, par erreur, réinitialiser une date d'ajout ou
     * un statut de lecture.
     */
    @Test
    fun scanningKnownOwnedEan_yieldsOwnedVerdict_withoutAlteringTheAlbum() = runBlocking {
        val series = Series(
            id = "xiii", title = "XIII", status = SeriesStatus.ONGOING,
            isTracked = true, color = 0L, knownTomeCount = null, notes = null,
        )
        repository.upsertSeries(series)
        val created = repository.addAlbum(
            seriesId = "xiii", tomeNumber = 1, title = "Le Jour du soleil noir",
            owned = true, readStatus = ReadStatus.READ, edition = null, ean = "9782505001325",
        )!!

        val scanned = repository.albumByEan("9782505001325")
        assertEquals(created, scanned)
        assertTrue("un EAN déjà possédé donne un verdict Possédé", scanned!!.owned)
    }

    /**
     * P2 — l'autre verdict connu : un tome manquant scanné en librairie doit
     * pouvoir être marqué possédé sur-le-champ (« Je viens de l'acheter »),
     * exactement le geste qui déclenche l'achat impulsif que l'app sert.
     */
    @Test
    fun scanningKnownMissingEan_thenMarkingOwned_updatesTheCollection() = runBlocking {
        val series = Series(
            id = "xiii", title = "XIII", status = SeriesStatus.ONGOING,
            isTracked = true, color = 0L, knownTomeCount = null, notes = null,
        )
        repository.upsertSeries(series)
        repository.addAlbum(
            seriesId = "xiii", tomeNumber = 2, title = null,
            owned = false, readStatus = ReadStatus.UNREAD, edition = null, ean = "9782505001332",
        )

        val scanned = repository.albumByEan("9782505001332")
        assertFalse("un EAN manquant donne un verdict Manquant", scanned!!.owned)

        repository.setOwned(scanned, true)

        val updated = repository.albumByEan("9782505001332")
        assertTrue("marquer possédé depuis le verdict doit persister", updated!!.owned)
    }

    /**
     * P2 — le verdict Inconnu : un EAN absent de toute la collection doit
     * pouvoir devenir un nouvel album, immédiatement retrouvable — sinon le
     * scan d'une nouveauté ne sert à rien.
     */
    @Test
    fun scanningUnknownEan_addsNewAlbum_thenFindableByTheSameEan() = runBlocking {
        val series = Series(
            id = "xiii", title = "XIII", status = SeriesStatus.ONGOING,
            isTracked = true, color = 0L, knownTomeCount = null, notes = null,
        )
        repository.upsertSeries(series)

        assertEquals(null, repository.albumByEan("9782505009994"))

        val created = repository.addAlbum(
            seriesId = "xiii", tomeNumber = 24, title = "Le Dossier Jason Fly",
            owned = true, readStatus = ReadStatus.UNREAD, edition = null, ean = "9782505009994",
        )!!

        val scanned = repository.albumByEan("9782505009994")
        assertEquals(created.id, scanned!!.id)
    }

    /** P3 — l'usage librairie : voir ses trous, et les voir bouger quand on achète. */
    @Test
    fun togglingOwned_updatesGapsAndCounts() = runBlocking {
        val series = Series(
            id = "thorgal", title = "Thorgal", status = SeriesStatus.ONGOING,
            isTracked = true, color = 0L, knownTomeCount = null, notes = null,
        )
        repository.upsertSeries(series)
        for (n in 1..5) {
            repository.addAlbum(
                seriesId = "thorgal", tomeNumber = n, title = null,
                owned = n != 3, readStatus = ReadStatus.UNREAD, edition = null,
            )
        }

        var albums = repository.albumsForSeries("thorgal").first()
        assertEquals(listOf(3), albums.tomeGaps())

        val gap = repository.albumBySeriesAndTome("thorgal", 3)!!
        repository.setOwned(gap, true)

        albums = repository.albumsForSeries("thorgal").first()
        assertTrue("le tome 3 doit maintenant être possédé", albums.first { it.tomeNumber == 3 }.owned)
        assertEquals(emptyList<Int>(), albums.tomeGaps())
    }

    /** P4 — la survie des données : ce qui sort par l'export doit revenir à l'identique. */
    @Test
    fun exportThenImportSnapshot_restoresCollectionIdentically() = runBlocking {
        repository.upsertSeries(
            Series(
                id = "xiii", title = "XIII", status = SeriesStatus.FINISHED,
                isTracked = false, color = 42L, knownTomeCount = 24, notes = "collection complète",
            ),
        )
        repository.addAlbum(
            seriesId = "xiii", tomeNumber = 1, title = "Le Jour du soleil noir",
            owned = true, readStatus = ReadStatus.READ, edition = null, ean = "9782505001325",
        )
        repository.addAlbum(
            seriesId = "xiii", tomeNumber = 2, title = null,
            owned = false, readStatus = ReadStatus.UNREAD, edition = null,
        )

        val exported = repository.exportSnapshot()

        // Base neuve : c'est le scénario réel — installation neuve, import du fichier
        // de sauvegarde exporté depuis l'ancien téléphone.
        val freshDb = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            AppDatabase::class.java,
        ).build()
        try {
            val freshRepository = CollectionRepository(freshDb)
            freshRepository.importSnapshot(exported)

            val restored = freshRepository.exportSnapshot()
            assertEquals(exported.series.toSet(), restored.series.toSet())
            assertEquals(exported.albums.toSet(), restored.albums.toSet())
        } finally {
            freshDb.close()
        }
    }
}
