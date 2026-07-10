package com.bdshelf.app.domain

import com.bdshelf.app.data.remote.ReleaseItem
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ShoppingListTest {

    private val seriesList = listOf(series("thorgal", "Thorgal"), series("asterix", "Astérix"))

    private fun release(
        seriesId: String,
        tome: Int,
        status: String = "RELEASED",
        owned: Boolean = false,
        title: String = "",
    ) = ReleaseWithOwnership(
        release = ReleaseItem(
            seriesId = seriesId,
            seriesTitle = seriesList.firstOrNull { it.id == seriesId }?.title ?: seriesId,
            tomeNumber = tome,
            title = title,
            expectedDate = "2026-01-01",
            status = status,
        ),
        owned = owned,
    )

    @Test
    fun `missing albums become shopping items grouped by series`() {
        val albums = listOf(
            album("thorgal", 2, owned = false, title = "L'Île des mers gelées"),
            album("thorgal", 1, owned = true),
            album("asterix", 1, owned = false),
        )
        val groups = buildShoppingList(seriesList, albums, emptyList())

        assertEquals(listOf("Astérix", "Thorgal"), groups.map { it.seriesTitle })
        assertEquals(2, groups.itemCount())
        val thorgalItems = groups.first { it.seriesId == "thorgal" }.items
        assertEquals(listOf(2), thorgalItems.map { it.tomeNumber })
        assertEquals("L'Île des mers gelées", thorgalItems.single().title)
    }

    @Test
    fun `released unowned releases without a matching album are included`() {
        val groups = buildShoppingList(
            seriesList,
            albums = emptyList(),
            releasedUnowned = listOf(release("thorgal", 40, title = "Tupilaks")),
        )
        val item = groups.single().items.single()
        assertEquals(40, item.tomeNumber)
        assertEquals("Tupilaks", item.title)
        assertEquals(null, item.albumId)
    }

    @Test
    fun `upcoming and owned releases are excluded`() {
        val groups = buildShoppingList(
            seriesList,
            albums = emptyList(),
            releasedUnowned = listOf(
                release("thorgal", 41, status = "UPCOMING"),
                release("thorgal", 39, owned = true),
            ),
        )
        assertTrue(groups.isEmpty())
    }

    @Test
    fun `a release matching an existing album is not duplicated`() {
        // Le tome 40 existe déjà en base (non possédé) : il apparaît comme trou,
        // pas deux fois.
        val albums = listOf(album("thorgal", 40, owned = false))
        val groups = buildShoppingList(
            seriesList,
            albums,
            releasedUnowned = listOf(release("thorgal", 40)),
        )
        assertEquals(1, groups.itemCount())
        assertEquals("thorgal-40", groups.single().items.single().albumId)
    }

    @Test
    fun `items are sorted by tome with unnumbered last`() {
        val albums = listOf(
            album("thorgal", null, owned = false, id = "thorgal-hs-1"),
            album("thorgal", 5, owned = false),
            album("thorgal", 2, owned = false),
        )
        val items = buildShoppingList(seriesList, albums, emptyList()).single().items
        assertEquals(listOf(2, 5, null), items.map { it.tomeNumber })
    }

    @Test
    fun `orphan albums without series are skipped`() {
        val albums = listOf(album("fantome", 1, owned = false))
        assertTrue(buildShoppingList(seriesList, albums, emptyList()).isEmpty())
    }

    @Test
    fun `share text lists series and tomes`() {
        val albums = listOf(
            album("thorgal", 2, owned = false, title = "L'Île des mers gelées"),
            album("asterix", 1, owned = false),
        )
        val text = buildShoppingList(seriesList, albums, emptyList()).toShareText("Ma liste :")
        assertEquals(
            """
            Ma liste :

            Astérix
              - Tome 1

            Thorgal
              - Tome 2 : L'Île des mers gelées
            """.trimIndent(),
            text,
        )
    }
}
