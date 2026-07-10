package com.bdshelf.app.domain

import com.bdshelf.app.data.local.dao.SeriesWithCounts
import com.bdshelf.app.data.local.entities.Album
import com.bdshelf.app.data.local.entities.ReadStatus
import com.bdshelf.app.data.local.entities.SeriesStatus
import org.junit.Assert.assertEquals
import org.junit.Test

class SeriesFilteringTest {

    private fun seriesWithCounts(id: String, title: String = id, owned: Int = 0, total: Int = 0) =
        SeriesWithCounts(
            id = id,
            title = title,
            status = SeriesStatus.ONGOING,
            isTracked = true,
            color = 0L,
            knownTomeCount = null,
            notes = null,
            ownedCount = owned,
            totalCount = total,
        )

    private val thorgal = seriesWithCounts("thorgal", "Thorgal", owned = 2, total = 3)
    private val asterix = seriesWithCounts("asterix", "Astérix", owned = 1, total = 1)
    private val xiii = seriesWithCounts("xiii", "XIII", owned = 0, total = 0)

    private val albums = listOf(
        album("thorgal", 1, owned = true, title = "La Magicienne trahie", readStatus = ReadStatus.READ),
        album("thorgal", 36, owned = true, title = "Aniel", readStatus = ReadStatus.UNREAD),
        album("thorgal", 2, owned = false, title = "L'Île des mers gelées"),
        album("asterix", 1, owned = true, title = "Astérix le Gaulois", readStatus = ReadStatus.READ),
    )

    private fun run(
        query: String = "",
        filter: SeriesFilter = SeriesFilter.ALL,
        sort: SeriesSort = SeriesSort.TITLE,
        series: List<SeriesWithCounts> = listOf(thorgal, asterix, xiii),
    ): List<String> = filterAndSortSeries(series, albums, query, filter, sort).map { it.id }

    @Test
    fun `searches series titles ignoring case and accents`() {
        assertEquals(listOf("asterix"), run(query = "asterix"))
        assertEquals(listOf("asterix"), run(query = "ASTÉRIX"))
    }

    @Test
    fun `searches album titles too`() {
        // « Aniel » n'apparaît que dans le titre d'un album de Thorgal.
        assertEquals(listOf("thorgal"), run(query = "aniel"))
    }

    @Test
    fun `incomplete filter keeps only series with gaps`() {
        assertEquals(listOf("thorgal"), run(filter = SeriesFilter.INCOMPLETE))
    }

    @Test
    fun `unread filter keeps series with owned unread albums`() {
        // Astérix : tout lu. XIII : rien possédé. Thorgal : Aniel possédé non lu.
        assertEquals(listOf("thorgal"), run(filter = SeriesFilter.UNREAD))
    }

    @Test
    fun `title sort is alphabetical ignoring accents`() {
        assertEquals(listOf("asterix", "thorgal", "xiii"), run(sort = SeriesSort.TITLE))
    }

    @Test
    fun `completion sort puts least complete first and empty series last`() {
        // Thorgal 2/3, Astérix 1/1, XIII 0 album (ratio indéfini -> fin).
        assertEquals(listOf("thorgal", "asterix", "xiii"), run(sort = SeriesSort.COMPLETION))
    }

    @Test
    fun `recent sort puts the latest owned addition first`() {
        val recentAlbums = listOf(
            album("thorgal", 1, owned = true).copy(dateAdded = 100L),
            album("asterix", 1, owned = true).copy(dateAdded = 200L),
        )
        val result = filterAndSortSeries(
            listOf(thorgal, asterix, xiii),
            recentAlbums,
            query = "",
            filter = SeriesFilter.ALL,
            sort = SeriesSort.RECENT,
        ).map { it.id }
        // Astérix a l'ajout le plus récent ; XIII (jamais alimentée) ferme la liste.
        assertEquals(listOf("asterix", "thorgal", "xiii"), result)
    }

    @Test
    fun `blank query keeps everything`() {
        assertEquals(3, run(query = "  ").size)
    }
}
