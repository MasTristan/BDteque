package com.bdshelf.app.data.local.dao

import com.bdshelf.app.data.local.entities.SeriesStatus

/**
 * Projection d'une [com.bdshelf.app.data.local.entities.Series] enrichie des
 * compteurs d'albums, pour l'écran liste des séries (§6.5 : compteur "x/y").
 */
data class SeriesWithCounts(
    val id: String,
    val title: String,
    val status: SeriesStatus,
    val isTracked: Boolean,
    val color: Long,
    val knownTomeCount: Int?,
    val notes: String?,
    val ownedCount: Int,
    val totalCount: Int,
) {
    val hasGap: Boolean get() = ownedCount < totalCount
}
