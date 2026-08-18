package com.bdshelf.app.domain

import com.bdshelf.app.data.local.entities.ReadStatus
import com.bdshelf.app.data.local.entities.SeriesStatus
import com.bdshelf.app.data.repo.CollectionSnapshot

/**
 * Pont entre [CollectionSnapshot] (Room/DTO) et [buildCollectionCsv]
 * (§ADR-002) : le domaine ne connaît que des lignes déjà mises en forme.
 * Cette conversion, y compris les libellés français, vit côté application,
 * à la frontière.
 */
fun CollectionSnapshot.toCsv(): String {
    val seriesById = series.associateBy { it.id }
    val rows = albums.map { album ->
        val s = seriesById[album.seriesId]
        ExportRow(
            seriesTitle = s?.title ?: album.seriesId,
            seriesStatus = s?.status?.toFrench() ?: "",
            seriesTracked = s?.isTracked == true,
            tomeNumber = album.tomeNumber,
            albumTitle = album.title,
            owned = album.owned,
            readStatus = album.readStatus.toFrench(),
            edition = album.edition,
            ean = album.ean,
        )
    }
    return buildCollectionCsv(rows)
}

private fun SeriesStatus.toFrench(): String = when (this) {
    SeriesStatus.ONGOING -> "En cours"
    SeriesStatus.FINISHED -> "Terminée"
    SeriesStatus.UNKNOWN -> "Statut inconnu"
}

private fun ReadStatus.toFrench(): String = when (this) {
    ReadStatus.UNREAD -> "Non lu"
    ReadStatus.READ -> "Lu"
    ReadStatus.LENT -> "Prêté"
}
