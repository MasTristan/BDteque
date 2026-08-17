package com.bdshelf.app.domain

import org.junit.Assert.assertEquals
import org.junit.Test

class GapDetectorTest {

    @Test
    fun `no gaps when all tomes owned`() {
        val albums = listOf(
            album("s", 1, owned = true),
            album("s", 2, owned = true),
            album("s", 3, owned = true),
        )
        assertEquals(emptyList<Int>(), GapDetector.gaps(albums))
    }

    @Test
    fun `internal missing tomes are gaps`() {
        val albums = listOf(
            album("s", 1, owned = true),
            album("s", 2, owned = false),
            album("s", 4, owned = true),
        )
        // Trous : 2 (non possédé) et 3 (absent), bornés par le plus grand tome connu (4).
        assertEquals(listOf(2, 3), GapDetector.gaps(albums))
    }

    @Test
    fun `empty albums yield no gaps`() {
        assertEquals(emptyList<Int>(), GapDetector.gaps(emptyList()))
    }

    @Test
    fun `unnumbered albums are ignored`() {
        val albums = listOf(
            album("s", 1, owned = true),
            album("s", null, owned = true),
        )
        assertEquals(emptyList<Int>(), GapDetector.gaps(albums))
    }

    @Test
    fun `next tome number is max plus one`() {
        val albums = listOf(
            album("s", 1, owned = true),
            album("s", 5, owned = false),
        )
        assertEquals(6, GapDetector.nextTomeNumber(albums))
    }

    @Test
    fun `next tome number is one when empty`() {
        assertEquals(1, GapDetector.nextTomeNumber(emptyList()))
    }

    @Test
    fun `gapReport without catalog matches gaps exactly`() {
        val albums = listOf(
            album("s", 1, owned = true),
            album("s", 2, owned = false),
            album("s", 4, owned = true),
        )
        val report = GapDetector.gapReport(albums)
        assertEquals(GapDetector.gaps(albums), report.internal)
        assertEquals(emptyList<Int>(), report.ahead)
    }

    @Test
    fun `gapReport without catalog matches gaps exactly for an empty collection`() {
        val report = GapDetector.gapReport(emptyList())
        assertEquals(emptyList<Int>(), report.internal)
        assertEquals(emptyList<Int>(), report.ahead)
    }

    @Test
    fun `gapReport separates internal gaps from tomes ahead of the known maximum`() {
        val albums = listOf(
            album("s", 1, owned = true),
            album("s", 2, owned = false),
            album("s", 4, owned = true),
        )
        // Connu localement : 1 à 4 (trou interne : 2, 3). Le catalogue en
        // connaît 7 : 5, 6, 7 sont un retard, pas un trou interne.
        val report = GapDetector.gapReport(albums, catalogTomeCount = 7)
        assertEquals(listOf(2, 3), report.internal)
        assertEquals(listOf(5, 6, 7), report.ahead)
    }

    @Test
    fun `gapReport ahead is empty when the catalog knows nothing new`() {
        val albums = listOf(album("s", 1, owned = true), album("s", 2, owned = true))
        val report = GapDetector.gapReport(albums, catalogTomeCount = 2)
        assertEquals(emptyList<Int>(), report.internal)
        assertEquals(emptyList<Int>(), report.ahead)
    }

    @Test
    fun `gapReport ahead covers every catalog tome when nothing is known locally yet`() {
        val report = GapDetector.gapReport(emptyList(), catalogTomeCount = 3)
        assertEquals(emptyList<Int>(), report.internal)
        assertEquals(listOf(1, 2, 3), report.ahead)
    }
}
