package com.bdshelf.app.domain

import com.bdshelf.app.data.local.entities.Album
import com.bdshelf.app.data.local.entities.Series

/**
 * Pont entre les entités Room/DTO et [buildShoppingList] (§ADR-002) : le
 * domaine ne connaît ni `Album`, ni `Series`, ni `ReleaseItem`, seulement ce
 * qui lui est nécessaire. Cette conversion vit côté application, à la
 * frontière.
 */
private fun Album.toShoppingInput() = ShoppingAlbumInput(id, seriesId, tomeNumber, title, owned)

private fun Series.toShoppingInput() = ShoppingSeriesInput(id, title)

private fun ReleaseWithOwnership.toShoppingInput() = ShoppingReleaseInput(
    seriesId = release.seriesId,
    seriesTitle = release.seriesTitle,
    tomeNumber = release.tomeNumber,
    title = release.title,
    status = release.status,
    owned = owned,
)

fun buildShoppingList(
    series: List<Series>,
    albums: List<Album>,
    releasedUnowned: List<ReleaseWithOwnership>,
): List<ShoppingGroup> = buildShoppingList(
    series = series.map { it.toShoppingInput() },
    albums = albums.map { it.toShoppingInput() },
    releasedUnowned = releasedUnowned.map { it.toShoppingInput() },
)
