package com.bdshelf.app.domain

import com.bdshelf.app.data.remote.ReleaseItem
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class CrossReferenceReleasesTest {

    private fun release(seriesId: String, tome: Int, status: String = "UPCOMING") = ReleaseItem(
        seriesId = seriesId,
        seriesTitle = seriesId,
        tomeNumber = tome,
        title = "T$tome",
        expectedDate = "2026-09-18",
        status = status,
    )

    @Test
    fun `only tracked series are kept`() {
        val releases = listOf(release("a", 1), release("b", 1))
        val result = crossReferenceReleases(releases, trackedSeriesIds = setOf("a"), ownedTomes = emptySet())
        assertEquals(1, result.size)
        assertEquals("a", result.first().release.seriesId)
    }

    @Test
    fun `owned flag reflects owned tomes`() {
        val releases = listOf(release("a", 1), release("a", 2))
        val result = crossReferenceReleases(
            releases,
            trackedSeriesIds = setOf("a"),
            ownedTomes = setOf("a" to 1),
        )
        assertTrue(result.first { it.release.tomeNumber == 1 }.owned)
        assertFalse(result.first { it.release.tomeNumber == 2 }.owned)
    }

    @Test
    fun `missing count ignores owned releases`() {
        val crossed = crossReferenceReleases(
            listOf(release("a", 1), release("a", 2), release("a", 3)),
            trackedSeriesIds = setOf("a"),
            ownedTomes = setOf("a" to 1),
        )
        assertEquals(2, crossed.missingCount())
    }

    @Test
    fun `empty tracked set yields nothing`() {
        val result = crossReferenceReleases(listOf(release("a", 1)), emptySet(), emptySet())
        assertTrue(result.isEmpty())
    }
}
