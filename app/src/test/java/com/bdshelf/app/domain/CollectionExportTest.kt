package com.bdshelf.app.domain

import com.bdshelf.app.data.local.entities.ReadStatus
import com.bdshelf.app.data.local.entities.SeriesStatus
import com.bdshelf.app.data.repo.CollectionSnapshot
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class CollectionExportTest {

    @Test
    fun `csv starts with header row`() {
        val csv = CollectionSnapshot(series = emptyList(), albums = emptyList()).toCsv()
        assertTrue(csv.startsWith("Série,Statut,Suivi,Tome,Titre,Possédé,Statut de lecture,Édition,Code-barres"))
    }

    @Test
    fun `csv renders one row per album sorted by series then tome`() {
        val snapshot = CollectionSnapshot(
            series = listOf(
                series("b-series", title = "B", status = SeriesStatus.FINISHED, isTracked = false),
                series("a-series", title = "A"),
            ),
            albums = listOf(
                album("b-series", 1, owned = true),
                album("a-series", 2, owned = false, readStatus = ReadStatus.READ),
                album("a-series", 1, owned = true, readStatus = ReadStatus.LENT),
            ),
        )
        val lines = snapshot.toCsv().trim().lines()
        // 1 en-tête + 3 albums.
        assertEquals(4, lines.size)
        // Tri : série A (tomes 1 puis 2), puis série B.
        assertTrue(lines[1].startsWith("A,En cours,oui,1,"))
        assertTrue(lines[2].startsWith("A,En cours,oui,2,"))
        assertTrue(lines[3].startsWith("B,Terminée,non,1,"))
    }

    @Test
    fun `csv escapes values containing commas and quotes`() {
        val snapshot = CollectionSnapshot(
            series = listOf(series("s", title = "Nom, avec virgule")),
            albums = listOf(album("s", 1, owned = true, title = "Titre \"cité\"")),
        )
        val row = snapshot.toCsv().trim().lines()[1]
        assertTrue(row.contains("\"Nom, avec virgule\""))
        assertTrue(row.contains("\"Titre \"\"cité\"\"\""))
    }

    @Test
    fun `unnumbered album is labelled hors-serie`() {
        val snapshot = CollectionSnapshot(
            series = listOf(series("s", title = "S")),
            albums = listOf(album("s", null, owned = true)),
        )
        assertTrue(snapshot.toCsv().contains("hors-série"))
    }
}
