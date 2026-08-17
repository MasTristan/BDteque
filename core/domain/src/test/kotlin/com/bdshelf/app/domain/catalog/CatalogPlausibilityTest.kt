package com.bdshelf.app.domain.catalog

import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class CatalogPlausibilityTest {

    @Test
    fun `accepts counts matching the manifest exactly`() {
        val result = CatalogPlausibility.check(CatalogCounts(series = 4120, albums = 34887), 4120, 34887)
        assertTrue(result.plausible)
    }

    @Test
    fun `accepts counts within the 5 percent tolerance`() {
        // 4120 * 1.05 = 4326, tout juste dans la tolérance.
        val result = CatalogPlausibility.check(CatalogCounts(series = 4120, albums = 34887), 4326, 34887)
        assertTrue(result.plausible)
    }

    @Test
    fun `rejects counts outside the 5 percent tolerance`() {
        val result = CatalogPlausibility.check(CatalogCounts(series = 4120, albums = 34887), 5000, 34887)
        assertFalse(result.plausible)
        assertNotNull(result.reason)
    }

    @Test
    fun `rejects an empty series table even if the manifest agrees`() {
        val result = CatalogPlausibility.check(CatalogCounts(series = 0, albums = 0), 0, 0)
        assertFalse(result.plausible)
    }

    @Test
    fun `rejects fewer than 1000 series even within manifest tolerance`() {
        val result = CatalogPlausibility.check(CatalogCounts(series = 500, albums = 5000), 500, 5000)
        assertFalse(result.plausible)
    }

    @Test
    fun `rejects zero albums with nonzero series`() {
        val result = CatalogPlausibility.check(CatalogCounts(series = 4120, albums = 0), 4120, 0)
        assertFalse(result.plausible)
    }

    @Test
    fun `rejects a plausibly truncated body under-counted beyond tolerance`() {
        // Corps tronqué : moitié des albums manquent, mais toujours "vraisemblable"
        // au premier coup d'oeil (des séries, des albums) — exactement le cas
        // que la tolérance de comptage doit attraper.
        val result = CatalogPlausibility.check(CatalogCounts(series = 4120, albums = 34887), 4120, 17000)
        assertFalse(result.plausible)
    }
}
