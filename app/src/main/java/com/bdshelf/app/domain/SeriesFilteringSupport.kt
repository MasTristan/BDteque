package com.bdshelf.app.domain

import com.bdshelf.app.data.local.dao.SeriesWithCounts
import com.bdshelf.app.data.local.entities.Album
import com.bdshelf.app.data.local.entities.ReadStatus

/**
 * Pont entre [SeriesWithCounts]/[Album] (Room) et [filterAndSortSeriesIds]
 * (§ADR-002) : le domaine ne connaît que les identifiants et les champs
 * nécessaires au filtrage/tri, pas la projection complète. Cette conversion
 * vit côté application, à la frontière.
 */
private fun SeriesWithCounts.toFilterCandidate() = SeriesFilterCandidate(id, title, ownedCount, totalCount)

private fun Album.toFilterAlbum() = SeriesFilterAlbum(seriesId, title, owned, readStatus == ReadStatus.UNREAD, dateAdded)

fun filterAndSortSeries(
    series: List<SeriesWithCounts>,
    albums: List<Album>,
    query: String,
    filter: SeriesFilter,
    sort: SeriesSort,
): List<SeriesWithCounts> {
    val byId = series.associateBy { it.id }
    val orderedIds = filterAndSortSeriesIds(
        series = series.map { it.toFilterCandidate() },
        albums = albums.map { it.toFilterAlbum() },
        query = query,
        filter = filter,
        sort = sort,
    )
    return orderedIds.mapNotNull { byId[it] }
}
