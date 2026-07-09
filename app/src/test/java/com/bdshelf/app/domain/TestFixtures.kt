package com.bdshelf.app.domain

import com.bdshelf.app.data.local.entities.Album
import com.bdshelf.app.data.local.entities.ReadStatus
import com.bdshelf.app.data.local.entities.Series
import com.bdshelf.app.data.local.entities.SeriesStatus

/** Fabriques d'entités pour les tests unitaires JVM (valeurs par défaut neutres). */

fun series(
    id: String,
    title: String = id,
    status: SeriesStatus = SeriesStatus.ONGOING,
    isTracked: Boolean = true,
    knownTomeCount: Int? = null,
    notes: String? = null,
): Series = Series(
    id = id,
    title = title,
    status = status,
    isTracked = isTracked,
    color = 0L,
    knownTomeCount = knownTomeCount,
    notes = notes,
)

fun album(
    seriesId: String,
    tomeNumber: Int?,
    owned: Boolean,
    id: String = if (tomeNumber != null) "$seriesId-$tomeNumber" else "$seriesId-hs",
    title: String? = null,
    readStatus: ReadStatus = ReadStatus.UNREAD,
    edition: String? = null,
    ean: String? = null,
): Album = Album(
    id = id,
    seriesId = seriesId,
    tomeNumber = tomeNumber,
    title = title,
    owned = owned,
    readStatus = readStatus,
    edition = edition,
    ean = ean,
    dateAdded = 0L,
)
