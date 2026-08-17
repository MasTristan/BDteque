package com.bdshelf.app.domain.catalog

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class CatalogBundleParserTest {

    @Test
    fun `parses one entity of each type`() {
        val lines = """
            {"t":"s","id":"thorgal","title":"Thorgal","publisher":"Le Lombard","status":"ONGOING","firstYear":1980,"tomeCount":41}
            {"t":"a","ean":"9782803680122","seriesId":"thorgal","tome":40,"title":"Le Feu écarlate","published":"2024-11-15","pages":48}
            {"t":"x","alias":"thorgal la jeunesse","seriesId":"thorgal-jeunesse"}
        """.trimIndent().lineSequence()

        val result = CatalogBundleParser.parse(lines)

        assertEquals(0, result.malformedLines)
        assertEquals(0, result.unknownTypeLines)
        assertEquals(3, result.entities.size)
        assertTrue(result.entities[0] is CatalogBundleEntity.SeriesEntity)
        assertTrue(result.entities[1] is CatalogBundleEntity.AlbumEntity)
        assertTrue(result.entities[2] is CatalogBundleEntity.AliasEntity)

        val series = result.entities[0] as CatalogBundleEntity.SeriesEntity
        assertEquals("thorgal", series.id)
        assertEquals(41, series.tomeCount)

        val album = result.entities[1] as CatalogBundleEntity.AlbumEntity
        assertEquals("9782803680122", album.ean)
        assertEquals(40, album.tomeNumber)
    }

    @Test
    fun `optional fields default to null`() {
        val lines = sequenceOf(
            """{"t":"s","id":"xiii","title":"XIII","status":"FINISHED"}""",
            """{"t":"a","ean":"9782505001325","seriesId":"xiii","title":"Le Jour du soleil noir"}""",
        )

        val result = CatalogBundleParser.parse(lines)

        assertEquals(0, result.malformedLines)
        val series = result.entities[0] as CatalogBundleEntity.SeriesEntity
        assertEquals(null, series.publisher)
        assertEquals(null, series.firstYear)
        assertEquals(null, series.tomeCount)
        val album = result.entities[1] as CatalogBundleEntity.AlbumEntity
        assertEquals(null, album.tomeNumber)
        assertEquals(null, album.published)
        assertEquals(null, album.pages)
    }

    @Test
    fun `blank lines are skipped without counting as malformed`() {
        val lines = sequenceOf(
            "",
            "   ",
            """{"t":"s","id":"xiii","title":"XIII","status":"FINISHED"}""",
            "",
        )

        val result = CatalogBundleParser.parse(lines)

        assertEquals(1, result.entities.size)
        assertEquals(0, result.malformedLines)
    }

    @Test
    fun `invalid json counts as malformed and does not abort parsing`() {
        val lines = sequenceOf(
            """{"t":"s","id":"xiii","title":"XIII","status":"FINISHED"}""",
            "not json at all",
            """{"t":"s","id":"thorgal","title":"Thorgal","status":"ONGOING"}""",
        )

        val result = CatalogBundleParser.parse(lines)

        assertEquals(2, result.entities.size)
        assertEquals(1, result.malformedLines)
    }

    @Test
    fun `known type missing a required field counts as malformed`() {
        // "a" sans ean, requis.
        val lines = sequenceOf("""{"t":"a","seriesId":"xiii","title":"Sans EAN"}""")

        val result = CatalogBundleParser.parse(lines)

        assertEquals(0, result.entities.size)
        assertEquals(1, result.malformedLines)
    }

    @Test
    fun `unknown type is ignored, not malformed`() {
        // Format futur, pas une erreur (§CONTRATS-DE-DONNEES §5.3) : forward-compat.
        val lines = sequenceOf(
            """{"t":"cover","ean":"9782803680122","url":"https://example.invalid/x.jpg"}""",
            """{"t":"s","id":"xiii","title":"XIII","status":"FINISHED"}""",
        )

        val result = CatalogBundleParser.parse(lines)

        assertEquals(1, result.entities.size)
        assertEquals(0, result.malformedLines)
        assertEquals(1, result.unknownTypeLines)
    }

    @Test
    fun `line without a t field is malformed`() {
        val lines = sequenceOf("""{"id":"xiii","title":"XIII"}""")

        val result = CatalogBundleParser.parse(lines)

        assertEquals(0, result.entities.size)
        assertEquals(1, result.malformedLines)
    }

    @Test
    fun `empty input yields an empty result`() {
        val result = CatalogBundleParser.parse(emptySequence())

        assertEquals(0, result.entities.size)
        assertEquals(0, result.malformedLines)
        assertEquals(0, result.unknownTypeLines)
    }
}
