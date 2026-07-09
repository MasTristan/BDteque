package com.bdshelf.app.domain

import com.bdshelf.app.data.repo.CollectionSnapshot
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class SnapshotValidationTest {

    private fun snapshot(
        version: Int = SUPPORTED_SNAPSHOT_VERSION,
        seriesIds: List<String> = listOf("s"),
        albums: List<Pair<String, String>> = listOf("s-1" to "s"),
    ) = CollectionSnapshot(
        version = version,
        series = seriesIds.map { series(it) },
        albums = albums.map { (albumId, seriesId) ->
            album(seriesId = seriesId, tomeNumber = 1, owned = true, id = albumId)
        },
    )

    @Test
    fun `valid snapshot passes`() {
        assertEquals(SnapshotValidation.Valid, snapshot().validate())
    }

    @Test
    fun `unsupported version is rejected`() {
        assertTrue(snapshot(version = 99).validate() is SnapshotValidation.Invalid)
    }

    @Test
    fun `duplicate series id is rejected`() {
        val result = snapshot(seriesIds = listOf("s", "s")).validate()
        assertTrue(result is SnapshotValidation.Invalid)
    }

    @Test
    fun `duplicate album id is rejected`() {
        val result = snapshot(albums = listOf("dup" to "s", "dup" to "s")).validate()
        assertTrue(result is SnapshotValidation.Invalid)
    }

    @Test
    fun `orphan album referencing missing series is rejected`() {
        val result = snapshot(albums = listOf("x-1" to "missing")).validate()
        assertTrue(result is SnapshotValidation.Invalid)
    }

    @Test
    fun `blank series id is rejected`() {
        val result = snapshot(seriesIds = listOf("")).validate()
        assertTrue(result is SnapshotValidation.Invalid)
    }
}
