package com.bdshelf.app.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.bdshelf.app.data.local.entities.Album
import com.bdshelf.app.data.local.entities.ReadStatus
import com.bdshelf.app.domain.GapDetector
import kotlin.math.abs

private sealed interface ShelfItem {
    val key: String
    val sortKey: Int

    data class Existing(val album: Album) : ShelfItem {
        override val key get() = album.id
        override val sortKey get() = album.tomeNumber ?: Int.MAX_VALUE
    }

    data class Gap(val tomeNumber: Int) : ShelfItem {
        override val key get() = "gap-$tomeNumber"
        override val sortKey get() = tomeNumber
    }
}

private fun buildShelfItems(albums: List<Album>): List<ShelfItem> {
    val existingNumbers = albums.mapNotNull { it.tomeNumber }.toSet()
    val gapNumbers = GapDetector.gaps(albums).filterNot { it in existingNumbers }
    val items: List<ShelfItem> = albums.map { ShelfItem.Existing(it) } + gapNumbers.map { ShelfItem.Gap(it) }
    return items.sortedBy { it.sortKey }
}

/**
 * L'étagère d'une série : `LazyRow` de [SpineTile], triée par numéro de tome
 * croissant (hors-séries en fin), avec les trous visibles (§5.4 / §6.6).
 */
@Composable
fun Shelf(
    seriesId: String,
    seriesColor: Color,
    albums: List<Album>,
    highlightedAlbumId: String? = null,
    onAlbumClick: (Album) -> Unit,
    onGapClick: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    // Légère variation de hauteur par série pour l'effet "rayonnage".
    val tileHeight = remember(seriesId) { 160.dp + (abs(seriesId.hashCode()) % 21).dp }
    val items = remember(albums) { buildShelfItems(albums) }

    LazyRow(
        modifier = modifier,
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        items(items = items, key = { it.key }) { item ->
            when (item) {
                is ShelfItem.Existing -> SpineTile(
                    tomeNumber = item.album.tomeNumber,
                    seriesColor = seriesColor,
                    owned = item.album.owned,
                    readStatus = item.album.readStatus,
                    height = tileHeight,
                    highlighted = item.album.id == highlightedAlbumId,
                    onClick = { onAlbumClick(item.album) },
                )

                is ShelfItem.Gap -> SpineTile(
                    tomeNumber = item.tomeNumber,
                    seriesColor = seriesColor,
                    owned = false,
                    readStatus = ReadStatus.UNREAD,
                    height = tileHeight,
                    onClick = { onGapClick(item.tomeNumber) },
                    onLongClick = { onGapClick(item.tomeNumber) },
                )
            }
        }
    }
}
