package com.bdshelf.app.domain

import org.junit.Assert.assertEquals
import org.junit.Test

class GapDetectorTest {

    private fun tome(number: Int?, owned: Boolean) = TomeOwnership(number, owned)

    @Test
    fun `no gaps when all tomes owned`() {
        val tomes = listOf(tome(1, owned = true), tome(2, owned = true), tome(3, owned = true))
        assertEquals(emptyList<Int>(), GapDetector.gaps(tomes))
    }

    @Test
    fun `internal missing tomes are gaps`() {
        val tomes = listOf(tome(1, owned = true), tome(2, owned = false), tome(4, owned = true))
        // Trous : 2 (non possédé) et 3 (absent), bornés par le plus grand tome connu (4).
        assertEquals(listOf(2, 3), GapDetector.gaps(tomes))
    }

    @Test
    fun `empty tomes yield no gaps`() {
        assertEquals(emptyList<Int>(), GapDetector.gaps(emptyList()))
    }

    @Test
    fun `unnumbered tomes are ignored`() {
        val tomes = listOf(tome(1, owned = true), tome(null, owned = true))
        assertEquals(emptyList<Int>(), GapDetector.gaps(tomes))
    }

    @Test
    fun `next tome number is max plus one`() {
        val tomes = listOf(tome(1, owned = true), tome(5, owned = false))
        assertEquals(6, GapDetector.nextTomeNumber(tomes))
    }

    @Test
    fun `next tome number is one when empty`() {
        assertEquals(1, GapDetector.nextTomeNumber(emptyList()))
    }

    @Test
    fun `gapReport without catalog matches gaps exactly`() {
        val tomes = listOf(tome(1, owned = true), tome(2, owned = false), tome(4, owned = true))
        val report = GapDetector.gapReport(tomes)
        assertEquals(GapDetector.gaps(tomes), report.internal)
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
        val tomes = listOf(tome(1, owned = true), tome(2, owned = false), tome(4, owned = true))
        // Connu localement : 1 à 4 (trou interne : 2, 3). Le catalogue en
        // connaît 7 : 5, 6, 7 sont un retard, pas un trou interne.
        val report = GapDetector.gapReport(tomes, catalogTomeCount = 7)
        assertEquals(listOf(2, 3), report.internal)
        assertEquals(listOf(5, 6, 7), report.ahead)
    }

    @Test
    fun `gapReport ahead is empty when the catalog knows nothing new`() {
        val tomes = listOf(tome(1, owned = true), tome(2, owned = true))
        val report = GapDetector.gapReport(tomes, catalogTomeCount = 2)
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
